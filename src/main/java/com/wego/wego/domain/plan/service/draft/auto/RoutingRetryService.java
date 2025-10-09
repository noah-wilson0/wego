package com.wego.wego.domain.plan.service.draft.auto;

import com.wego.wego.domain.plan.dto.draft.auto.DraftPlanCorrectedPlaceResponse;
import com.wego.wego.domain.plan.dto.draft.auto.DraftPlanCorrectionRequest;
import com.wego.wego.domain.plan.dto.draft.auto.DraftPlanGeminiResponse;
import com.wego.wego.domain.plan.dto.draft.route.DraftPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.draft.route.RoutingSummary;
import com.wego.wego.external.route.dto.RouteResult;
import com.wego.wego.external.route.exception.KakaoRouteException;
import com.wego.wego.external.route.exception.RoutingRepairRequiredException;
import com.wego.wego.external.route.kakao.sevice.KaKaoMobilityFetchService;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.repository.PlaceRepository;
import com.wego.wego.global.config.dto.RepairContext;
import com.wego.wego.global.exception.retry.RetryRoutingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service

public class RoutingRetryService {

    private final KaKaoMobilityFetchService kakao;
    private final PlaceRepository placeRepository;
    @Qualifier("AutoWebClient")
    private final WebClient autoWebClient;

    public RoutingRetryService(
            KaKaoMobilityFetchService kakao, PlaceRepository placeRepository,
            @Qualifier("AutoWebClient") WebClient autoWebClient
            ) {
        this.kakao = kakao;
        this.placeRepository = placeRepository;
        this.autoWebClient = autoWebClient;
    }

    /**
     * 하루 라우팅을 시도하고, 실패 시 교체 → 스냅샷 재개 → 재시도까지 자동 수행.
     * 성공 시 완성된 스냅샷을 반환하고 ctx.spec 도 최신 체인으로 갱신되어 있음.
     */
    @Retryable(
            include = RetryRoutingException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 300, multiplier = 2.0)
    )
    public RoutingSummary routeOneDayWithAutoRepair(RepairContext ctx, DraftPlanGeminiResponse gemini) {
        try {
            // 1) 최초/재시도 모두 동일하게 "당일 전체" 계산 시도
            RoutingSummary result = computeOneDayOrThrow(ctx.getSnapshot(), ctx.getSpec());
            ctx.setSnapshot(result);
            return result;

        } catch (RoutingRepairRequiredException ex) {
            // 2) 실패 → 교체 대상 세팅
            ctx.setDate(ex.getDate());
            ctx.setChainIdx(ex.getChainIdx());
            ctx.setKakaoCode(ex.getKakaoCode());
            ctx.setRouteLegSnapshot(ex.getRouteLegSnapshot());
            ctx.setChainSnapshot(ex.getChainSnapShot());
            ctx.setRoutingSummarySnapshot(ex.getRoutingSummarySnapShot());

            DraftPlanCorrectionRequest.CorrectionPlace cp = (ex.getKakaoCode() == 105)
                    ? new DraftPlanCorrectionRequest.CorrectionPlace(
                    ex.getOrigin().getTitle(),
                    ex.getOrigin().getAddr2() == null ? ex.getOrigin().getAddr1()
                            : ex.getOrigin().getAddr1() + ex.getOrigin().getAddr2(),
                    ex.getOrigin().getTel()
            )
                    : new DraftPlanCorrectionRequest.CorrectionPlace(
                    ex.getDestination().getTitle(),
                    ex.getDestination().getAddr2() == null ? ex.getDestination().getAddr1()
                            : ex.getDestination().getAddr1() + ex.getDestination().getAddr2(),
                    ex.getDestination().getTel()
            );

            // 3) FastAPI로 교체 후보 요청
            DraftPlanCorrectedPlaceResponse fix = autoWebClient.post()
                    .uri("/ai/repair-slot")
                    .bodyValue(new DraftPlanCorrectionRequest(gemini, cp))
                    .retrieve()
                    .bodyToMono(DraftPlanCorrectedPlaceResponse.class)
                    .block();
            log.info("FastAPI OK (repair): {}", fix);

            // 4) 교체 후보를 DB Place로 매핑
            Place newPlace = placeRepository.findMostSimilarTitle(fix.title())
                    .orElseThrow(() -> new RuntimeException("교체 장소 매핑 실패: " + fix.title()));

            // 5) 당일 체인 재구성 후 교체
            DraftPlanRoutingRequest.RoutingDaySpec spec = ctx.getSpec();
            List<Place> chain = new ArrayList<>(spec.places());
            boolean hasAcc = (spec.accommodation() != null);
            if (hasAcc) chain.add(spec.accommodation());

            int replaceIdx = (ex.getKakaoCode() == 105) ? ex.getChainIdx() : ex.getChainIdx() + 1;
            if (replaceIdx < 0 || replaceIdx >= chain.size()) {
                throw new RuntimeException("교체 인덱스 범위 오류: " + replaceIdx + " / chain.size=" + chain.size());
            }
            chain.set(replaceIdx, newPlace);

            // 6) 스냅샷 재개 (여기서 다시 막히면 리트라이 트리거)
            RoutingSummary resumed;
            try {
                resumed = resumeRoutingFromSnapshot(
                        ex.getRoutingSummarySnapShot(),
                        ex.getDate(),
                        chain,
                        ex.getChainIdx(),
                        ex.getKakaoCode()
                );
            } catch (RoutingRepairRequiredException ex2) {
                // 재개 단계에서도 또 막힘 → 최신 컨텍스트 넣고 리트라이 트리거
                ctx.setDate(ex2.getDate());
                ctx.setChainIdx(ex2.getChainIdx());
                ctx.setKakaoCode(ex2.getKakaoCode());
                ctx.setRouteLegSnapshot(ex2.getRouteLegSnapshot());
                ctx.setChainSnapshot(ex2.getChainSnapShot());
                ctx.setRoutingSummarySnapshot(ex2.getRoutingSummarySnapShot());
                throw new RetryRoutingException("retry after auto repair (resume failed)");
            }

            // 7) spec/snapshot 갱신
            List<Place> newPlaces = hasAcc ? new ArrayList<>(chain.subList(0, chain.size() - 1))
                    : new ArrayList<>(chain);
            Place newAcc = hasAcc ? chain.get(chain.size() - 1) : null;

            DraftPlanRoutingRequest.RoutingDaySpec repaired = DraftPlanRoutingRequest.RoutingDaySpec.builder()
                    .date(spec.date())
                    .start_time(spec.start_time())
                    .end_time(spec.end_time())
                    .places(newPlaces)
                    .accommodation(newAcc)
                    .build();

            ctx.setSpec(repaired);
            ctx.setSnapshot(resumed);

            // 8) 전체 재시도를 유도
            throw new RetryRoutingException("retry after auto repair");
        }
    }


    // ==== 내부 계산 함수들 (기존 DraftPlanLangGraphService의 메서드 이동) ====

    /** 하루(spec)만 계산해서 snapshot에 합쳐 반환. 실패 시 RoutingRepairRequiredException 던짐(스냅샷 포함). */
    private RoutingSummary computeOneDayOrThrow(RoutingSummary snapshot,
                                                DraftPlanRoutingRequest.RoutingDaySpec spec) {
        Map<LocalDate, List<RoutingSummary.RouteLeg>> daily =
                new LinkedHashMap<>(snapshot.dailyRoutes());

        List<Place> chain = new ArrayList<>(spec.places());
        if (spec.accommodation() != null) chain.add(spec.accommodation());

        List<RoutingSummary.RouteLeg> legs = new ArrayList<>();
        int chainIdx = 0;

        try {
            for (chainIdx = 0; chainIdx < Math.max(0, chain.size() - 1); chainIdx++) {
                RouteResult r = kakao.fetchKaKaoMobilityData(
                        chain.get(chainIdx), chain.get(chainIdx + 1)
                );
                legs.add(RoutingSummary.RouteLeg.builder()
                        .sequence(chainIdx + 1)  // 1..N
                        .origin(r.getOriginId())
                        .destination(r.getDestinationId())
                        .duration(r.getDuration())
                        .build());
            }
            daily.put(spec.date(), legs);

            return RoutingSummary.builder()
                    .routeType(snapshot.routeType())
                    .dailyRoutes(daily)
                    .build();

        } catch (KakaoRouteException ex) {
            // 지금까지 성공한 당일 legs를 daily에 반영한 스냅샷을 들고 예외 보고
            daily.put(spec.date(), new ArrayList<>(legs));

            throw new RoutingRepairRequiredException(
                    spec.date(),
                    chainIdx,
                    ex.getCode(),
                    chain.get(chainIdx),
                    chain.get(Math.min(chainIdx + 1, chain.size() - 1)),
                    new ArrayList<>(legs),
                    new ArrayList<>(chain),
                    RoutingSummary.builder()
                            .routeType(snapshot.routeType())
                            .dailyRoutes(daily)
                            .build()
            );
        }
    }

    /**
     * 스냅샷에서 해당 일자의 마지막 leg 1개(105) 롤백 후, startIdx부터 tail 재계산해 스냅샷을 갱신.
     *  - kakaoCode=105 → startIdx = chainIdx-1 (단, 0 미만이면 0), 그리고 기존 당일 legs 마지막 1개 제거 후 tail 재계산
     *  - kakaoCode=106 → startIdx = chainIdx (당일 legs는 그대로 두고 그 다음부터 tail 재계산)
     */
    private RoutingSummary resumeRoutingFromSnapshot(
            RoutingSummary routingSummarySnapShot,
            LocalDate date,
            List<Place> chain,
            int chainIdx,
            int kakaoCode
    ) {
        log.info("문제가 생긴 경로 재계산");
        Map<LocalDate, List<RoutingSummary.RouteLeg>> daily =
                new LinkedHashMap<>(routingSummarySnapShot.dailyRoutes());

        List<RoutingSummary.RouteLeg> legs =
                new ArrayList<>(daily.getOrDefault(date, List.of()));

        if (kakaoCode == 105 && !legs.isEmpty()) {
            // (chainIdx-1, chainIdx) 구간을 다시 계산할 것이므로 기존 마지막 1개 제거
            legs.remove(legs.size() - 1);
        }

        int startIdx = (kakaoCode == 105) ? Math.max(chainIdx - 1, 0) : chainIdx;

        try {
            for (int i = startIdx; i < Math.max(0, chain.size() - 1); i++) {
                RouteResult r = kakao.fetchKaKaoMobilityData(
                        chain.get(i), chain.get(i + 1)
                );
                legs.add(RoutingSummary.RouteLeg.builder()
                        .sequence(i + 1)
                        .origin(r.getOriginId())
                        .destination(r.getDestinationId())
                        .duration(r.getDuration())
                        .build());
            }
            daily.put(date, legs);

            return RoutingSummary.builder()
                    .routeType(routingSummarySnapShot.routeType())
                    .dailyRoutes(daily)
                    .build();

        } catch (KakaoRouteException ex) {
            // 재개 중 다시 실패 → 최신 스냅샷으로 감싸 재보고
            throw new com.wego.wego.external.route.exception.RoutingRepairRequiredException(
                    date,
                    startIdx,
                    ex.getCode(),
                    chain.get(startIdx),
                    chain.get(Math.min(startIdx + 1, chain.size() - 1)),
                    new ArrayList<>(legs),
                    new ArrayList<>(chain),
                    RoutingSummary.builder()
                            .routeType(routingSummarySnapShot.routeType())
                            .dailyRoutes(daily)
                            .build()
            );

        }
    }

    // 재시도 전부 소진 시 (원하면 스냅샷 반환으로 바꿔도 됨)
    @Recover
    public RoutingSummary recover(RetryRoutingException ex, RepairContext ctx, DraftPlanGeminiResponse gemini) {
        log.error("Auto repair retries exhausted. date={}, code={}, chainIdx={}",
                ctx.getDate(), ctx.getKakaoCode(), ctx.getChainIdx(), ex);
        // 현재까지 스냅샷을 그냥 반환하거나, 비즈니스 예외로 전파
        // return ctx.getSnapshot();
        throw new com.wego.wego.external.route.exception.RoutingRepairRequiredException(
                ctx.getDate() == null ? LocalDate.now() : ctx.getDate(),
                ctx.getChainIdx(),
                ctx.getKakaoCode(),
                null, null,
                ctx.getRouteLegSnapshot(),
                ctx.getChainSnapshot(),
                ctx.getSnapshot() == null ? ctx.getRoutingSummarySnapshot() : ctx.getSnapshot()
        );
    }
}

