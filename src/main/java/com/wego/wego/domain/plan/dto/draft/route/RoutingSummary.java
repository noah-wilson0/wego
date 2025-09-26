package com.wego.wego.domain.plan.dto.draft.route;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Builder
public record RoutingSummary(
        String routeType,                         // "car" | "transit"
        Map<LocalDate, List<RouteLeg>> dailyRoutes
) {
    @Builder
    public record RouteLeg(
            int sequence,         // 1..N
            String origin,        // contentId
            String destination,   // contentId
            int duration         // seconds
    ) {}
}



