package com.wego.wego.domain.plan.service.edit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wego.wego.domain.plan.dto.edit.TravelPlanEditGeminiResponse;
import com.wego.wego.domain.plan.dto.edit.TravelPlanNormalizeResponse;
import com.wego.wego.external.tourapi.place.service.PlaceService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
class TravelPlanLangGraphServiceTest {

    @Autowired
    PlaceService placeService;


    @Test
    void getAiEditTravelPlan() throws JsonProcessingException {
        // 1) 네가 준 JSON 그대로 사용
        String json = "" +
                "{\n" +
                "  \"label\": \"서울\",\n" +
                "  \"start_date\": \"2025-11-28\",\n" +
                "  \"end_date\": \"2025-11-29\",\n" +
                "  \"days\": [\n" +
                "    {\n" +
                "      \"date\": \"2025-11-28\",\n" +
                "      \"start_time\": \"11:00\",\n" +
                "      \"end_time\": \"23:00\",\n" +
                "      \"places\": [\n" +
                "        { \"sequence\": 1, \"title\": \"경복궁\", \"content_id\": \"126508\" },\n" +
                "        { \"sequence\": 2, \"title\": \"토속촌삼계탕\", \"content_id\": \"132815\" },\n" +
                "        { \"sequence\": 3, \"title\": \"북촌한옥마을\", \"content_id\": \"126537\" },\n" +
                "        { \"sequence\": 4, \"title\": \"달 카페\", \"content_id\": \"1018855\" },\n" +
                "        { \"sequence\": 5, \"title\": \"봉피양 방이점\", \"content_id\": \"2774280\" },\n" +
                "        { \"sequence\": 6, \"title\": \"남산서울타워\", \"content_id\": \"126535\" },\n" +
                "        { \"sequence\": 7, \"title\": \"롯데호텔서울\", \"content_id\": \"142729\" }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"date\": \"2025-11-29\",\n" +
                "      \"start_time\": \"10:00\",\n" +
                "      \"end_time\": \"15:00\",\n" +
                "      \"places\": [\n" +
                "        { \"sequence\": 1, \"title\": \"창덕궁 낙선재\", \"content_id\": \"1604941\" },\n" +
                "        { \"sequence\": 2, \"title\": \"순희네빈대떡\", \"content_id\": \"1998192\" },\n" +
                "        { \"sequence\": 3, \"title\": \"국립민속박물관\", \"content_id\": \"2608977\" },\n" +
                "        { \"sequence\": 4, \"title\": \"동대문디자인플라자(DDP)\", \"content_id\": \"2470006\" }\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"routes\": [\n" +
                "    {\n" +
                "      \"route_type\": \"CAR\",\n" +
                "      \"dailyRoutes\": {\n" +
                "        \"2025-11-28\": [\n" +
                "          { \"sequence\": 1, \"origin\": \"126508\", \"destination\": \"132815\", \"duration\": 516 },\n" +
                "          { \"sequence\": 2, \"origin\": \"132815\", \"destination\": \"126537\", \"duration\": 662 },\n" +
                "          { \"sequence\": 3, \"origin\": \"126537\", \"destination\": \"1018855\", \"duration\": 401 },\n" +
                "          { \"sequence\": 4, \"origin\": \"1018855\", \"destination\": \"2774280\", \"duration\": 4647 },\n" +
                "          { \"sequence\": 5, \"origin\": \"2774280\", \"destination\": \"126535\", \"duration\": 2475 },\n" +
                "          { \"sequence\": 6, \"origin\": \"126535\", \"destination\": \"142729\", \"duration\": 955 }\n" +
                "        ],\n" +
                "        \"2025-11-29\": [\n" +
                "          { \"sequence\": 1, \"origin\": \"1604941\", \"destination\": \"1998192\", \"duration\": 700 },\n" +
                "          { \"sequence\": 2, \"origin\": \"1998192\", \"destination\": \"2608977\", \"duration\": 1118 },\n" +
                "          { \"sequence\": 3, \"origin\": \"2608977\", \"destination\": \"2470006\", \"duration\": 1356 }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"createdAt\": \"2025-10-15\"\n" +
                "}\n";

        // 2) ObjectMapper (LocalDate 파싱 + 알 수 없는 필드 무시)
        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 3) JSON → TravelPlanNormalizeResponse
        TravelPlanNormalizeResponse travelPlanNormalizeResponse = om.readValue(json, TravelPlanNormalizeResponse.class);

        // 4) AI 편집 응답(테스트용 1건): 2025-11-29의 sequence=4 교체
        var geminiResponse = new TravelPlanEditGeminiResponse(
                List.of(new TravelPlanEditGeminiResponse.ChangeItem(
                        LocalDate.parse("2025-11-29"),
                        4,
                        new TravelPlanEditGeminiResponse.PlacePatch("2470006", "동대문디자인플라자(DDP)"),
                        new TravelPlanEditGeminiResponse.PlacePatch("2902634", "청수당 베이커리")
                ))
        );

        var edit = geminiResponse;

// (1) afterPlace contentId들 모아서 일괄 정규화 (DB 조회)
        List<String> afterIds = edit.changes().stream()
                .map(c -> c.afterPlace().contentId())
                .distinct()
                .toList();

        log.info("[afterIds] {}", afterIds);

// 일괄 조회 → contentId → Place 매핑
        var afterPlaceMap = placeService.findAllByContentIdIn(afterIds).stream()
                .collect(Collectors.toMap(
                        p -> String.valueOf(p.getContentId()),
                        p -> p
                ));

        log.info("[afterPlaceMap.size] {}", afterPlaceMap.size());
        afterPlaceMap.forEach((k, v) -> log.info(" - {} => {} ({}, {}, {})",
                k, v.getTitle(), v.getLongitude(), v.getLatitude(), v.getPlaceType()));

//        Map<LocalDate, Map<Integer, TravelPlanEditGeminiResponse.PlacePatch>> replaceMap =
//
//                edit.changes().stream().collect(Collectors.groupingBy(
//                        changeItem -> {
//                            changeItem.date(),
//                        changeItem.}
//                ));

    }

    private boolean existPlace(String contentId) {
        return placeService.findByContentId(contentId).isPresent();
    }
}