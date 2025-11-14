package com.wego.wego.domain.plan.dto.edit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.entity.TravelPlanDay;
import com.wego.wego.domain.plan.entity.TravelPlanRoute;
import com.wego.wego.external.tourapi.place.entity.Place;
import lombok.AllArgsConstructor;
import lombok.With;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 *  accommodation이 없는 dto 여행 일정 편집 화면에 필요한 dto
 * @param label
 * @param start_date
 * @param end_date
 * @param days
 * @param routes
 * @param createdAt
 */

public record TravelPlanNormalizeResponse(
        @With
        String label,
        LocalDate start_date,
        LocalDate end_date,
        List<TravelPlanNormalizeResponse.DaySchedule> days,
        List<TravelPlanNormalizeResponse.RouteInfo> routes,
        LocalDate createdAt
) {

    public record DaySchedule(
            LocalDate date,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
            LocalTime start_time,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
            LocalTime end_time,
            List<TravelPlanNormalizeResponse.PlaceItem> places
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

    public record RouteInfo(
            String route_type,
            Map<LocalDate , List<TravelPlanNormalizeResponse.RouteItem>> dailyRoutes
    ) {}

    public record RouteItem(
            int sequence,
            String origin,
            String destination,
            int duration
    ) {}

    public static TravelPlanNormalizeResponse of(
            String label,
            LocalDate start_date,
            LocalDate end_date,
            List<DaySchedule> days,
            List<RouteInfo> routes,
            LocalDate createdAt
    ) {
        return new TravelPlanNormalizeResponse(label, start_date, end_date, days, routes, createdAt);
    }

    public TravelPlanNormalizeResponse(TravelPlan plan) {
        this(
                plan.getSlug(),                        // slug 조립
                plan.getStartDate(),                   // start_date
                plan.getEndDate(),                     // end_date
                toDaySchedules(plan),                  // days 조립
                toRouteInfos(plan),                    // routes 조립
                plan.getCreatedAt()    // createdAt 타입 맞추기
        );
    }

    private static List<DaySchedule> toDaySchedules(TravelPlan plan) {
        return plan.getTravelPlanDays().stream()
                .sorted(Comparator.comparing(TravelPlanDay::getDate))
                .map(travelPlanDay -> {
                    List<PlaceItem> placeItems = travelPlanDay.getTravelPlanPlaces()
                            .stream().sorted(Comparator.comparingInt(p -> p.getSequence()))
                            .map(travelPlanPlace -> {
                                Place place = travelPlanPlace.getPlace();
                                return new PlaceItem(
                                        place.getContentId(),
                                        place.getPlaceType(),
                                        place.getTitle(),
                                        place.getImage(),
                                        travelPlanPlace.getSequence(),
                                        Double.valueOf(place.getLongitude()),
                                        Double.valueOf(place.getLatitude()),
                                        travelPlanPlace.getStartTime(),
                                        travelPlanPlace.getEndTime()
                                );
                            }).toList();

                    return new DaySchedule(
                            travelPlanDay.getDate(),
                            travelPlanDay.getStartTime(),
                            travelPlanDay.getEndTime(),
                            placeItems
                    );
                }).toList();
    }

    private static List<RouteInfo> toRouteInfos(TravelPlan plan) {
        Map<LocalDate, List<RouteItem>> dailyRoute = new LinkedHashMap<>();
        AtomicReference<String> routeTypeRef = new AtomicReference<>(null);
        plan.getTravelPlanDays().stream()
                .sorted(Comparator.comparing(TravelPlanDay::getDate))
                .forEach(travelPlanDay -> {
                    List<RouteItem> routeItems = travelPlanDay.getTravelPlanRoutes().stream()
                            .sorted(Comparator.comparingInt(TravelPlanRoute::getSequence))
                            .map(travelPlanRoute -> {
                                return new RouteItem(
                                        travelPlanRoute.getSequence(),
                                        travelPlanRoute.getOrigin().getContentId(),
                                        travelPlanRoute.getDestination().getContentId(),
                                        travelPlanRoute.getDuration()
                                );
                            }).toList();
                    routeTypeRef.set(travelPlanDay.getTravelPlanRoutes().getFirst().getRouteType().toString());
                    dailyRoute.put(travelPlanDay.getDate(), routeItems);

                });

        return List.of(new RouteInfo(routeTypeRef.get(), dailyRoute));
    }


}


