package com.wego.wego.domain.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@Transactional
@AutoConfigureMockMvc(addFilters = false)
class TravelPlanRouteControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final String UUID = "Test-uuid-123";

    @DisplayName("차 여행 경로 생성")
    @Test
    void createTempScheduleRouteCar() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/travel_plan/route/temp/schedule/%s/%s".formatted("car", UUID))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
    @DisplayName("대중교통 여행 경로 생성")
    @Test
    void createTempScheduleRouteTransit() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/travel_plan/route/temp/schedule/%s/%s".formatted("transit", UUID))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


}

