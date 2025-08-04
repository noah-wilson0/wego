package com.wego.wego.domain.plan.service;

import com.wego.wego.global.util.RedisKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TravelPlanRouteServiceTest {

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    String uuid = "88f5f299-4f69-4fe7-ab41-2a72edbce418";

    @Test
    void getScheduleRoute() {
        log.info("장소 json: {}", redisTemplate.opsForValue().get(RedisKeyUtils.placesKey(uuid)));
        log.info("숙소 json: {}", redisTemplate.opsForValue().get(RedisKeyUtils.accommodationsKey(uuid)));
        log.info("날짜 json: {}", redisTemplate.opsForValue().get(RedisKeyUtils.dateKey(uuid)));

    }
}