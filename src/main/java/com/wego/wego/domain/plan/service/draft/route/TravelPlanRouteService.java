package com.wego.wego.domain.plan.service.draft.route;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.*;
import com.wego.wego.domain.plan.dto.draft.route.DraftPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.draft.route.RoutingSummary;
import com.wego.wego.domain.plan.dto.draft.route.TravelPlanRouteJson;
import com.wego.wego.domain.plan.util.RouteScheduleUtil;
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

import java.time.LocalDate;
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


    public String saveScheduleRoute(String uuid, String routeType) {
        String responseJson; // DraftPlanResponse JSON 반환
        String slug;
        String slugJson = redisTemplate.opsForValue().get(RedisKeyUtils.slugKey(uuid));
        String dateJson = redisTemplate.opsForValue().get(RedisKeyUtils.dateKey(uuid));
        String timesJson = redisTemplate.opsForValue().get(RedisKeyUtils.timeKey(uuid));
        String placesJson = redisTemplate.opsForValue().get(RedisKeyUtils.placesKey(uuid));
        String accommodationsJson = redisTemplate.opsForValue().get(RedisKeyUtils.accommodationsKey(uuid));

        TempTravelDateRequest tempTravelDateRequests;
        TempTravelTimeRequest tempTravelTimeRequests;
        List<TempTravelPlanPlaceRequest> tempTravelPlanPlaceRequests;
        List<TempTravelPlanAccommodationRequest> tempTravelPlanAccommodationRequests;

        try {
            slug = objectMapper.readTree(slugJson).get("slug").asText();
            tempTravelDateRequests = objectMapper.readValue(dateJson, TempTravelDateRequest.class);
            tempTravelTimeRequests = objectMapper.readValue(timesJson, TempTravelTimeRequest.class);
            tempTravelPlanPlaceRequests = objectMapper.readValue(placesJson, new TypeReference<>() {});
            tempTravelPlanAccommodationRequests = objectMapper.readValue(accommodationsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.info("여행 경로 데이터 통합 중 객체화 실패");
            throw new RuntimeException(e);
        }
        /**
         *  DraftPlanRoutingRequest
         *      로 매핑한 객체를  getRouteByType에 넘기고 그 이후 파라미터도 동일하다.
         *      이를 통해 MAP<LocalDate,List<RouteResult>>으로 반환받지 말고
         *      RoutingSummary같은 객체를 만들어서 그걸 반환받고 RoutingSummary와 DailyRouteInputReqeust로
         *      DraftPlanResponse 만들기
         */

        //여행 장소/숙소 테이블
        Map<String, Place> placeMap = placeService.findByContentIdIn(
                Stream.concat(
                        tempTravelPlanPlaceRequests.stream().map(TempTravelPlanPlaceRequest::contentId),
                        tempTravelPlanAccommodationRequests.stream().map(TempTravelPlanAccommodationRequest::contentId)
                ).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(Place::getContentId, Function.identity()));

        //여행 장소 데이터
//        List<Place> places = placeService.findByContentIdIn(
//                tempTravelPlanPlaceRequests.stream().map(TempTravelPlanPlaceRequest::contentId).toList()
//        );

        //여행 일자별 숙소 데이터
        Map<LocalDate, Place> dayOfAccom = new HashMap();
        for (TempTravelPlanAccommodationRequest tempAccom : tempTravelPlanAccommodationRequests) {
            dayOfAccom.put(tempAccom.date(), placeMap.get(tempAccom.contentId()));
        }

        //경로 요청 객체 준비
        List<DraftPlanRoutingRequest.RoutingDaySpec> daySpecs = new ArrayList<>();
        int totalDays = tempTravelTimeRequests.travelDayTimes().size();
        double placeMaxSize = Math.ceil(tempTravelPlanPlaceRequests.size() / totalDays);
        int placeIndex=0;
        for (TempTravelTimeRequest.TravelDayTimes tempTravelTimeRequest : tempTravelTimeRequests.travelDayTimes()) {
            List<Place> routingPlaces = new ArrayList<>();

            while (placeIndex < tempTravelPlanPlaceRequests.size()
                    && routingPlaces.size() < placeMaxSize) {
                TempTravelPlanPlaceRequest req = tempTravelPlanPlaceRequests.get(placeIndex++);
                Place p = placeMap.get(req.contentId());
                if (p != null) routingPlaces.add(p);

            }

            daySpecs.add(DraftPlanRoutingRequest.RoutingDaySpec
                    .builder()
                    .date(tempTravelTimeRequest.date())
                    .start_time(tempTravelTimeRequest.startTime())
                    .end_time(tempTravelTimeRequest.endTime())
                    .places(routingPlaces)
                    .accommodation(dayOfAccom.containsKey(tempTravelTimeRequest.date()) ?
                            dayOfAccom.get(tempTravelTimeRequest.date()) : null)
                    .build());
        }

        //경로 요청 객체 생성 완료
        DraftPlanRoutingRequest draftPlanRoutingRequest = new DraftPlanRoutingRequest(daySpecs);

        RoutingSummary routingSummary = getRouteByType(routeType, draftPlanRoutingRequest);

        //조립 - 여행 장소 숙소
        List<DraftPlanResponse.DaySchedule> days = new ArrayList<>();

        for (DraftPlanRoutingRequest.RoutingDaySpec daySpec : draftPlanRoutingRequest.days()) {
            DraftPlanResponse.DaySchedule daySchedule = RouteScheduleUtil.toDaySchedule(daySpec, routingSummary.dailyRoutes().get(daySpec.date()));

            days.add(daySchedule);
        }
        //조립 - 여행 경로
//        List<DraftPlanResponse.RouteInfo> routes = new ArrayList<>();

        // 기존 수동 매핑 대신 한 줄로
        List<DraftPlanResponse.RouteInfo> routes = List.of(
                DraftPlanResponse.RouteInfo.from(routingSummary)
        );




        DraftPlanResponse draftPlanResponse = DraftPlanResponse
                .builder()
                .slug(slug)
                .start_date(tempTravelDateRequests.startDate())
                .end_date(tempTravelDateRequests.endDate())
                .days(days)
                .routes(routes)
                .build();

        try {
            // DraftPlanResponse JSON을 tempScheduleKey에 저장
            responseJson = objectMapper.writeValueAsString(draftPlanResponse);
            redisTemplate.opsForValue().set(RedisKeyUtils.tempScheduleKey(uuid), responseJson, 6, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.info("경로 정보 redis 저장 실패");
            throw new RuntimeException(e);
        }

        return responseJson;

    }





    private RoutingSummary getRouteByType(String routeType, DraftPlanRoutingRequest draftPlanRoutingRequest) {
        return switch (routeType) {
            case "car" -> getCarKaKaoMobility(draftPlanRoutingRequest);
            case "transit" -> getPublicTransTMap(draftPlanRoutingRequest);
            default -> throw new IllegalArgumentException("지원하지 않는 경로 타입입니다: " + routeType);
        };
    }

    private RoutingSummary getCarKaKaoMobility (DraftPlanRoutingRequest draftPlanRoutingRequest) {

        Map<LocalDate, List<RoutingSummary.RouteLeg>> dailyRoutes = new HashMap<>();

        for (DraftPlanRoutingRequest.RoutingDaySpec routingDaySpec: draftPlanRoutingRequest.days()) {
            List<RoutingSummary.RouteLeg> routeLegs = new ArrayList<>();
            List<Place> places = routingDaySpec.places();
            if (routingDaySpec.accommodation() != null) places.add(routingDaySpec.accommodation());

            for (int i = 0; i < places.size() - 1; i++) {
                RouteResult routeResult = kaKaoMobilityFetchService.fetchKaKaoMobilityData(
                        places.get(i),
                        places.get(i + 1)
                );
                routeLegs.add(RoutingSummary.RouteLeg.builder()
                                .sequence(i+1)
                                .origin(routeResult.getOriginId())
                                .destination(routeResult.getDestinationId())
                                .duration(routeResult.getDuration())
                        .build());
            }

            dailyRoutes.put(routingDaySpec.date(), routeLegs);
        }
        return new RoutingSummary("car", dailyRoutes);
    }

    private RoutingSummary getPublicTransTMap(DraftPlanRoutingRequest draftPlanRoutingRequest) {
        Map<LocalDate, List<RoutingSummary.RouteLeg>> dailyRoutes = new HashMap<>();

        for (DraftPlanRoutingRequest.RoutingDaySpec routingDaySpec: draftPlanRoutingRequest.days()) {
            List<RoutingSummary.RouteLeg> routeLegs = new ArrayList<>();
            List<Place> places = routingDaySpec.places();
            if (routingDaySpec.accommodation() != null) places.add(routingDaySpec.accommodation());

            for (int i = 0; i < places.size() - 1; i++) {
                RouteResult routeResult = tMapTransitService.fetchTMapTransitData(
                        places.get(i),
                        places.get(i + 1)
                );
                routeLegs.add(RoutingSummary.RouteLeg.builder()
                        .sequence(i+1)
                        .origin(routeResult.getOriginId())
                        .destination(routeResult.getDestinationId())
                        .duration(routeResult.getDuration())
                        .build());
            }

            dailyRoutes.put(routingDaySpec.date(), routeLegs);
        }
        return new RoutingSummary("transit", dailyRoutes);
    }
}
