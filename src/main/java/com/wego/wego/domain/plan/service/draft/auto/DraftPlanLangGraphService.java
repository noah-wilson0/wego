package com.wego.wego.domain.plan.service.draft.auto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;
import com.wego.wego.domain.plan.dto.DraftPlanGeminiResponse;
import com.wego.wego.domain.plan.dto.DraftPlanResponse;
import com.wego.wego.domain.plan.dto.draft.auto.AutoGenerateInitialRequest;
import com.wego.wego.domain.plan.dto.draft.auto.ChemiSummaryForAiDto;
import com.wego.wego.domain.plan.dto.draft.auto.GeminiPlaceItemResponse;
import com.wego.wego.domain.plan.dto.draft.route.DraftPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.draft.route.RoutingSummary;
import com.wego.wego.domain.plan.service.support.SlugResolver;
import com.wego.wego.domain.plan.util.RouteScheduleUtil;
import com.wego.wego.external.route.dto.RouteResult;
import com.wego.wego.external.route.kakao.sevice.KaKaoMobilityFetchService;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.repository.PlaceRepository;
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

    private final KaKaoMobilityFetchService kaKaoMobilityFetchService;

    private final RedisTemplate<String,String > redisTemplate;
    private final ObjectMapper objectMapper;

    @Qualifier("AutoWebClient")
    private final WebClient autoWebClient;

    public DraftPlanLangGraphService(
            MemberRepository memberRepository,
            PlaceRepository placeRepository,
            SlugResolver slugResolver,
            KaKaoMobilityFetchService kaKaoMobilityFetchService,
            RedisTemplate<String,String> redisTemplate,
            ObjectMapper objectMapper,
            @Qualifier("AutoWebClient") WebClient autoWebClient
    ) {
        this.memberRepository = memberRepository;
        this.placeRepository = placeRepository;
        this.slugResolver = slugResolver;
        this.kaKaoMobilityFetchService = kaKaoMobilityFetchService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.autoWebClient = autoWebClient;
    }

    @Transactional
    public DraftPlanResponse createAutoDraftPlan(String uuid, Member member) {
        // 1) Redis 입력 로드
        String slugJson  = redisTemplate.opsForValue().get(RedisKeyUtils.slugKey(uuid));
        String dateJson  = redisTemplate.opsForValue().get(RedisKeyUtils.dateKey(uuid));
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
        String endDate   = dateNode.get("endDate").asText();

        // 2) 케미 요약
        ChemiSummaryForAiDto chemiSummaryForAiDto = memberRepository
                .findChemiSummaryForAiDtoByMemberId(member.getId())
                .orElseThrow(() -> new RuntimeException("케미 테스트 필요"));

        // 3) FastAPI 호출(Gemini)
        AutoGenerateInitialRequest req = new AutoGenerateInitialRequest(
                member.getId(), regionName, startDate, endDate, chemiSummaryForAiDto
        );

        DraftPlanGeminiResponse gemini;
        try {
            gemini = autoWebClient.post()
                    .uri("/ai/generate-initial")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(DraftPlanGeminiResponse.class)
                    .block();
            log.info("FastAPI OK: {}", gemini);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            log.error("FastAPI error status={}, body={}", ex.getRawStatusCode(), body, ex);
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY, "FastAPI 응답 오류: " + body, ex
            );
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException ex) {
            log.error("FastAPI 연결 실패: {}", ex.getMessage(), ex);
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_GATEWAY, "FastAPI 연결 실패: " + ex.getMessage(), ex
            );
        }

        // 4) label → slug
        String resolvedSlug = slugResolver.resolveSlugByLabel(gemini.label());

        // 5) Gemini 응답 → RoutingDaySpec 목록으로 변환
        List<DraftPlanRoutingRequest.RoutingDaySpec> daySpecs = new ArrayList<>();
        for (DraftPlanGeminiResponse.Days d : gemini.days()) {
            LocalDate date = LocalDate.parse(d.date());
            LocalTime start = LocalTime.parse(d.start_time());
            LocalTime end   = LocalTime.parse(d.end_time());

            // 방문지 매핑
            List<Place> places = new ArrayList<>();
            for (DraftPlanGeminiResponse.Days.Places gp : d.places()) {
                Place p = placeRepository.findMostSimilarTitle(gp.title())
                        .orElseThrow(() -> new RuntimeException("매칭되지 않은 여행 장소: " + gp.title()));
                places.add(p);
            }

            // 숙소 매핑(없으면 null)
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

        // 6) 경로 계산 → RoutingSummary (car 고정)
        RoutingSummary summary = computeCarRoutingSummary(routingRequest);

        // 7) DaySchedule 조립(체류시간 포함), RouteInfo 변환
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

        // 8) Redis 저장
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue()
                    .set(RedisKeyUtils.tempScheduleKey(uuid), json, 6, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.warn("DraftPlanResponse 직렬화 실패 (Redis 저장 생략)", e);
        }

        return response;
    }

    /** Kakao Mobility로 일자별 연속구간 경로 계산 → RoutingSummary(car) */
    private RoutingSummary computeCarRoutingSummary(DraftPlanRoutingRequest request) {
        Map<LocalDate, List<RoutingSummary.RouteLeg>> daily = new LinkedHashMap<>();

        for (DraftPlanRoutingRequest.RoutingDaySpec spec : request.days()) {
            // 호출용 시퀀스(숙소가 있으면 마지막에 붙여서 구간 완성)
            List<Place> chain = new ArrayList<>(spec.places());
            if (spec.accommodation() != null) chain.add(spec.accommodation());

            List<RoutingSummary.RouteLeg> legs = new ArrayList<>();
            for (int i = 0; i < Math.max(0, chain.size() - 1); i++) {
                RouteResult r = kaKaoMobilityFetchService.fetchKaKaoMobilityData(
                        chain.get(i), chain.get(i + 1)
                );
                legs.add(RoutingSummary.RouteLeg.builder()
                        .sequence(i + 1)                            // 1..N
                        .origin(r.getOriginId())
                        .destination(r.getDestinationId())
                        .duration(r.getDuration())
                        .build());
            }
            daily.put(spec.date(), legs);
        }

        return RoutingSummary.builder()
                .routeType("car")
                .dailyRoutes(daily)
                .build();
    }

    /** 위경도 문자열 안전 파싱 (이 파일에선 불필요하지만 남겨둠) */
    private static double safeParseDouble(String s) {
        if (s == null || s.isBlank()) return 0.0;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
    }

    public Page<GeminiPlaceItemResponse> getPlacesPaged(String regionName, List<String> placeTypes, Pageable pageable) {
        List<Integer> cityIds = slugResolver.resolveCityIdsByLabel(regionName);
        return placeRepository.searchGeminiPlaceItemResponseByTitleInCities(
                placeTypes, cityIds.stream().mapToLong(Integer::longValue).boxed().toList(), pageable
        );
    }
}
