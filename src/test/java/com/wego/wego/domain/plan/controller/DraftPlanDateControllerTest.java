package com.wego.wego.domain.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.TempTravelDateRequest;
import com.wego.wego.domain.plan.dto.TempTravelTimeRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class DraftPlanDateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final String UUID = "Test-uuid-123";

    @Test
    void createTempScheduleDate() throws Exception {
        TempTravelDateRequest tempTravelDateRequest = new TempTravelDateRequest(
                LocalDate.of(2025, 8, 1),
                LocalDate.of(2025, 8, 3)
        );
        mockMvc.perform(post("/travel_plan/date/temp/schedule/"+UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tempTravelDateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void getTempScheduleDate() throws Exception {
        MvcResult result = mockMvc.perform(get("/travel_plan/date/temp/schedule/" + UUID))
                .andExpect(status().isOk())
                .andReturn();
        log.info(result.getResponse().getContentAsString());
    }


    @Test
    void createTempScheduleTimes() throws Exception {
        TempTravelTimeRequest tempTravelTimeRequest = new TempTravelTimeRequest(
                List.of(
                        new TempTravelTimeRequest.TravelDayTimes(LocalDate.of(2025, 8, 1), LocalTime.of(10,0),LocalTime.of(20,0)),
                        new TempTravelTimeRequest.TravelDayTimes(LocalDate.of(2025, 8, 2), LocalTime.of(10,0),LocalTime.of(20,0)),
                        new TempTravelTimeRequest.TravelDayTimes(LocalDate.of(2025, 8, 3), LocalTime.of(10,0),LocalTime.of(14,0))
                        )
        );
        mockMvc.perform(post("/travel_plan/date/temp/schedule/times/"+UUID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tempTravelTimeRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void getTempScheduleTimes() throws Exception {
        MvcResult result = mockMvc.perform(get("/travel_plan/date/temp/schedule/times/" + UUID))
                .andExpect(status().isOk())
                .andReturn();
        log.info(result.getResponse().getContentAsString());
    }

}