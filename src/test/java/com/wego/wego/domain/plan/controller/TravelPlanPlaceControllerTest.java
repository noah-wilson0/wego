package com.wego.wego.domain.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.iam.v1.Policy;
import com.wego.wego.domain.plan.dto.TempTravelPlanAccommodationRequest;
import com.wego.wego.domain.plan.dto.TempTravelPlanPlaceRequest;
import com.wego.wego.domain.plan.dto.TempTravelTimeRequest;
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
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@Transactional
@AutoConfigureMockMvc(addFilters = false)
class TravelPlanPlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final String UUID = "Test-uuid-123";
    private static final List<String> PLACE_TYPES = List.of(
            "TOURIST_SPOT", "RESTAURANT", "CAFE", "ACCOMMODATION"
    );

    @DisplayName("장소 조회 테스트")
    @Test
    void getPagedPlaces() throws Exception {
        String placeType = PLACE_TYPES.get(2); // 예: "CAFE"

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/travel_plan/place/%s/paged".formatted(placeType))
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "averageRating,desc")
                        .param("sort", "likeCount,desc") // 다중 정렬 조건도 포함
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String contentAsString = result.getResponse().getContentAsString();
        log.info("📦 페이징된 장소 응답: {}", contentAsString);
    }

    @Test
    @DisplayName("장소 저장 테스트")
    void savePlaces() throws Exception {
        List<TempTravelPlanPlaceRequest> places = List.of(
                new TempTravelPlanPlaceRequest("129921"),
                new TempTravelPlanPlaceRequest("2918695"),
                new TempTravelPlanPlaceRequest("3307514")
//                new TempTravelPlanPlaceRequest("2894178"),
//                new TempTravelPlanPlaceRequest("2930998"), ///
//                new TempTravelPlanPlaceRequest("2924803"),
//                new TempTravelPlanPlaceRequest("264570"),
//                new TempTravelPlanPlaceRequest("2456536"),
//                new TempTravelPlanPlaceRequest("1603149"),
//                new TempTravelPlanPlaceRequest("1602451"), ////
//                new TempTravelPlanPlaceRequest("3076460"),
//                new TempTravelPlanPlaceRequest("2733970"), //왜 카카오 모빌리티에서 요청이 이거는 안되지?
//                new TempTravelPlanPlaceRequest("126486"),
//                new TempTravelPlanPlaceRequest("3074841"),
//                new TempTravelPlanPlaceRequest("2720441")
        );

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/travel_plan/place/temp/schedule/%s".formatted(UUID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(places)))
                .andExpect(status().isOk());
    }
    @Test
    @DisplayName("숙소 저장 테스트")
    void saveAccommodation() throws Exception {
        String times = redisTemplate.opsForValue().get(RedisKeyUtils.timeKey(UUID));
        TempTravelTimeRequest tempTravelTimeRequest = objectMapper.readValue(times, TempTravelTimeRequest.class);
        List<TempTravelPlanAccommodationRequest> accommodation = new ArrayList<>();

        for (int i = 0; i < tempTravelTimeRequest.travelDayTimes().size()-1; i++) {
            accommodation.add(new TempTravelPlanAccommodationRequest(tempTravelTimeRequest.travelDayTimes().get(i).date(),
                    "2574118"));
        }

        mockMvc.perform(MockMvcRequestBuilders
                        .post("/travel_plan/place/temp/schedule/%s/accommodation".formatted(UUID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(accommodation)))
                .andExpect(status().isOk());
    }

}