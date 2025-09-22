package com.wego.wego.domain.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.With;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record DraftTravelPlanResponse(
        @With String slug,
        LocalDate start_date,
        LocalDate end_date,
        List<DaySchedule> days,
        List<RouteInfo> routes
) {

    public record DaySchedule(
            LocalDate date,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
            LocalTime start_time,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
            LocalTime end_time,
            List<PlaceItem> places,
            AccommodationItem accommodation
    ) {}

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

    public record RouteInfo(
            String route_type,
            Map<LocalDate, List<RouteDetail>> daily_route
    ) {}

    public record RouteDetail(
            int sequence,
            String origin,
            String destination,
            int duration
    ) {}

}

