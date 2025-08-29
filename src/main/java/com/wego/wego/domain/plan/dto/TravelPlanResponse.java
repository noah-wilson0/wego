package com.wego.wego.domain.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.entity.TravelPlanDay;
import com.wego.wego.domain.plan.entity.TravelPlanPlace;
import com.wego.wego.domain.plan.entity.TravelPlanRoute;
import com.wego.wego.global.enums.PlaceType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public record TravelPlanResponse(
        LocalDate start_date,
        LocalDate end_date,
        List<DaySchedule> days,
        List<RouteInfo> routes,
        LocalDate createdAt
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
    /** 공용 변환 메서드: TravelPlan → TravelPlanResponse */
    public static TravelPlanResponse from(TravelPlan travelPlan) {

        List<DaySchedule> days = new ArrayList<>();
        List<RouteInfo> routes = new ArrayList<>();

        // 날짜 순으로 보장하고 싶다면 정렬(이미 정렬돼 있으면 생략 가능)
        List<TravelPlanDay> travelPlanDays = new ArrayList<>(travelPlan.getTravelPlanDays());
        travelPlanDays.sort(Comparator.comparing(TravelPlanDay::getDate));

        for (TravelPlanDay day : travelPlanDays) {
            // ---- Places / Accommodation ----
            List<PlaceItem> placeItems = new ArrayList<>();
            AccommodationItem accommodationItem = null;

            List<TravelPlanPlace> dayPlaces = new ArrayList<>(day.getTravelPlanPlaces());
            dayPlaces.sort(Comparator.comparingInt(TravelPlanPlace::getSequence));

            for (TravelPlanPlace tpp : dayPlaces) {
                boolean isAccommodation =
                        PlaceType.ACCOMMODATION.getCode().equals(tpp.getPlace().getPlaceType());

                if (isAccommodation && accommodationItem == null) {
                    accommodationItem = new AccommodationItem(
                            tpp.getPlace().getContentId(),
                            tpp.getPlace().getPlaceType(),
                            tpp.getPlace().getTitle(),
                            tpp.getPlace().getImage(),
                            tpp.getSequence(),
                            tpp.getStartTime(),
                            tpp.getEndTime()
                    );
                } else {
                    placeItems.add(new PlaceItem(
                            tpp.getPlace().getContentId(),
                            tpp.getPlace().getPlaceType(),
                            tpp.getPlace().getTitle(),
                            tpp.getPlace().getImage(),
                            tpp.getSequence(),
                            tpp.getStartTime(),
                            tpp.getEndTime()
                    ));
                }
            }

            // ---- Routes ----
            Map<LocalDate, List<RouteDetail>> dailyRoutes = new HashMap<>();
            List<RouteDetail> routeDetails = new ArrayList<>();

            List<TravelPlanRoute> dayRoutes = new ArrayList<>(day.getTravelPlanRoutes());
            dayRoutes.sort(Comparator.comparingInt(TravelPlanRoute::getSequence));

            for (TravelPlanRoute tpr : dayRoutes) {
                routeDetails.add(new RouteDetail(
                        tpr.getSequence(),
                        tpr.getOrigin().getContentId(),
                        tpr.getDestination().getContentId(),
                        tpr.getDuration()
                ));
            }
            if (!routeDetails.isEmpty()) {
                dailyRoutes.put(day.getDate(), routeDetails);
            }

            String routeType = dayRoutes.isEmpty() ? null : String.valueOf(dayRoutes.get(0).getRouteType());

            // ---- assemble per day ----
            days.add(new DaySchedule(
                    day.getDate(),
                    day.getStartTime(),
                    day.getEndTime(),
                    placeItems,
                    accommodationItem
            ));

            routes.add(new RouteInfo(routeType, dailyRoutes));
        }

        return new TravelPlanResponse(
                travelPlan.getStartDate(),
                travelPlan.getEndDate(),
                days,
                routes,
                travelPlan.getCreatedAt()
        );
    }
}

