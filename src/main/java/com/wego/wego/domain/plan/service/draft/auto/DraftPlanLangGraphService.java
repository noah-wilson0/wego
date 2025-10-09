package com.wego.wego.domain.plan.service.draft.auto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;
import com.wego.wego.domain.plan.dto.DraftPlanPlaceResponse;
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

    private final RoutingRetryService routingRetryService; // ★ 추가

    public DraftPlanLangGraphService(
            MemberRepository memberRepository,
            PlaceRepository placeRepository,
            SlugResolver slugResolver,
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper,
            @Qualifier("AutoWebClient") WebClient autoWebClient,
            RoutingRetryService routingRetryService // ★ 추가
    ) {
        this.memberRepository = memberRepository;
        this.placeRepository = placeRepository;
        this.slugResolver = slugResolver;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.autoWebClient = autoWebClient;
        this.routingRetryService = routingRetryService; // ★ 추가
    }

    @Transactional
    public DraftPlanResponse createAutoDraftPlan(String uuid, Member member) {
        // (1)~(3) FastAPI generate-initial 호출 부분 그대로
        String slugJson = redisTemplate.opsForValue().get(RedisKeyUtils.slugKey(uuid));
        String dateJson = redisTemplate.opsForValue().get(RedisKeyUtils.dateKey(uuid));
        if (slugJson == null || dateJson == null) {
            throw new IllegalStateException("초기 입력(지역/날짜) 정보가 없습니다. 이전 단계가 완료되지 않았습니다.");
        }

        JsonNode slugNode;
        JsonNode dateNode;
        try {
            slugNode = objectMapper.readTree(slugJson);
            dateNode = objectMapper.readTree(dateJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("사용자 입력 파싱 실패", e);
        }

        String slug = slugNode.get("slug").asText();
        String regionName = slugResolver.resolveLabel(slug);
        String startDate = dateNode.get("startDate").asText();
        String endDate = dateNode.get("endDate").asText();

        var chemiSummaryForAiDto = memberRepository
                .findChemiSummaryForAiDtoByMemberId(member.getId())
                .orElseThrow(() -> new RuntimeException("케미 테스트 필요"));

        AutoGenerateInitialRequest req = new AutoGenerateInitialRequest(
                member.getId(), regionName, startDate, endDate, chemiSummaryForAiDto
        );

        DraftPlanGeminiResponse gemini = autoWebClient.post()
                .uri("/ai/generate-initial")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(DraftPlanGeminiResponse.class)
                .block();
        log.info("FastAPI OK: {}", gemini);

        // (4) label → slug
        String resolvedSlug = slugResolver.resolveSlugByLabel(gemini.label());

        // (5) 응답 → RoutingDaySpec 변환(초기 매핑)
        List<DraftPlanRoutingRequest.RoutingDaySpec> daySpecs = new ArrayList<>();
        for (DraftPlanGeminiResponse.Days d : gemini.days()) {
            LocalDate date = LocalDate.parse(d.date());
            LocalTime start = LocalTime.parse(d.start_time());
            LocalTime end = LocalTime.parse(d.end_time());

            List<Place> places = new ArrayList<>();
            for (DraftPlanGeminiResponse.Days.Places gp : d.places()) {
                Place p = placeRepository.findMostSimilarTitle(gp.title())
                        .orElseThrow(() -> new RuntimeException("매칭되지 않은 여행 장소: " + gp.title()));
                places.add(p);
            }

            Place acc = null;
            if (d.accommodation() != null) {
                acc = placeRepository.findMostSimilarTitle(d.accommodation().title())
                        .orElseThrow(() -> new RuntimeException("매칭되지 않은 숙소: " + d.accommodation().title()));
            }

            daySpecs.add(DraftPlanRoutingRequest.RoutingDaySpec.builder()
                    .date(date)
                    .start_time(start)
                    .end_time(end)
                    .places(places)
                    .accommodation(acc)
                    .build());
        }

        DraftPlanRoutingRequest routingRequest = new DraftPlanRoutingRequest(daySpecs);

        // (6) 하루씩 계산: try/catch 제거 → 리트라이 서비스 호출
        RoutingSummary snapshot = RoutingSummary.builder()
                .routeType("car")
                .dailyRoutes(new LinkedHashMap<>())
                .build();

        for (int dayIdx = 0; dayIdx < routingRequest.days().size(); dayIdx++) {
            DraftPlanRoutingRequest.RoutingDaySpec spec = routingRequest.days().get(dayIdx);

            // 재시도 컨텍스트 생성
            RepairContext ctx = new RepairContext(snapshot, spec);

            // 실패 시 내부에서 자동으로 교체 → 재시도 → 성공 시 완성 스냅샷 반환
            snapshot = routingRetryService.routeOneDayWithAutoRepair(ctx, gemini);

            // 최신 스펙 반영(교체됐을 수 있음)
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
