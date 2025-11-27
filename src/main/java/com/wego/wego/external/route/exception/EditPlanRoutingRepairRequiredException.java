package com.wego.wego.external.route.exception;

import com.wego.wego.domain.plan.dto.edit.route.EditPlanRoutingResponse;
import com.wego.wego.external.tourapi.place.entity.Place;

import java.time.LocalDate;


/** kakaoCode
 *     105: (chainIdx-1,chainIdx) 과 현재 계산(chainIdx, chainIdx+1) 재 계산 필요
 *     106: 재 계산할 항목이 현재 계산(chainIdx, chainIdx+1)만 필요함
 */
public class EditPlanRoutingRepairRequiredException extends RuntimeException {
    private final LocalDate date;
    private final int failedIdx; // 일자별 연속 구간 경로 계산(computeCarRoutingSummary) 중 문제가 발생한 인덱스
    private final int kakaoCode;
    private final Place origin;
    private final Place destination;
    private final EditPlanRoutingResponse editPlanRoutingResponseSnapShot;

    public EditPlanRoutingRepairRequiredException(LocalDate date, int failedIdx, int kakaoCode, Place origin, Place destination, EditPlanRoutingResponse editPlanRoutingResponseSnapShot) {
        super();
        this.date = date;
        this.failedIdx = failedIdx;
        this.kakaoCode = kakaoCode;
        this.origin = origin;
        this.destination = destination;
        this.editPlanRoutingResponseSnapShot = editPlanRoutingResponseSnapShot;
    }


    public LocalDate getDate() {
        return date;
    }

    public int getFailedIdx() {
        return failedIdx;
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

    public EditPlanRoutingResponse getEditPlanRoutingResponseSnapShot() {
        return editPlanRoutingResponseSnapShot;
    }
}
