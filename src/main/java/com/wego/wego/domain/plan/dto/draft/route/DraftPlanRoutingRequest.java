package com.wego.wego.domain.plan.dto.draft.route;

import com.wego.wego.external.tourapi.place.entity.Place;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Builder
public record DraftPlanRoutingRequest(
        List<RoutingDaySpec> days
) {
    @Builder
    public record RoutingDaySpec(
            LocalDate date,
            LocalTime start_time,
            LocalTime end_time,
            List<Place> places,        // 방문 순서대로
            Place accommodation        // 당일 숙소(없으면 null)
    ) {}
}
