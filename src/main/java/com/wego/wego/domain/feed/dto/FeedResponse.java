package com.wego.wego.domain.feed.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.wego.wego.domain.feed.entity.*;
import com.wego.wego.global.enums.PlaceType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public record FeedResponse(
        Long feed_id,
        String author,
        String title,
        String body,
        int people,
        int like_count,
        int view_count,
        LocalDate created_at,

        // 피드용 여행 계획 요약
        String slug,
        LocalDate start_date,
        LocalDate end_date,

        // 케미 태그
        List<ChemiItem> chemis,

        // 일차별 상세
        List<DaySchedule> days,
        List<RouteInfo> routes
) {

    public record ChemiItem(
            Long id,
            String name,
            String image,
            String description
    ) {}

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

    // ===== factory =====

    public static FeedResponse from(Feed feed) {
        FeedTravelPlan fp = feed.getFeedTravelPlan();
        Objects.requireNonNull(fp, "FeedTravelPlan must not be null");

        // 케미 태그 변환
        List<ChemiItem> chemiItems = feed.getFeedChemiTags().stream()
                .map(FeedChemiTag::getChemi)
                .filter(Objects::nonNull)
                .map(c -> new ChemiItem(c.getId(), c.getName(), c.getImage(), c.getDescription()))
                .toList();

        // 일차/경로 변환
        List<DaySchedule> days = new ArrayList<>();
        List<RouteInfo> routes = new ArrayList<>();

        List<FeedTravelPlanDay> planDays = new ArrayList<>(fp.getFeedTravelPlanDays());
        planDays.sort(Comparator.comparing(FeedTravelPlanDay::getDate));

        for (FeedTravelPlanDay day : planDays) {
            // ----- places & accommodation -----
            List<PlaceItem> placeItems = new ArrayList<>();
            AccommodationItem accommodationItem = null;

            List<FeedTravelPlanPlace> dayPlaces = new ArrayList<>(day.getTravelPlanPlaces());
            dayPlaces.sort(Comparator.comparingInt(FeedTravelPlanPlace::getSequence));

            for (FeedTravelPlanPlace p : dayPlaces) {
                String placeType = p.getPlace().getPlaceType();
                boolean isAccommodation = PlaceType.ACCOMMODATION.getCode().equals(placeType);

                if (isAccommodation && accommodationItem == null) {
                    accommodationItem = new AccommodationItem(
                            p.getPlace().getContentId(),
                            placeType,
                            p.getPlace().getTitle(),
                            p.getPlace().getImage(),
                            p.getSequence(),
                            p.getStartTime(),
                            p.getEndTime()
                    );
                } else {
                    placeItems.add(new PlaceItem(
                            p.getPlace().getContentId(),
                            placeType,
                            p.getPlace().getTitle(),
                            p.getPlace().getImage(),
                            p.getSequence(),
                            p.getStartTime(),
                            p.getEndTime()
                    ));
                }
            }

            // ----- routes -----
            Map<LocalDate, List<RouteDetail>> dailyRoutes = new HashMap<>();
            List<RouteDetail> routeDetails = new ArrayList<>();

            List<FeedTravelPlanRoute> dayRoutes = new ArrayList<>(day.getTravelPlanRoutes());
            dayRoutes.sort(Comparator.comparingInt(FeedTravelPlanRoute::getSequence));

            for (FeedTravelPlanRoute r : dayRoutes) {
                routeDetails.add(new RouteDetail(
                        r.getSequence(),
                        r.getOrigin().getContentId(),
                        r.getDestination().getContentId(),
                        r.getDuration()
                ));
            }
            if (!routeDetails.isEmpty()) {
                dailyRoutes.put(day.getDate(), routeDetails);
            }

            String routeType = dayRoutes.get(0).getRouteType().name();

            // ----- assemble -----
            days.add(new DaySchedule(
                    day.getDate(),
                    day.getStartTime(),
                    day.getEndTime(),
                    placeItems,
                    accommodationItem
            ));
            routes.add(new RouteInfo(routeType, dailyRoutes));
        }

        return new FeedResponse(
                feed.getId(),
                feed.getMember().getName(),
                feed.getTitle(),
                feed.getBody(),
                feed.getPeople(),
                feed.getLikeCount(),
                feed.getViewCount(),
                feed.getCreatedAt(),
                fp.getSlug(),
                fp.getStartDate(),
                fp.getEndDate(),
                chemiItems,
                days,
                routes
        );
    }


}
