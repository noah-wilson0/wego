package com.wego.wego.domain.plan.dto.edit.route;

import com.wego.wego.domain.plan.dto.draft.route.DraftPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.draft.route.RoutingSummary;
import com.wego.wego.external.tourapi.place.entity.Place;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Builder
@AllArgsConstructor
public class EditPlanRepairContext {
    // 필수
    private EditPlanRoutingResponse snapshot;                               // 누적 경료 계산 결과(여러 날 포함)
    private EditPlanRoutingRequest.DailyRouteRequest dailyRouteRequest;           // 현재 일자 spec

    // 디버깅/관측용(선택)
    private LocalDate failedDate;                                        // 실패가 난 일자
    private int failedDailyPlaceIdx;                                          // 실패 인덱스
    private int kakaoCode;                                         // 105/106 코드

    private List<EditPlanRoutingResponse.RouteEdge> routEdgeSnapshot = new ArrayList<>();

    public EditPlanRepairContext(EditPlanRoutingRequest.DailyRouteRequest dailyRouteRequest) {
        this.dailyRouteRequest = dailyRouteRequest;
    }

    public EditPlanRepairContext(EditPlanRoutingResponse snapshot, EditPlanRoutingRequest.DailyRouteRequest dailyRouteRequest) {
        this.snapshot = snapshot;
        this.dailyRouteRequest = dailyRouteRequest;
    }

    public EditPlanRepairContext replaceFailedPlace(Place newPlace) {
        this.dailyRouteRequest.places.set(this.failedDailyPlaceIdx, newPlace);
        return this;

    }
}

