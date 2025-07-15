package com.wego.wego.domain.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.TravelDateRequest;
import com.wego.wego.domain.plan.dto.TravelTimeRequest;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanDateService {
    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveScheduleDate(String uuid,TravelDateRequest travelDateRequest) {
        try {
            redisTemplate.opsForValue().set(RedisKeyUtils.dateKey(uuid),objectMapper.writeValueAsString(travelDateRequest),6, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.info("여행 일정 날짜 캐시 실패");
            throw new RuntimeException(e);
        }
    }
    public String getScheduleDate(String uuid) {
        String scheduleDate = redisTemplate.opsForValue().get(RedisKeyUtils.dateKey(uuid));

        if (scheduleDate == null) {
            log.info("여행 일정 날짜 읽기 실패");
        }
        return scheduleDate;
    }
    public void saveScheduleTime(String uuid, TravelTimeRequest travelTimeRequest) {
        try {
            redisTemplate.opsForValue().set(RedisKeyUtils.timeKey(uuid),objectMapper.writeValueAsString(travelTimeRequest),6, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.info("여행 일정 날짜별 시간 캐시 실패");
            throw new RuntimeException(e);
        }
    }
    public String getScheduleTime(String uuid) {
        String scheduleDate = redisTemplate.opsForValue().get(RedisKeyUtils.timeKey(uuid));

        if (scheduleDate == null) {
            log.info("여행 일정 날짜별 시간 읽기 실패");
        }
        return scheduleDate;
    }
}
