package com.wego.wego.domain.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.TempTravelPlanAccommodationRequest;
import com.wego.wego.domain.plan.dto.TempTravelPlanPlaceRequest;
import com.wego.wego.domain.plan.dto.TravelPlanPlaceResponse;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.repository.PlaceRepository;
import com.wego.wego.global.enums.PlaceType;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TravelPlanPlaceService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PlaceRepository placeRepository;

    public Page<TravelPlanPlaceResponse> findAll(String placeType, Pageable pageable) {
        return placeRepository.findByPlaceType(placeType, pageable)
                .map(place -> TravelPlanPlaceResponse.builder()
                        .contentId(place.getContentId())
                        .title(place.getTitle())
                        .image(place.getImage())
                        .placeType(place.getPlaceType())
                        .addr(place.getAddr1())
                        .averageRating(place.getAverageRating())
                        .likeCount(place.getLikeCount())
                        .build());
    }
    public void saveTempSchedulePlace(String uuid, List<TempTravelPlanPlaceRequest> tempSchedulePlaceRequests) {
        try {
            redisTemplate.opsForValue().set(RedisKeyUtils.placesKey(uuid), objectMapper.writeValueAsString(tempSchedulePlaceRequests));
        } catch (JsonProcessingException e) {
            log.info("임시 여행 장소 리스트 저장 실패");
            throw new RuntimeException(e);
        }
    }
    public void saveTempScheduleAccommodation(String uuid, List<TempTravelPlanAccommodationRequest> tempTravelPlanAccommodationRequests) {
        try {
            redisTemplate.opsForValue().set(RedisKeyUtils.accommodationsKey(uuid), objectMapper.writeValueAsString(tempTravelPlanAccommodationRequests));
        } catch (JsonProcessingException e) {
            log.info("임시 여행 숙소 리스트 저장 실패");
            throw new RuntimeException(e);
        }
    }

}
