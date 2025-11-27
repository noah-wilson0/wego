package com.wego.wego.domain.plan.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.repository.TravelPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class TravelPlanSnapshotStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final TravelPlanRepository travelPlanRepository;

    public <T> T getNormalizeEditPlan(
            String redisKey,
            Long travelPlanId,
            Class<T> dtoType,
            Function<TravelPlan, T> initializer
    ) {
        return loadOrCreateByKey(redisKey, travelPlanId, dtoType, initializer);
    }

    public <T> T loadOrCreateByKey(
            String redisKey,
            Long travelPlanId,
            Class<T> dtoType,
            Function<TravelPlan, T> initializer
    ) {
        if (!redisTemplate.hasKey(redisKey)) {
            TravelPlan travelPlan = travelPlanRepository.findById(travelPlanId)
                    .orElseThrow(() -> new RuntimeException("존재 하지 않는 여행 일정: id=" + travelPlanId));

            try {
                T dto = initializer.apply(travelPlan);
                String json = objectMapper.writeValueAsString(dto);
                redisTemplate.opsForValue().set(redisKey, json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Redis 저장용 직렬화 실패", e);
            }
        }

        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            throw new IllegalStateException("Redis에 여행 일정 스냅샷이 없습니다: key=" + redisKey);
        }

        try {
            return objectMapper.readValue(json, dtoType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 스냅샷 파싱 실패: type=" + dtoType.getSimpleName(), e);
        }
    }

    public void saveByKey(String redisKey, Object updatedSnapshot) {
        try {
            String json = objectMapper.writeValueAsString(updatedSnapshot);
            redisTemplate.opsForValue().set(redisKey, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 저장 실패", e);
        }
    }
}

