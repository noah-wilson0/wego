package com.wego.wego.domain.plan.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.AreaCityTokens;
import com.wego.wego.domain.plan.dto.RetryFailedDay;
import com.wego.wego.domain.plan.dto.TempTravelPlanGeminiResponse;
import com.wego.wego.domain.plan.service.GeminiRequestService;
import com.wego.wego.external.tourapi.location.entity.AreaCode;
import com.wego.wego.external.tourapi.location.entity.CityCode;
import com.wego.wego.external.tourapi.location.service.AreaCodeService;
import com.wego.wego.external.tourapi.location.service.CityCodeService;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.repository.PlaceRepository;
import com.wego.wego.external.tourapi.place.service.PlaceService;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class TravelPlanAutoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlaceService placeService;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private AreaCodeService areaCodeService;

    @Autowired
    private CityCodeService cityCodeService;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;


    private final String UUID = "Test-uuid-123";

    String result;
    TempTravelPlanGeminiResponse tempTravelPlanGeminiResponse;
    @Autowired
    private GeminiRequestService geminiRequestService;

    @Test
    void autoTempTravelPlan() throws Exception {
        mockMvc.perform(post("/travel_plan/recommend/temp/sechedule/auto/" + UUID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void getTravelAutoPlan() {
        String s = redisTemplate.opsForValue().get(RedisKeyUtils.recommendKey(UUID));
        log.info(s);
    }

//    @BeforeEach
    void setUp() throws Exception {
        log.info("before");
        result = mockMvc.perform(post("/travel_plan/recommend/temp/sechedule/auto/" + UUID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        log.info("ai요청 결과:{{}",result);

        tempTravelPlanGeminiResponse = objectMapper.readValue(result, TempTravelPlanGeminiResponse.class);
    }

    /**
     * ✅ 테스트 목적:
     * - AI가 추천한 여행 일정(TravelPlanGeminiResponse) 내 장소들을 DB(place 테이블)와 매칭하기 위함
     * - 매칭 로직:
     *   1. 제목(title) 기반 LIKE 검색
     *   2. 전체 주소(addr1 + addr2) 기반 완전 일치 검색
     *   3. 유사도 기반 similarity(title, AI_title)로 fallback 검색

     * ✅ 주요 테스트 포인트:
     * - AI 응답의 각 장소 title과 addr이 실제 DB에 존재하는지 확인
     * - 존재하지 않을 경우 similarity를 통해 가장 유사한 title 검색

     * ✅ 테스트 결과 (2025-07-22 기준):
     * - title 기반 LIKE 검색은 대체로 성공했으나 일부 불일치 존재(ai-> N남산 타워, DB -> 한쿡 N서울타워)
     * - addr1 + addr2 기반 일치 비교는 공백/NULL/trim 문제로 인해 실패 빈도 있음
     * - similarity 기반 검색은 일부 타지역(예: '강릉 남산공원' vs '남산공원(서울)')으로 잘못 매칭되는 오탐 발생
     *   → 따라서 지역 필터(city_code_id)와 함께 similarity 검색을 추가 도입 필요
     */
    @DisplayName("Ai 추천 일정 및 DB 데이터 매칭 여부 테스트 1")
//    @Test
    void autoTempTravelPlanV1() throws Exception {

        tempTravelPlanGeminiResponse.days().stream().forEach(
                day -> {
                    day.places().stream().forEach(
                            place -> {
                                List<Place> byTitle = placeService.findByTitleContaining(place.title());
                                List<Place> byAddress = placeService.findByFullAddress(place.addr());

                                List<String> titles = new ArrayList<>();
                                byTitle.stream().forEach(
                                        title -> {
                                            titles.add(String.valueOf(title.getTitle()));
                                        }
                                );
                                log.info("🔍 AI 장소: {}", place.title());
                                log.info(" - DB 제목 매칭 여부: {}", !byTitle.isEmpty());
                                log.info(" - DB 제목 여부: {}", titles);
                                log.info(" - ai response 주소: {}",place.addr());

                                List<String> addrs = new ArrayList<>();
                                byTitle.stream().forEach(
                                        addr -> {
                                            addrs.add(String.valueOf(addr.getAddr1()+addr.getAddr2()));
                                        }
                                );

                                log.info(" - DB 주소 여부: {}", addrs);

                                List<Object[]> mostSimilarTitleWithScore = placeRepository.findMostSimilarTitleWithScore(place.title());
                                if (!mostSimilarTitleWithScore.isEmpty()) {
                                    String matchedTitle = (String) mostSimilarTitleWithScore.get(0)[0];
                                    Float similarity = (Float) mostSimilarTitleWithScore.get(0)[1];
                                    log.warn("⚠️ 유사도 기반 매칭: AI [{}] → DB [{}], 유사도: {}", place.title(), matchedTitle, similarity);
                                } else {
                                    log.warn("❌ 유사도 기반 매칭 실패: {}", place.title());
                                }

                                if (byTitle.isEmpty() && byAddress.isEmpty()) {
                                    log.warn("❗ DB에 매칭되는 장소가 없습니다: {}", place.title());
                                }
                            }
                    );
                }
        );


    }

    /**
     * 일단 이코드로 구현하고 나중에 프로토 모델 만들고 해결하기
     *  종묘 와 db매칭하면 종묘 [유네스코 세계유산]이 나와야 하는데 종묘 묘현례가 나옴
     */
//    @Test
    @DisplayName("지역 필터(city_code_id)와 함께 similarity 검색 - Gemini 재시도 포함")
    void autoTempTravelPlanV3() {
        log.info("시작");

        final int MAX_RETRY = 5;

        tempTravelPlanGeminiResponse.days().forEach(day -> {
            day.places().forEach(place -> {
                Optional<Place> mostSimilarTitleInCity = Optional.empty();
                TempTravelPlanGeminiResponse.Days.Places currentPlace = place;

                int retryCount = 0;
                boolean matched = false;
                while (!matched && retryCount <= MAX_RETRY) {
                    log.info("매칭 전 ai 여행 장소명: {}", currentPlace.title());
                    AreaCityTokens areaCityTokens = extractAreaAndCityFromAddr(currentPlace.addr());
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
                        log.info("🟢 매칭 성공 ({}회 시도)", retryCount);
                    } else {
                        log.warn("🔁 매칭 실패 ({}회 시도) - Gemini 재요청 시도", retryCount);
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
                                    objectMapper.writeValueAsString(tempTravelPlanGeminiResponse),
                                    retry
                            );

                            List<TempTravelPlanGeminiResponse.Days.Places> retryPlaces =
                                    objectMapper.readValue(newRecommendationJson, new TypeReference<>() {});
                            if (!retryPlaces.isEmpty()) {
                                currentPlace = retryPlaces.get(0);
                            } else {
                                log.warn("⚠️ Gemini 응답이 비어있습니다. 루프 중단");
                                break;
                            }
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException("JSON 파싱 오류", e);
                        }

                        retryCount++;
                    }
                }

                log.info("📌 최종 결과:");
                log.info(" - AI 장소명: {}", currentPlace.title());
                log.info(" - AI 주소명: {}", currentPlace.addr());

                if (mostSimilarTitleInCity.isPresent()) {
                    Place matchedPlace = mostSimilarTitleInCity.get();
                    log.info(" - DB 장소명: {}", matchedPlace.getTitle());
                    log.info(" - DB 주소명: {}{}", matchedPlace.getAddr1(), matchedPlace.getAddr2());
                } else {
                    log.warn("❌ 최종적으로도 매칭 실패 ({}회 시도)", retryCount);
                }
                log.info("=================================================");
            });
        });
    }



    @Test
    @DisplayName("지역 필터(city_code_id)와 함께 similarity 검색 - Gemini 재시도 포함")
    void autoTempTravelPlanV2() {
        log.info("시작");

        final int MAX_RETRY = 5;

        tempTravelPlanGeminiResponse.days().forEach(day -> {
            day.places().forEach(place -> {
                Optional<Place> mostSimilarTitleInCity = Optional.empty();
                TempTravelPlanGeminiResponse.Days.Places currentPlace = place;

                int retryCount = 0;
                boolean matched = false;

                while (!matched && retryCount <= MAX_RETRY) {
                    AreaCityTokens areaCityTokens = extractAreaAndCityFromAddr(currentPlace.addr());
                    AreaCode areaCode = areaCodeService.findByNameContainedIn(areaCityTokens.areaName()).orElse(null);
                    if (areaCode == null) break;

                    CityCode cityCode = cityCodeService.findByAreaCodeAndName(areaCode, areaCityTokens.cityName()).orElse(null);
                    if (cityCode == null) break;

                    mostSimilarTitleInCity = placeService.findMostSimilarTitleInCity(currentPlace.title(), cityCode.getCityCodeId());

                    if (mostSimilarTitleInCity.isPresent()) {
                        matched = true;
                        log.info("🟢 매칭 성공 ({}회 시도) - AI: {} / DB: {}", retryCount, currentPlace.title(), mostSimilarTitleInCity.get().getTitle());
                    } else {
                        log.warn("🔁 매칭 실패 ({}회 시도) - Gemini 재요청 시도", retryCount);
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
                                    objectMapper.writeValueAsString(tempTravelPlanGeminiResponse),
                                    retry
                            );

                            List<TempTravelPlanGeminiResponse.Days.Places> retryPlaces =
                                    objectMapper.readValue(newRecommendationJson, new TypeReference<>() {});
                            if (!retryPlaces.isEmpty()) {
                                currentPlace = retryPlaces.get(0);
                            } else {
                                log.warn("⚠️ Gemini 응답이 비어있습니다. 루프 중단");
                                break;
                            }
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException("JSON 파싱 오류", e);
                        }

                        retryCount++;
                    }
                }

                log.info("📌 최종 결과:");
                log.info(" - AI 장소명: {}", currentPlace.title());
                log.info(" - AI 주소명: {}", currentPlace.addr());
                if (mostSimilarTitleInCity.isPresent()) {
                    log.info(" - DB 장소명: {}", mostSimilarTitleInCity.get().getTitle());
                    log.info(" - DB 주소명: {}{}", mostSimilarTitleInCity.get().getAddr1(), mostSimilarTitleInCity.get().getAddr2());
                } else {
                    log.warn("❌ 최종적으로도 매칭 실패 ({}회 시도)", retryCount);
                }
            });
        });
    }


    @Test
    void getRecommendSchedule() {

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