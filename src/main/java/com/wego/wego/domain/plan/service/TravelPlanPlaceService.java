package com.wego.wego.domain.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.TempTravelPlanAccommodationRequest;
import com.wego.wego.domain.plan.dto.TempTravelPlanPlaceRequest;
import com.wego.wego.domain.plan.dto.TravelPlanPlaceResponse;
import com.wego.wego.domain.plan.repository.AreaSlugRepository;
import com.wego.wego.domain.plan.repository.CitySlugRepository;
import com.wego.wego.external.tourapi.location.repository.CityCodeRepository;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TravelPlanPlaceService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private final AreaSlugRepository areaSlugRepository;
    private final CitySlugRepository citySlugRepository;
    private final PlaceRepository placeRepository;
    private final CityCodeRepository cityCodeRepository;

    public Page<TravelPlanPlaceResponse> findAll(String areaSlug, String placeType, Pageable pageable) {

        List<Integer> cityIds = resolveCityIds(areaSlug);

        return placeRepository.findByPlaceTypeAndCityCodeIdIn(placeType, cityIds, pageable)
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
            redisTemplate.opsForValue().set(RedisKeyUtils.placesKey(uuid), objectMapper.writeValueAsString(tempSchedulePlaceRequests),6, TimeUnit.HOURS);
            log.info(redisTemplate.opsForValue().get(RedisKeyUtils.placesKey(uuid)));
        } catch (JsonProcessingException e) {
            log.info("임시 여행 장소 리스트 저장 실패");
            throw new RuntimeException(e);
        }
    }

    public void saveTempScheduleAccommodation(String uuid, List<TempTravelPlanAccommodationRequest> requests) {
        List<TempTravelPlanAccommodationRequest> filtered = requests.stream()
                .filter(req -> req.contentId() != null && !req.contentId().isBlank())
                .collect(Collectors.toList());
        String result=null;
        try {
            result = objectMapper.writeValueAsString(filtered);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // filtered 리스트만 Redis 저장
        redisTemplate.opsForValue().set(RedisKeyUtils.accommodationsKey(uuid), result,6, TimeUnit.HOURS);
    }

    private List<Integer> resolveCityIds(String areaSlug) {
        Optional<Integer> areaCodeIdBySlug = areaSlugRepository.findAreaCodeIdBySlug(areaSlug);

        if (areaCodeIdBySlug != null) {
            return cityCodeRepository.findCityCodeIdsByAreaCodeId(areaCodeIdBySlug.get());

        } else {
            return citySlugRepository.findCityCodeIdsBySlug(areaSlug);
        }
    }


}
