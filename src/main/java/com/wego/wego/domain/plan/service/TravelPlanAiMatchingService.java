package com.wego.wego.domain.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.AreaCityTokens;
import com.wego.wego.domain.plan.dto.RetryFailedDay;
import com.wego.wego.domain.plan.dto.TravelPlanGeminiResponse;
import com.wego.wego.external.tourapi.location.entity.AreaCode;
import com.wego.wego.external.tourapi.location.entity.CityCode;
import com.wego.wego.external.tourapi.location.service.AreaCodeService;
import com.wego.wego.external.tourapi.location.service.CityCodeService;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.service.PlaceService;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanAiMatchingService {

    private final ObjectMapper objectMapper;

    private final AreaCodeService areaCodeService;
    private final CityCodeService cityCodeService;
    private final PlaceService placeService;
    private final GeminiRequestService geminiRequestService;

    private final RedisTemplate<String, String> redisTemplate;

    static final int MAX_RETRY = 5;


    public void autoTempTravelPlan(String uuid) {
        log.info("🚀 여행 일정 자동 생성 시작 - UUID: {}", uuid);

        String result = geminiRequestService.getGeminiTravelPlan(uuid);
        TravelPlanGeminiResponse travelPlanGeminiResponse;
        try {
            travelPlanGeminiResponse = objectMapper.readValue(result, TravelPlanGeminiResponse.class);
            log.info("✅ Gemini 추천 일정 파싱 완료");
        } catch (JsonProcessingException e) {
            throw new RuntimeException("❌ Gemini JSON 파싱 실패", e);
        }

        List<TravelPlanGeminiResponse.Days> updatedDays = new ArrayList<>();

        for (TravelPlanGeminiResponse.Days day : travelPlanGeminiResponse.days()) {
            log.info("📅 [{}] 일정 처리 시작", day.date());
            List<TravelPlanGeminiResponse.Days.Places> updatedPlaces = new ArrayList<>();

            for (TravelPlanGeminiResponse.Days.Places place : day.places()) {
                TravelPlanGeminiResponse.Days.Places currentPlace = place;

                log.info("🔍 AI 장소 후보: {}", currentPlace.title());
                Optional<Place> mostSimilarTitleInCity = Optional.empty();
                int retryCount = 0;
                boolean matched = false;

                while (!matched && retryCount <= MAX_RETRY) {
                    AreaCityTokens areaCityTokens = extractAreaAndCityFromAddr(currentPlace.addr());
                    if (areaCityTokens == null) {
                        log.warn("⚠️ 주소 파싱 실패 - 입력 주소: {}", currentPlace.addr());
                        break;
                    }

                    AreaCode areaCode = areaCodeService.findByNameContainedIn(areaCityTokens.areaName()).orElse(null);
                    if (areaCode == null) {
                        log.warn("❌ AreaCode 매칭 실패 - areaName: {}", areaCityTokens.areaName());
                        break;
                    }

                    CityCode cityCode = cityCodeService.findByAreaCodeAndName(areaCode, areaCityTokens.cityName()).orElse(null);
                    if (cityCode == null) {
                        log.warn("❌ CityCode 매칭 실패 - cityName: {}", areaCityTokens.cityName());
                        break;
                    }

                    mostSimilarTitleInCity = placeService.findMostSimilarTitleInCity(currentPlace.title(), cityCode.getCityCodeId());

                    if (mostSimilarTitleInCity.isPresent()) {
                        matched = true;
                        log.info("🟢 DB 장소 매칭 성공 - AI: '{}' → DB: '{}'", currentPlace.title(), mostSimilarTitleInCity.get().getTitle());
                    } else {
                        log.warn("🔁 매칭 실패 ({}회 시도) - Gemini 재요청", retryCount);

                        RetryFailedDay retry = new RetryFailedDay(
                                day.date(),
                                day.start_time(),
                                day.end_time(),
                                new RetryFailedDay.FailedPlace(
                                        currentPlace.title(),
                                        currentPlace.addr(),
                                        currentPlace.tel()
                                )
                        );

                        try {
                            String newRecommendationJson = geminiRequestService.retryGeminiTravelPlan(
                                    objectMapper.writeValueAsString(travelPlanGeminiResponse),
                                    retry
                            );

                            List<TravelPlanGeminiResponse.Days.Places> retryPlaces =
                                    objectMapper.readValue(newRecommendationJson, new TypeReference<>() {});
                            if (!retryPlaces.isEmpty()) {
                                currentPlace = retryPlaces.get(0);
                                log.info("🔁 Gemini 재추천 장소: {}", currentPlace.title());
                            } else {
                                log.warn("⚠️ Gemini 응답 비어있음. 루프 중단");
                                break;
                            }
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException("❌ Gemini 재추천 JSON 파싱 오류", e);
                        }

                        retryCount++;
                    }
                }

                if (mostSimilarTitleInCity.isPresent()) {
                    Place matchedPlace = mostSimilarTitleInCity.get();
                    log.info("✅ 최종 매칭 결과 → AI: '{}' / DB: '{}'", currentPlace.title(), matchedPlace.getTitle());
                    log.info("   ⤷ 주소: {} {}", matchedPlace.getAddr1(), matchedPlace.getAddr2());
                } else {
                    log.warn("❌ 최종 매칭 실패 - AI 장소명: '{}'", currentPlace.title());
                }

                updatedPlaces.add(currentPlace); // 최종 currentPlace 저장
                log.info("----------------------------------------------------");
            }

            TravelPlanGeminiResponse.Days updatedDay = new TravelPlanGeminiResponse.Days(
                    day.date(),
                    day.start_time(),
                    day.end_time(),
                    updatedPlaces,
                    day.accommodations()
            );
            updatedDays.add(updatedDay);
        }

        TravelPlanGeminiResponse updatedResponse = new TravelPlanGeminiResponse(
                travelPlanGeminiResponse.start_date(),
                travelPlanGeminiResponse.end_date(),
                updatedDays
        );

        try {
            String updatedJson = objectMapper.writeValueAsString(updatedResponse);
            redisTemplate.opsForValue().set(RedisKeyUtils.recommendKey(uuid), updatedJson,6, TimeUnit.HOURS);
            log.info("💾 Redis 저장 완료 - key: {}", RedisKeyUtils.recommendKey(uuid));
            log.debug("📦 저장된 데이터: {}", updatedJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("❌ 최종 JSON 직렬화 실패", e);
        }

        log.info("🎉 여행 자동 생성 완료 - UUID: {}", uuid);
    }



    public AreaCityTokens extractAreaAndCityFromAddr(String addr) {
        if (addr == null || addr.isBlank()) return null;

        String[] tokens = addr.trim().split("\\s+");
        if (tokens.length < 1) return null;

        String areaName = tokens[0];
        String cityName;

        // 세종시는 시군구가 따로 없으므로 첫 번째를 그대로 사용
        if (areaName.contains("세종")) {
            log.info("세종시 추출 → area: {}, city: {}", areaName, areaName);
            return new AreaCityTokens(areaName, areaName);
        }

        if (tokens.length < 2) {
            log.warn("주소가 너무 짧아 시군구 추출 불가: {}", addr);
            return new AreaCityTokens(areaName, null);  // area는 반환, city는 없음
        }

        cityName = tokens[1];
        log.info("주소 추출 → area: {}, city: {}", areaName, cityName);

        return new AreaCityTokens(areaName, cityName);
    }
}
