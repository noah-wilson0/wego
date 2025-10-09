package com.wego.wego.global.config.dto;

import com.wego.wego.domain.plan.dto.draft.route.DraftPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.draft.route.RoutingSummary;
import com.wego.wego.external.tourapi.place.entity.Place;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class RepairContext {
    // 필수
    private RoutingSummary snapshot;                               // 누적 스냅샷(여러 날 포함)
    private DraftPlanRoutingRequest.RoutingDaySpec spec;           // 현재 일자 spec

    // 디버깅/관측용(선택)
    private LocalDate date;                                        // 실패가 난 일자
    private int chainIdx;                                          // 실패 인덱스
    private int kakaoCode;                                         // 105/106 코드

    private List<RoutingSummary.RouteLeg> routeLegSnapshot = new ArrayList<>();
    private List<Place> chainSnapshot = new ArrayList<>();
    private RoutingSummary routingSummarySnapshot;                 // 하루 반영 포함 스냅샷

    public RepairContext(RoutingSummary snapshot, DraftPlanRoutingRequest.RoutingDaySpec spec) {
        this.snapshot = snapshot;
        this.spec = spec;
    }
}

