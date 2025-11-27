package com.wego.wego.domain.plan.dto.edit.route;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Builder

public record EditPlanRoutingResponse (
        String routeType,                         // "car" | "transit"
        Map<LocalDate, List<RouteEdge>> dailyRoutes
) {
    @Builder
    public record RouteEdge(
            int sequence,         // 1..N
            String origin,        // contentId
            String destination,   // contentId
            int duration         // seconds
    ) { }
}
