package com.wego.wego.domain.plan.controller;

import com.google.iam.v1.Policy;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@Transactional
@AutoConfigureMockMvc(addFilters = false)
class TravelPlanPlaceControllerTest {

    @Autowired
    private TravelPlanPlaceController travelPlanPlaceController;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final List<String> PLACE_TYPES = List.of(
            "TOURIST_SPOT", "RESTAURANT", "CAFE", "ACCOMMODATION"
    );

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


}