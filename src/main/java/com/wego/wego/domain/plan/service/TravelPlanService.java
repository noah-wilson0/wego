package com.wego.wego.domain.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.TempTravelPlanResponse;
import com.wego.wego.domain.plan.dto.TravelPlanRouteJson;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.service.PlaceService;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanService {

    private final PlaceService placeService;

    private final RedisTemplate<String,String > redisTemplate;
    private final ObjectMapper objectMapper;



    public String getTempTravelPlan(String uuid) {

        String routes = redisTemplate.opsForValue().get(RedisKeyUtils.routeKey(uuid));
        TravelPlanRouteJson travelPlanRouteJson;
        try {
            travelPlanRouteJson = objectMapper.readValue(routes, TravelPlanRouteJson.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        List<TempTravelPlanResponse.DaySchedule> newDays=new ArrayList<>();
        for (int i = 0; i < travelPlanRouteJson.days().size(); i++) {
            TravelPlanRouteJson.DaySchedule daySchedule = travelPlanRouteJson.days().get(i);

            //places 생성
            List<TempTravelPlanResponse.PlaceItem> newPlaces=new ArrayList<>();
            for (int j = 0; j < daySchedule.places().size(); j++) {
                TravelPlanRouteJson.PlaceItem placeItem = daySchedule.places().get(j);
                Place place = placeService.findByContentId(placeItem.content_id()).get();
                newPlaces.add(new TempTravelPlanResponse.PlaceItem(
                        placeItem.content_id(),
                        place.getPlaceType(),
                        place.getTitle(),
                        place.getImage(),
                        placeItem.sequence(),
                        placeItem.start_time(),
                        placeItem.end_time()
                ));
            }

            //accommodation 생성
            TempTravelPlanResponse.AccommodationItem newAccommodationItem = null;

            if (daySchedule.accommodation() != null) {
                Place accommodation = placeService.findByContentId(daySchedule.accommodation().content_id()).get();
                newAccommodationItem=new TempTravelPlanResponse.AccommodationItem(
                        daySchedule.accommodation().content_id(),
                        accommodation.getPlaceType(),
                        accommodation.getTitle(),
                        accommodation.getImage(),
                        daySchedule.accommodation().sequence(),
                        daySchedule.accommodation().start_time(),
                        daySchedule.accommodation().end_time()
                );
            }

            //DaySchedule 추가
            newDays.add(new TempTravelPlanResponse.DaySchedule(
                    daySchedule.date(),
                    daySchedule.start_time(),
                    daySchedule.end_time(),
                    newPlaces,
                    newAccommodationItem
            ));
        }

        List<TempTravelPlanResponse.RouteInfo> newRoutes=new ArrayList<>();
        for (int i = 0; i < travelPlanRouteJson.routes().size(); i++) {
            TravelPlanRouteJson.RouteInfo routeInfo = travelPlanRouteJson.routes().get(i);

            Map<LocalDate,List<TempTravelPlanResponse.RouteDetail>> newDailyRoutes=new HashMap<>();
            List<TempTravelPlanResponse.RouteDetail> newRouteDetails = new ArrayList<>();

            List<TravelPlanRouteJson.RouteDetail> routeDetails = routeInfo.daily_route().get(travelPlanRouteJson.days().get(i).date());
            for (int j = 0; j < routeDetails.size(); j++) {
                TempTravelPlanResponse.RouteDetail newRouteDetail = new TempTravelPlanResponse.RouteDetail(
                        routeDetails.get(j).sequence(),
                        routeDetails.get(j).origin(),
                        routeDetails.get(j).destination(),
                        routeDetails.get(j).duration()
                );
                newRouteDetails.add(newRouteDetail);
            }
            newDailyRoutes.put(travelPlanRouteJson.days().get(i).date(),newRouteDetails);
            newRoutes.add(new TempTravelPlanResponse.RouteInfo(routeInfo.route_type(), newDailyRoutes));
        }

        TempTravelPlanResponse tempTravelPlanResponse=new TempTravelPlanResponse(
                travelPlanRouteJson.start_date(),
                travelPlanRouteJson.end_date(),
                newDays,
                newRoutes
        );
        String response;
        try {
            response = objectMapper.writeValueAsString(tempTravelPlanResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        redisTemplate.opsForValue().set(RedisKeyUtils.tempScheduleKey(uuid), response);

        return response;
    }

    public void persistTravelPlan(String uuid) {
        String json = redisTemplate.opsForValue().get(RedisKeyUtils.tempScheduleKey(uuid));
        TempTravelPlanResponse tempTravelPlanResponse;
        try {
            tempTravelPlanResponse = objectMapper.readValue(json, TempTravelPlanResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

}
