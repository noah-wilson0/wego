package com.wego.wego.domain.plan.service.edit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.draft.route.DraftPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.draft.route.RoutingSummary;
import com.wego.wego.domain.plan.dto.edit.EditLanggraphRequest;
import com.wego.wego.domain.plan.dto.edit.TravelPlanEditGeminiResponse;
import com.wego.wego.domain.plan.dto.edit.TravelPlanNormalizeResponse;
import com.wego.wego.domain.plan.service.support.SlugResolver;
import com.wego.wego.domain.plan.util.NormalizeStayTimeAllocator;
import com.wego.wego.external.route.dto.RouteResult;
import com.wego.wego.external.route.kakao.sevice.KaKaoMobilityFetchService;
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
import java.util.*;
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
    private final KaKaoMobilityFetchService kaKaoMobilityFetchService;

    private final SlugResolver slugResolver;

    public TravelPlanLangGraphService(RedisTemplate<String, String> redisTemplate,
                                      ObjectMapper objectMapper,
                                      @Qualifier("AutoWebClient") WebClient autoWebClient,
                                      PlaceService placeService, KaKaoMobilityFetchService kaKaoMobilityFetchService,
                                      SlugResolver slugResolver) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.autoWebClient = autoWebClient;
        this.placeService = placeService;
        this.kaKaoMobilityFetchService = kaKaoMobilityFetchService;
        this.slugResolver = slugResolver;
    }


    public TravelPlanNormalizeResponse getAiEditTravelPlan(Long travelPlanId, String prompt) {
        String key = RedisKeyUtils.editTravelPlanKey(String.valueOf(travelPlanId));
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

        Map<LocalDate, List<TravelPlanEditGeminiResponse.ChangeItem>> changesByDate =
                geminiResponse.changes().stream()
                        .collect(Collectors.groupingBy(
                                TravelPlanEditGeminiResponse.ChangeItem::date,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        List<TravelPlanNormalizeResponse.DaySchedule> newDaySchedules = new ArrayList<>();
        List<TravelPlanNormalizeResponse.RouteInfo> newRoutes = new ArrayList<>();
        TravelPlanNormalizeResponse.RouteInfo newRouteInfo = new TravelPlanNormalizeResponse.RouteInfo(travelPlanNormalizeResponse.routes().getFirst().route_type(),
                new LinkedHashMap<>());
        newRoutes.add(newRouteInfo);

        for (int i = 0; i < travelPlanNormalizeResponse.days().size(); i++) {
            TravelPlanNormalizeResponse.DaySchedule daySchedule = travelPlanNormalizeResponse.days().get(i);

            List<TravelPlanEditGeminiResponse.ChangeItem> dayChanges =
                    changesByDate.getOrDefault(daySchedule.date(), Collections.emptyList());
            if (dayChanges.isEmpty()) {
                // 변경이 없는 날짜면 원본 유지
                newDaySchedules.add(daySchedule);
                TravelPlanNormalizeResponse.RouteInfo routeInfo = travelPlanNormalizeResponse.routes().get(i);
                newRouteInfo.dailyRoutes().put(daySchedule.date(),routeInfo.dailyRoutes().get(daySchedule.date()));
                continue;
            }
            // normalizeresponse.dayschedule.places 업데이트
            for (int j = 0; j < geminiResponse.changes().size(); j++) {
                TravelPlanEditGeminiResponse.ChangeItem changeItem = geminiResponse.changes().get(j);


                //응답 데이터 정규화
                Place findBeforePlace = updatedPlaceMap.get(changeItem.beforePlace().contentId());
                Place findAfterPlace = updatedPlaceMap.get(changeItem.afterPlace().contentId());

                List<TravelPlanNormalizeResponse.RouteItem> routeItems = travelPlanNormalizeResponse.routes().getFirst()
                        .dailyRoutes().get(daySchedule.date()).stream().sorted(Comparator.comparing(TravelPlanNormalizeResponse.RouteItem::sequence)).toList();


                List<TravelPlanNormalizeResponse.RouteItem> newRouteItems = routeItems.stream()
                        .map(routeItem -> {
                            if (routeItem.origin().equals(findAfterPlace.getContentId())) {
                                RouteResult routeResult = kaKaoMobilityFetchService.fetchKaKaoMobilityData(findAfterPlace, placeService.findByContentId(routeItem.destination()).get());
                                return new TravelPlanNormalizeResponse.RouteItem(routeItem.sequence(), routeResult.getOriginId(), routeResult.getDestinationId(), routeResult.getDuration());
                            }
                            else if (routeItem.destination().equals(findBeforePlace.getContentId())) {
                                RouteResult routeResult = kaKaoMobilityFetchService.fetchKaKaoMobilityData(placeService.findByContentId(routeItem.origin()).get(), findAfterPlace);
                                return new TravelPlanNormalizeResponse.RouteItem(routeItem.sequence(), routeResult.getOriginId(), routeResult.getDestinationId(), routeResult.getDuration());
                            }
                            return routeItem;
                        }).collect(Collectors.toList());

                List<TravelPlanNormalizeResponse.PlaceItem> newPlaces = daySchedule.places().stream()
                        .map(placeItem -> {
                            if (placeItem.content_id().equals(findBeforePlace.getContentId())) {
                                return new TravelPlanNormalizeResponse.PlaceItem(
                                        findAfterPlace.getContentId(),
                                        findAfterPlace.getPlaceType(),
                                        findAfterPlace.getTitle(),
                                        findAfterPlace.getImage(),
                                        changeItem.sequence(),
                                        Double.parseDouble(findAfterPlace.getLongitude()),
                                        Double.parseDouble(findAfterPlace.getLatitude()),
                                        placeItem.start_time(),
                                        placeItem.end_time()
                                );
                            }
                            return placeItem;
                        }).toList();



// 2) DaySpec 구성 (Normalize → RoutingDaySpec 어댑트)
                DraftPlanRoutingRequest.RoutingDaySpec daySpec = DraftPlanRoutingRequest.RoutingDaySpec.builder()
                        .date(daySchedule.date())
                        .start_time(daySchedule.start_time())
                        .end_time(daySchedule.end_time())
                        .places(
                                newPlaces.stream()
                                        .map(p -> {
                                            Place full = updatedPlaceMap.get(p.content_id());
                                            if (full != null) return full;
                                            return placeService.findByContentId(p.content_id())
                                                    .orElseThrow(() -> new IllegalStateException("Place not found: " + p.content_id()));
                                        })
                                        .collect(Collectors.toList())
                        )
                        .accommodation(null) // 편집 Normalize에서는 숙소 분리
                        .build();

// 3) legs 구성 (newRouteItems → RoutingSummary.RouteLeg)
                List<RoutingSummary.RouteLeg> legs = newRouteItems.stream()
                        .sorted(Comparator.comparingInt(TravelPlanNormalizeResponse.RouteItem::sequence))
                        .map(r -> RoutingSummary.RouteLeg.builder()
                                .sequence(r.sequence())
                                .origin(r.origin())
                                .destination(r.destination())
                                .duration((int) r.duration())   // 초 단위
                                .build())
                        .collect(Collectors.toList());

// 4) 체류시간 재계산
                TravelPlanNormalizeResponse.DaySchedule stayAdjustedDay =
                        NormalizeStayTimeAllocator.allocateTravelPlanWeightedStayTimes(daySpec, legs);

// 5) newRoutes / newDaySchedules에 결과 반영
                newRouteInfo.dailyRoutes().put(daySchedule.date(), newRouteItems);


                newDaySchedules.add(
                        new TravelPlanNormalizeResponse.DaySchedule(
                                stayAdjustedDay.date(),
                                stayAdjustedDay.start_time(),
                                stayAdjustedDay.end_time(),
                                stayAdjustedDay.places()
                        )
                );


            }


        }

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
