package com.wego.wego.domain.plan.service.edit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.edit.EditLanggraphRequest;
import com.wego.wego.domain.plan.dto.edit.TravelPlanEditGeminiResponse;
import com.wego.wego.domain.plan.dto.edit.TravelPlanNormalizeResponse;
import com.wego.wego.domain.plan.dto.edit.route.EditPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.edit.route.EditPlanRoutingResponse;
import com.wego.wego.domain.plan.support.SlugResolver;
import com.wego.wego.domain.plan.util.NormalizeStayTimeAllocator;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.service.PlaceService;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@Transactional(readOnly = true)
public class TravelPlanLangGraphService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Qualifier("AutoWebClient")
    private final WebClient autoWebClient;

    private final PlaceService placeService;
    private final EditPlanRoutingCalculator editPlanRoutingCalculator;


    private final SlugResolver slugResolver;

    public TravelPlanLangGraphService(RedisTemplate<String, String> redisTemplate,
                                      ObjectMapper objectMapper,
                                      @Qualifier("AutoWebClient") WebClient autoWebClient,
                                      PlaceService placeService, EditPlanRoutingCalculator editPlanRoutingCalculator,
                                      SlugResolver slugResolver) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.autoWebClient = autoWebClient;
        this.placeService = placeService;
        this.editPlanRoutingCalculator = editPlanRoutingCalculator;
        this.slugResolver = slugResolver;
    }


    public TravelPlanNormalizeResponse getAiEditTravelPlan(Long memberId, Long travelPlanId, String prompt) {
        String key = RedisKeyUtils.editTravelPlanKey(String.valueOf(memberId), String.valueOf(travelPlanId));
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new IllegalStateException("편집본이 Redis에 없습니다: key=" + key);
        }
        TravelPlanNormalizeResponse travelPlanNormalizeResponse;
        try {
            travelPlanNormalizeResponse = objectMapper.readValue(json, TravelPlanNormalizeResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 편집본 파싱 실패", e);
        }
        log.info("prompt:{}",prompt);
        log.info("travelPlan:{}",travelPlanNormalizeResponse.toString());
        EditLanggraphRequest editLanggraphRequest = new EditLanggraphRequest(prompt, travelPlanNormalizeResponse);

        TravelPlanEditGeminiResponse geminiResponse = autoWebClient.post()
                .uri("/ai/edit-travel-plan")
                .bodyValue(editLanggraphRequest)
                .retrieve()
                .bodyToMono(TravelPlanEditGeminiResponse.class)
                .block();

        log.info("/ai/edit-travel-plan 응답결과: " + geminiResponse.toString());

        // 장소 재배열 후 경로 계산을 한다.
        // 단일, 복합 장소 수정시 사용될 수 있도록 한다.
        // 전체 수정은 다른 코드에서 해결한다.

        List<String> allIds = geminiResponse.changes().stream()
                .flatMap(c -> Stream.of(
                        c.beforePlace().contentId(),
                        c.afterPlace().contentId()
                ))
                .collect(Collectors.toList());


        Map<String, Place> updatedPlaceMap = placeService.findAllByContentIdIn(allIds).stream()
                .collect(Collectors.toMap(Place::getContentId, p -> p));

        EditPlanRoutingRequest editPlanRoutingRequest = EditPlanRoutingRequest.builder().build();

        for (TravelPlanNormalizeResponse.DaySchedule daySchedule : travelPlanNormalizeResponse.days()) {

            List<TravelPlanNormalizeResponse.PlaceItem> places = daySchedule.places();
            List<String> contentIdsForDay = places.stream()
                    .map(TravelPlanNormalizeResponse.PlaceItem::content_id)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));

            for (TravelPlanEditGeminiResponse.ChangeItem changeItem : geminiResponse.changes()) {
                if (places.get(changeItem.sequence()-1).content_id().equals(changeItem.beforePlace().contentId())) {
                    contentIdsForDay.set(changeItem.sequence() - 1,
                            updatedPlaceMap.get(changeItem.afterPlace().contentId()).getContentId());

                }

            }

            List<Place> updatePlaces = new ArrayList<>();

            for (String contentId : contentIdsForDay) {
                Place place = placeService.findByContentId(contentId).get();
                updatePlaces.add(place);
            }

            editPlanRoutingRequest.getDays().add(EditPlanRoutingRequest.DailyRouteRequest.builder()
                    .date(daySchedule.date())
                    .places(updatePlaces)
                    .start_time(daySchedule.start_time())
                    .end_time(daySchedule.end_time())
                    .build());

        }


        EditPlanRoutingResponse editPlanRoutingResponse = editPlanRoutingCalculator.calculateCarRoute(editPlanRoutingRequest);

        List<TravelPlanNormalizeResponse.DaySchedule> newDaySchedules = new ArrayList<>();

        for (EditPlanRoutingRequest.DailyRouteRequest day : editPlanRoutingRequest.getDays()) {
            newDaySchedules.add(NormalizeStayTimeAllocator.allocateTravelPlanWeightedStayTimes(day, editPlanRoutingResponse.dailyRoutes().get(day.getDate())));

        }

        Map<LocalDate, List<TravelPlanNormalizeResponse.RouteItem>> routeDailyRoutes = new LinkedHashMap<>();

        editPlanRoutingResponse.dailyRoutes().forEach((date, edges) -> {
            List<TravelPlanNormalizeResponse.RouteItem> routeItems = edges.stream()
                    .map(edge -> new TravelPlanNormalizeResponse.RouteItem(
                            edge.sequence(),
                            edge.origin(),
                            edge.destination(),
                            edge.duration()
                    ))
                    .toList();
            routeDailyRoutes.put(date, routeItems);
        });

        List<TravelPlanNormalizeResponse.RouteInfo> newRoutes = List.of(
                new TravelPlanNormalizeResponse.RouteInfo(
                        editPlanRoutingResponse.routeType(),
                        routeDailyRoutes
                )
        );


        ;
        TravelPlanNormalizeResponse newTravelPlanNormalizeResponse = TravelPlanNormalizeResponse.of(
                travelPlanNormalizeResponse.label(),
                travelPlanNormalizeResponse.start_date(),
                travelPlanNormalizeResponse.end_date(),
                newDaySchedules,
                newRoutes,
                travelPlanNormalizeResponse.createdAt()
        );
        String newTravelPlanNormalizeResponseJson = null;
        try {
            newTravelPlanNormalizeResponseJson = objectMapper.writeValueAsString(newTravelPlanNormalizeResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log.info("ai 편집 결과: "+ newTravelPlanNormalizeResponseJson);
        redisTemplate.opsForValue().set(key,newTravelPlanNormalizeResponseJson);
        return newTravelPlanNormalizeResponse;


    }




}
