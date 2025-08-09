package com.wego.wego.domain.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.TempTravelDateRequest;
import com.wego.wego.domain.plan.dto.TempTravelTimeRequest;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanDayService {
    private final RedisTemplate<String,String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveTempScheduleDate(String uuid, TempTravelDateRequest tempTravelDateRequest) {
        log.info("saveTempScheduleDate");
        try {
            redisTemplate.opsForValue().set(RedisKeyUtils.dateKey(uuid),objectMapper.writeValueAsString(tempTravelDateRequest),6, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.info("여행 일정 날짜 캐시 실패");
            throw new RuntimeException(e);
        }
    }
    public String getTempScheduleDate(String uuid) {
        String scheduleDate = redisTemplate.opsForValue().get(RedisKeyUtils.dateKey(uuid));

        if (scheduleDate == null) {
            log.info("여행 일정 날짜 읽기 실패");
        }
        return scheduleDate;
    }
    public void saveTempScheduleTime(String uuid, TempTravelTimeRequest tempTravelTimeRequest) {
        try {
            redisTemplate.opsForValue().set(RedisKeyUtils.timeKey(uuid),objectMapper.writeValueAsString(tempTravelTimeRequest),6, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.info("여행 일정 날짜별 시간 캐시 실패");
            throw new RuntimeException(e);
        }
    }
    public String getTempScheduleTime(String uuid) {
        String scheduleDate = redisTemplate.opsForValue().get(RedisKeyUtils.timeKey(uuid));

        if (scheduleDate == null) {
            log.info("여행 일정 날짜별 시간 읽기 실패");
        }
        return scheduleDate;
    }
}
