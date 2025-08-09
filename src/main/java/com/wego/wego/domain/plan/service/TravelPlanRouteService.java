package com.wego.wego.domain.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.*;
import com.wego.wego.external.route.dto.RouteResult;
import com.wego.wego.external.route.kakao.sevice.KaKaoMobilityFetchService;
import com.wego.wego.external.route.tmap.service.TMapTransitService;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.service.PlaceService;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanRouteService {

    private final KaKaoMobilityFetchService kaKaoMobilityFetchService;
    private final TMapTransitService tMapTransitService;
    private final PlaceService placeService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public String getScheduleRoute(String uuid, String route_type) {
        String route;
        String dateJson = redisTemplate.opsForValue().get(RedisKeyUtils.dateKey(uuid));
        String timesJson = redisTemplate.opsForValue().get(RedisKeyUtils.timeKey(uuid));
        String placesJson = redisTemplate.opsForValue().get(RedisKeyUtils.placesKey(uuid));
        String accommodationsJson = redisTemplate.opsForValue().get(RedisKeyUtils.accommodationsKey(uuid));

        TempTravelDateRequest tempTravelDateRequests;
        TempTravelTimeRequest tempTravelTimeRequests;
        List<TempTravelPlanPlaceRequest> tempTravelPlanPlaceRequests;
        List<TempTravelPlanAccommodationRequest> tempTravelPlanAccommodationRequests;

        try {
            tempTravelDateRequests = objectMapper.readValue(dateJson, TempTravelDateRequest.class);
            tempTravelTimeRequests = objectMapper.readValue(timesJson, TempTravelTimeRequest.class);
            tempTravelPlanPlaceRequests = objectMapper.readValue(placesJson, new TypeReference<>() {});
            tempTravelPlanAccommodationRequests = objectMapper.readValue(accommodationsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.info("여행 경로 데이터 통합 중 객체화 실패");
            throw new RuntimeException(e);
        }

        Map<String, Place> placeMap = placeService.findByContentIdIn(
                Stream.concat(
                        tempTravelPlanPlaceRequests.stream().map(TempTravelPlanPlaceRequest::contentId),
                        tempTravelPlanAccommodationRequests.stream().map(TempTravelPlanAccommodationRequest::contentId)
                ).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(Place::getContentId, Function.identity()));

        List<TravelPlanRouteJson.DaySchedule> daySchedules = new ArrayList<>();
        List<TravelPlanRouteJson.RouteInfo> routes = new ArrayList<>();

        int totalDays = tempTravelTimeRequests.travelDayTimes().size();
        int placeIndex = 0;
        List<TravelPlanRouteJson.PlaceItem> places=null;
        Map<LocalDate, List<Place>> dayPlaceListForRoute = new LinkedHashMap<>();
        Map<LocalDate, List<RouteResult>> dailyRoute = new HashMap<>();

        for (int i = 0; i < totalDays; i++) {
            TempTravelTimeRequest.TravelDayTimes travelDayTimes = tempTravelTimeRequests.travelDayTimes().get(i);
            LocalDate date = travelDayTimes.date();
            LocalTime startTime = travelDayTimes.startTime();
            LocalTime endTime = travelDayTimes.endTime();

            //  장소 구성
            List<Place> placesInDay = new ArrayList<>();
            while (placeIndex < tempTravelPlanPlaceRequests.size()
                    && placesInDay.size() < (int) Math.ceil((double) tempTravelPlanPlaceRequests.size() / totalDays)) {
                TempTravelPlanPlaceRequest req = tempTravelPlanPlaceRequests.get(placeIndex++);
                Place p = placeMap.get(req.contentId());
                if (p != null) placesInDay.add(p);
            }

            // 숙소 추가
            Place accommodationPlace = null;
            if (i<totalDays-1 && date.equals(tempTravelPlanAccommodationRequests.get(i).date())) {
                accommodationPlace = placeMap.get(tempTravelPlanAccommodationRequests.get(i).contentId());
                placesInDay.add(accommodationPlace); // 숙소 맨 뒤 추가
            }

            //  경로 계산
            Map<LocalDate, List<Place>> routeInputMap = Map.of(date, placesInDay);
            Map<LocalDate, List<RouteResult>> todayRoute = getRouteByType(route_type, routeInputMap);
            log.info(todayRoute.get(date).toString());
            log.info(String.valueOf(todayRoute.size()));
            dailyRoute.putAll(todayRoute);

            List<RouteResult> routeResults = todayRoute.get(date);
            List<TravelPlanRouteJson.RouteDetail> routeDetails = new ArrayList<>();
            for (int j = 0; j < routeResults.size(); j++) {
                RouteResult r = routeResults.get(j);
                routeDetails.add(new TravelPlanRouteJson.RouteDetail(
                        j + 1,
                        r.getOriginId(),
                        r.getDestinationId(),
                        r.getFare(),
                        r.getDistance(),
                        r.getDuration()
                ));
            }
            routes.add(new TravelPlanRouteJson.RouteInfo(route_type, Map.of(date, routeDetails)));

            // 🔹 체류 시간 계산
            int totalDurationSec = (int) Duration.between(startTime, endTime).getSeconds();
            int totalMoveSec = routeResults.stream().mapToInt(RouteResult::getDuration).sum();
            int stayTimePerPlaceSec = (totalDurationSec - totalMoveSec) / placesInDay.size();

            // 🔹 장소 + 숙소 객체 구성
            List<TravelPlanRouteJson.PlaceItem> placeItems = new ArrayList<>();
            TravelPlanRouteJson.AccommodationItem accommodationItem = null;
            LocalTime currentTime = startTime;

            for (int k = 0; k < placesInDay.size(); k++) {
                Place p = placesInDay.get(k);
                LocalTime itemStart = currentTime;
                LocalTime itemEnd = itemStart.plusSeconds(stayTimePerPlaceSec);

                if (k < routeResults.size()) {
                    currentTime = itemEnd.plusSeconds(routeResults.get(k).getDuration());
                }

                if (p.getPlaceType().equals("B01")) {
                    accommodationItem = new TravelPlanRouteJson.AccommodationItem(
                            p.getContentId(),
                            k + 1,
                            itemStart,
                            itemEnd
                    );
                } else {
                    placeItems.add(new TravelPlanRouteJson.PlaceItem(
                            p.getContentId(),
                            k + 1,
                            itemStart,
                            itemEnd
                    ));
                }
            }

            //  daySchedule 생성 및 추가
            daySchedules.add(new TravelPlanRouteJson.DaySchedule(
                    date,
                    startTime,
                    endTime,
                    placeItems,
                    accommodationItem
            ));
        } //for i

        TravelPlanRouteJson schedule = new TravelPlanRouteJson(
                tempTravelDateRequests.startDate(),
                tempTravelDateRequests.endDate(),
                daySchedules,
                routes
        );

        try {
            route = objectMapper.writeValueAsString(schedule);
            redisTemplate.opsForValue().set(RedisKeyUtils.routeKey(uuid), route,6, TimeUnit.HOURS); //car,transit 어떤것이든 json안에 route_type이 존재하므로 key도 달리줄 필요는 없다. 조회 시점에 car,transit인지 모르기 때문에 이슈가 생김
        } catch (JsonProcessingException e) {
            log.info("경로 정보 redis 저장 실패");
            throw new RuntimeException(e);
        }
        return route;
    }

    private Map<LocalDate, List<RouteResult>> getRouteByType(String routeType, Map<LocalDate, List<Place>> routeInputMap) {
        return switch (routeType) {
            case "car" -> getCarKaKaoMobility(routeInputMap);
            case "transit" -> getPublicTransTMap(routeInputMap);
            default -> throw new IllegalArgumentException("지원하지 않는 경로 타입입니다: " + routeType);
        };
    }

    private Map<LocalDate, List<RouteResult>> getCarKaKaoMobility(Map<LocalDate, List<Place>> routeInputMap) {
        Map<LocalDate, List<RouteResult>> dailyRoute = new HashMap<>();

        for (Map.Entry<LocalDate, List<Place>> entry : routeInputMap.entrySet()) {
            List<Place> dayPlaces = entry.getValue();
            List<RouteResult> routes = new ArrayList<>();

            for (int i = 0; i < dayPlaces.size() - 1; i++) {
                routes.add(kaKaoMobilityFetchService.fetchKaKaoMobilityData(
                        dayPlaces.get(i),
                        dayPlaces.get(i + 1)
                ));
            }
            dailyRoute.put(entry.getKey(), routes);
        }

        return dailyRoute;
    }

    private Map<LocalDate, List<RouteResult>> getPublicTransTMap(Map<LocalDate, List<Place>> routeInputMap ) {
        // 추후 대중교통 경로 처리
        Map<LocalDate, List<RouteResult>> dailyRoute = new HashMap<>();

        for (Map.Entry<LocalDate, List<Place>> entry : routeInputMap.entrySet()) {
            List<Place> dayPlaces = entry.getValue();
            List<RouteResult> routes = new ArrayList<>();

            for (int i = 0; i < dayPlaces.size() - 1; i++) {
                routes.add(tMapTransitService.fetchTMapTransitData(
                        dayPlaces.get(i),
                        dayPlaces.get(i + 1)
                ));
            }
            dailyRoute.put(entry.getKey(), routes);
        }
        return dailyRoute;
    }
}
