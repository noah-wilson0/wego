package com.wego.wego.external.route.exception;

import com.wego.wego.domain.plan.dto.draft.route.RoutingSummary;
import com.wego.wego.external.tourapi.place.entity.Place;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.core.NestedExceptionUtils.buildMessage;

/** kakaoCode
 *     105: (chainIdx-1,chainIdx) 과 현재 계산(chainIdx, chainIdx+1) 재 계산 필요
 *     106: 재 계산할 항목이 현재 계산(chainIdx, chainIdx+1)만 필요함
 */
public class RoutingRepairRequiredException extends RuntimeException{
    private final LocalDate date;
    private final int chainIdx; // 일자별 연속 구간 경로 계산(computeCarRoutingSummary) 중 문제가 발생한 인덱스
    private final int kakaoCode;
    private final Place origin;
    private final Place destination;
    private final List<RoutingSummary.RouteLeg> routeLegSnapshot;
    private final List<Place> chainSnapShot;
    private final RoutingSummary routingSummarySnapShot;

    public RoutingRepairRequiredException(LocalDate date, int chainIdx, int kakaoCode, Place origin, Place destination, List<RoutingSummary.RouteLeg> routeLegSnapshot, List<Place> chainSnapShot, RoutingSummary routingSummarySnapShot) {
        super();
        this.date = date;
        this.chainIdx = chainIdx;
        this.kakaoCode = kakaoCode;
        this.origin = origin;
        this.destination = destination;
        this.routeLegSnapshot = routeLegSnapshot;
        this.chainSnapShot = chainSnapShot;
        this.routingSummarySnapShot = routingSummarySnapShot;
    }


    public LocalDate getDate() {
        return date;
    }

    public int getChainIdx() {
        return chainIdx;
    }

    public int getKakaoCode() {
        return kakaoCode;
    }

    public Place getOrigin() {
        return origin;
    }

    public Place getDestination() {
        return destination;
    }

    public List<RoutingSummary.RouteLeg> getRouteLegSnapshot() {
        return routeLegSnapshot;
    }

    public List<Place> getChainSnapShot() {
        return chainSnapShot;
    }

    public RoutingSummary getRoutingSummarySnapShot() {
        return routingSummarySnapShot;
    }
}
