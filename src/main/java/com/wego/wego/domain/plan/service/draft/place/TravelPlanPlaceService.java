package com.wego.wego.domain.plan.service.draft.place;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.TempTravelPlanAccommodationRequest;
import com.wego.wego.domain.plan.dto.TempTravelPlanPlaceRequest;
import com.wego.wego.domain.plan.dto.DraftPlanPlaceResponse;
import com.wego.wego.domain.plan.repository.AreaSlugRepository;
import com.wego.wego.domain.plan.repository.CitySlugRepository;
import com.wego.wego.domain.plan.support.SlugResolver;
import com.wego.wego.external.tourapi.location.repository.CityCodeRepository;
import com.wego.wego.external.tourapi.place.repository.PlaceRepository;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final SlugResolver slugResolver;

    public Page<DraftPlanPlaceResponse> findAll(String uuid, String placeType, Pageable pageable) {
        String slug;
        String slugJson = redisTemplate.opsForValue().get(RedisKeyUtils.slugKey(uuid));
        log.info(slugJson);
        try {
            slug = objectMapper.readTree(slugJson).get("slug").asText();

        } catch (JsonProcessingException e) {
            log.info("여행 경로 데이터 통합 중 객체화 실패");
            throw new RuntimeException(e);
        }
        log.info(slug);
        List<Integer> cityIds = slugResolver.resolveCityIds(slug);
        log.info(cityIds.toString());
        return placeRepository.findByPlaceTypeAndCityCodeIdIn(placeType, cityIds, pageable)
                .map(place -> DraftPlanPlaceResponse.builder()
                        .contentId(place.getContentId())
                        .title(place.getTitle())
                        .image(place.getImage())
                        .placeType(place.getPlaceType())
                        .addr(place.getAddr1())
                        .longitude(Double.parseDouble(place.getLongitude()))
                        .latitude(Double.parseDouble(place.getLatitude()))
                        .averageRating(place.getAverageRating())
                        .likeCount(place.getLikeCount())
                        .build());
    }
    public Page<DraftPlanPlaceResponse> findAllByLabel(String label, String placeType, Pageable pageable) {

        String slug = slugResolver.resolveSlugByLabel(label);
        List<Integer> cityIds = slugResolver.resolveCityIds(slug);
        log.info(cityIds.toString());
        return placeRepository.findByPlaceTypeAndCityCodeIdIn(placeType, cityIds, pageable)
                .map(place -> DraftPlanPlaceResponse.builder()
                        .contentId(place.getContentId())
                        .title(place.getTitle())
                        .image(place.getImage())
                        .placeType(place.getPlaceType())
                        .addr(place.getAddr1())
                        .longitude(Double.parseDouble(place.getLongitude()))
                        .latitude(Double.parseDouble(place.getLatitude()))
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

//    private List<Integer> resolveCityIds(String areaSlug) {
//        Optional<Integer> areaCodeIdBySlug = areaSlugRepository.findAreaCodeIdBySlug(areaSlug);
//
//        if (areaCodeIdBySlug.isPresent()) {
//            return cityCodeRepository.findCityCodeIdsByAreaCodeId(areaCodeIdBySlug.get());
//
//        } else {
//            return citySlugRepository.findCityCodeIdsBySlug(areaSlug);
//        }
//    }


}
