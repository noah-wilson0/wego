package com.wego.wego.domain.plan.service.draft.auto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;
import com.wego.wego.domain.plan.dto.DraftPlanResponse;
import com.wego.wego.domain.plan.dto.draft.auto.*;
import com.wego.wego.domain.plan.dto.draft.route.DraftPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.draft.route.RoutingSummary;
import com.wego.wego.domain.plan.service.support.SlugResolver;
import com.wego.wego.domain.plan.util.RouteScheduleUtil;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.repository.PlaceRepository;
import com.wego.wego.global.config.dto.RepairContext;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional(readOnly = true)
public class DraftPlanLangGraphService {

    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;
    private final SlugResolver slugResolver;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Qualifier("AutoWebClient")
    private final WebClient autoWebClient;

    private final RoutingRetryService routingRetryService;

    public DraftPlanLangGraphService(
            MemberRepository memberRepository,
            PlaceRepository placeRepository,
            SlugResolver slugResolver,
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            @Qualifier("AutoWebClient") WebClient autoWebClient,
            RoutingRetryService routingRetryService
    ) {
        this.memberRepository = memberRepository;
        this.placeRepository = placeRepository;
        this.slugResolver = slugResolver;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.autoWebClient = autoWebClient;
        this.routingRetryService = routingRetryService;
    }

    @Transactional
    public DraftPlanResponse createAutoDraftPlan(String uuid, Member member) {
        // (1)~(3) FastAPI generate-initial 호출
        String slugJson = redisTemplate.opsForValue().get(RedisKeyUtils.slugKey(uuid));
        String dateJson = redisTemplate.opsForValue().get(RedisKeyUtils.dateKey(uuid));
        String timeJson = redisTemplate.opsForValue().get(RedisKeyUtils.timeKey(uuid));
        if (slugJson == null || dateJson == null) {
            throw new IllegalStateException("초기 입력(지역/날짜) 정보가 없습니다. 이전 단계가 완료되지 않았습니다.");
        }

        JsonNode slugNode;
        JsonNode dateNode;
        JsonNode timeNode;
        try {
            slugNode = objectMapper.readTree(slugJson);
            dateNode = objectMapper.readTree(dateJson);
            timeNode = objectMapper.readTree(timeJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("사용자 입력 파싱 실패", e);
        }

        String slug = slugNode.get("slug").asText();
        String regionName = slugResolver.resolveLabel(slug);
        String startDate = dateNode.get("startDate").asText();
        String endDate = dateNode.get("endDate").asText();

        List<AutoGenerateInitialRequest.DayTime> dayTimes = new ArrayList<>();
        for (JsonNode n : timeNode.get("travelDayTimes")) {
            dayTimes.add(new AutoGenerateInitialRequest.DayTime(
                    n.get("date").asText(),
                    n.get("startTime").asText(),
                    n.get("endTime").asText()
            ));
        }

        var chemiSummaryForAiDto = memberRepository
                .findChemiSummaryForAiDtoByMemberId(member.getId())
                .orElseThrow(() -> new RuntimeException("케미 테스트 필요"));

        AutoGenerateInitialRequest req = new AutoGenerateInitialRequest(
                member.getId(), regionName, startDate, endDate, chemiSummaryForAiDto, dayTimes
        );
        log.info("[auto] AutoGenerateInitialRequest: {}", req);

        DraftPlanGeminiResponse gemini = autoWebClient.post()
                .uri("/ai/generate-initial")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(DraftPlanGeminiResponse.class)
                .block();
        log.info("[auto] FastAPI OK: {}", gemini);

        // (4) label → slug
        String resolvedSlug = slugResolver.resolveSlugByLabel(gemini.label());

        // (5) 전체 슬롯 정규화 + DB 매핑 (title 기반, 슬롯당 최대 3회 재시도)
        List<DraftPlanRoutingRequest.RoutingDaySpec> daySpecs =
                normalizeAndMapAllSlots(gemini, regionName, 3);

        DraftPlanRoutingRequest routingRequest = new DraftPlanRoutingRequest(daySpecs);

        // (6) 하루씩 계산: 리트라이 서비스 호출
        RoutingSummary snapshot = RoutingSummary.builder()
                .routeType("car")
                .dailyRoutes(new LinkedHashMap<>())
                .build();

        for (int dayIdx = 0; dayIdx < routingRequest.days().size(); dayIdx++) {
            DraftPlanRoutingRequest.RoutingDaySpec spec = routingRequest.days().get(dayIdx);

            RepairContext ctx = new RepairContext(snapshot, spec);
            snapshot = routingRetryService.routeOneDayWithAutoRepair(ctx, gemini);

            // 최신 스펙 반영(교체되었을 수 있음)
            routingRequest.days().set(dayIdx, ctx.getSpec());
        }

        RoutingSummary summary = snapshot;

        // (7) DaySchedule 조립
        List<DraftPlanResponse.DaySchedule> days = new ArrayList<>();
        for (DraftPlanRoutingRequest.RoutingDaySpec spec : routingRequest.days()) {
            List<RoutingSummary.RouteLeg> legs = summary.dailyRoutes().get(spec.date());
            days.add(RouteScheduleUtil.toDaySchedule(spec, legs));
        }

        List<DraftPlanResponse.RouteInfo> routes = List.of(
                DraftPlanResponse.RouteInfo.from(summary)
        );

        DraftPlanResponse response = DraftPlanResponse.builder()
                .slug(resolvedSlug)
                .start_date(gemini.start_date())
                .end_date(gemini.end_date())
                .days(days)
                .routes(routes)
                .build();

        // (8) Redis 저장
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue()
                    .set(RedisKeyUtils.tempScheduleKey(uuid), json, 6, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("DraftPlanResponse 직렬화 실패 (Redis 저장 생략)", e);
        }

        return response;
    }

    // =========================
    // 정규화 + 매핑 유틸리티
    // =========================

    private List<DraftPlanRoutingRequest.RoutingDaySpec> normalizeAndMapAllSlots(
            DraftPlanGeminiResponse gemini,
            String regionName,
            int maxAttempts
    ) {
        List<DraftPlanRoutingRequest.RoutingDaySpec> daySpecs = new ArrayList<>();

        for (DraftPlanGeminiResponse.Days d : gemini.days()) {
            LocalDate date = LocalDate.parse(d.date());
            LocalTime start = LocalTime.parse(d.start_time());
            LocalTime end = LocalTime.parse(d.end_time());

            // places
            List<Place> mappedPlaces = new ArrayList<>();
            for (DraftPlanGeminiResponse.Days.Places gp : d.places()) {
                Place p = normalizeOneSlotWithRetry(
                        gemini, regionName, gp.title(), gp.addr(), gp.tel(), maxAttempts, false
                );
                mappedPlaces.add(p);
            }

            // accommodation (마지막 날 null 허용)
            Place acc = null;
            if (d.accommodation() != null) {
                acc = normalizeOneSlotWithRetry(
                        gemini, regionName,
                        d.accommodation().title(), d.accommodation().addr(), d.accommodation().tel(),
                        maxAttempts, true
                );
            }

            daySpecs.add(DraftPlanRoutingRequest.RoutingDaySpec.builder()
                    .date(date)
                    .start_time(start)
                    .end_time(end)
                    .places(mappedPlaces)
                    .accommodation(acc)
                    .build());
        }

        return daySpecs;
    }

    /**
     * 단일 슬롯을 정규화해서 DB Place로 반환.
     * - 1) DB 직접 매핑(제목 기반) → 2) 실패 시 /ai/repair-slot 호출해 교체 타이틀 획득 → DB 매핑
     * - 최대 maxAttempts 회 시도
     */
    private Place normalizeOneSlotWithRetry(
            DraftPlanGeminiResponse gemini,
            String regionName,
            String title,
            String addr,
            String tel,
            int maxAttempts,
            boolean isAccommodation
    ) {
        String curTitle = title;
        String curAddr  = addr;
        String curTel   = tel;

        for (int attempt = 1; attempt <= Math.max(1, maxAttempts); attempt++) {
            try {
                // 1) DB 직접 매핑 (제목 기반)
                Optional<Place> direct = findDirect(regionName, curTitle, curAddr, curTel);
                if (direct.isPresent()) {
                    log.info("[normalize] direct OK (acc={}): {}", isAccommodation, curTitle);
                    return direct.get();
                }

                // 2) 실패 → FastAPI로 교체 후보 요청
                DraftPlanCorrectedPlaceResponse fix = requestRepair(gemini, curTitle, curAddr, curTel);

                // 3) 교체 후보를 DB Place로 매핑 (제목 기반)
                Place mapped = mapFixedToDb(regionName, fix);
                log.info("[normalize] repair OK (acc={}): {} -> {}", isAccommodation, curTitle, mapped.getTitle());
                return mapped;

            } catch (Exception ex) {
                log.warn("[normalize] attempt {} failed (acc={}): title='{}', cause={}",
                        attempt, isAccommodation, curTitle, ex.toString());

                if (attempt >= maxAttempts) {
                    throw new RuntimeException(
                            "정규화 실패: title='" + title + "', acc=" + isAccommodation + ", attempts=" + attempt, ex
                    );
                } else {
                    // 다음 루프에서 시도할 값 업데이트: repair 재호출해서 최신 후보를 받아두고 타이틀만 교체
                    try {
                        DraftPlanCorrectedPlaceResponse lastFix = requestRepair(gemini, curTitle, curAddr, curTel);
                        curTitle = lastFix.title();
                        curAddr  = lastFix.addr();
                        curTel   = lastFix.tel();
                        log.info("[normalize] next attempt will use repaired candidate title: {}", curTitle);
                    } catch (Exception inner) {
                        log.warn("[normalize] repair re-request failed, keep current fields. cause={}", inner.toString());
                    }
                }
            }
        }
        throw new IllegalStateException("unreachable");
    }

    /** region 기준 DB 직접 매핑 (제목 기반) */
    private Optional<Place> findDirect(String regionName, String title, String addr, String tel) {
        // 제목 기반 유사/정확 매칭
        return placeRepository.findMostSimilarTitle(title);
    }

    /** FastAPI /ai/repair-slot 요청 → 정규화된 후보 1개(title/addr/tel) */
    private DraftPlanCorrectedPlaceResponse requestRepair(
            DraftPlanGeminiResponse gemini,
            String title, String addr, String tel
    ) {
        DraftPlanCorrectionRequest.CorrectionPlace cp =
                new DraftPlanCorrectionRequest.CorrectionPlace(title, addr, tel);

        DraftPlanCorrectedPlaceResponse fix = autoWebClient.post()
                .uri("/ai/repair-slot")
                .bodyValue(new DraftPlanCorrectionRequest(gemini, cp))
                .retrieve()
                .bodyToMono(DraftPlanCorrectedPlaceResponse.class)
                .block();

        if (fix == null) {
            throw new IllegalStateException("repair-slot returned null for: " + title);
        }
        return fix;
    }

    /** 교체 후보 → DB Place 매핑 (제목 기반) */
    private Place mapFixedToDb(String regionName, DraftPlanCorrectedPlaceResponse fix) {
        return placeRepository.findMostSimilarTitle(fix.title())
                .orElseThrow(() -> new RuntimeException("교체 장소 매핑 실패(제목): " + fix.title()));
    }

    // =============== 보조 API ===============

    public Page<GeminiPlaceItemResponse> getPlacesPaged(String regionName, List<String> placeTypes, Pageable pageable) {
        List<Integer> cityIds = slugResolver.resolveCityIdsByLabel(regionName);
        return placeRepository.searchGeminiPlaceItemResponseByTitleInCities(
                placeTypes, cityIds.stream().mapToLong(Integer::longValue).boxed().toList(), pageable
        );
    }

    public List<GeminiPlaceItemResponse> searchPlace(String regionName, List<String> placeTypes, String title) {
        List<Integer> cityIds = slugResolver.resolveCityIdsByLabel(regionName);
        return placeRepository.searchGeminiPlaceItemResponseByTitleInCitiesAndLikeTitle(
                placeTypes, cityIds.stream().mapToLong(Integer::longValue).boxed().toList(),
                title
        );
    }
}
