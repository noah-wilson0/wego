package com.wego.wego.domain.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wego.wego.domain.plan.dto.draft.route.DraftPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.draft.route.RoutingSummary;
import lombok.Builder;
import lombok.With;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Builder
public record DraftPlanResponse(
        @With String slug,
        LocalDate start_date,
        LocalDate end_date,
        List<DaySchedule> days,
        List<RouteInfo> routes //TODO List일 필요가 없음 unboxed가능 temp json의 routes보면 알 수 있음
) {
    @Builder
    public record DaySchedule(
            LocalDate date,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
            LocalTime start_time,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
            LocalTime end_time,
            List<PlaceItem> places,
            AccommodationItem accommodation
    ) {}
    @Builder
    public record PlaceItem(
            String content_id,
            String placeType,
            String title,
            String image,
            int sequence,
            double longitude,
            double latitude,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
            LocalTime start_time,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
            LocalTime end_time
    ) {}
    @Builder
    public record AccommodationItem(
            String content_id,
            String placeType,
            String title,
            String image,
            int sequence,
            double longitude,
            double latitude,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
            LocalTime start_time,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
            LocalTime end_time
    ) {}
    @Builder
    public record RouteInfo(
            String route_type,
            Map<LocalDate, List<RouteDetail>> daily_route
    ) {
        public static RouteInfo from(RoutingSummary summary) {
            if (summary == null) {
                return RouteInfo.builder()
                        .route_type(null)
                        .daily_route(Map.of())
                        .build();
            }

            Map<LocalDate, List<RouteDetail>> map = new LinkedHashMap<>();
            for (Map.Entry<LocalDate, List<RoutingSummary.RouteLeg>> entry : summary.dailyRoutes().entrySet()) {
                map.put(entry.getKey(),
                        RouteDetail.toDetails(entry.getValue()));
            }

            return RouteInfo.builder()
                    .route_type(summary.routeType())
                    .daily_route(map)
                    .build();
        }
    }
    @Builder
    public record RouteDetail(
            int sequence,
            String origin,
            String destination,
            int duration
    ) {
        private static List<RouteDetail> toDetails(List<RoutingSummary.RouteLeg> legs) {
            List<RouteDetail> details = new ArrayList<>();
            for (int i = 0; i < legs.size(); i++) {
                RoutingSummary.RouteLeg l = legs.get(i);
                details.add(RouteDetail.builder()
                        .sequence(l.sequence())
                        .origin(l.origin())
                        .destination(l.destination())
                        .duration(l.duration())
                        .build());
            }
            return details;
        }
    }

}

