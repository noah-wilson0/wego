package com.wego.wego.domain.place.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.place.dto.LabelSearchCondition;
import com.wego.wego.domain.place.dto.SearchCondition;
import com.wego.wego.domain.plan.dto.DraftPlanPlaceResponse;
import com.wego.wego.domain.plan.support.SlugResolver;
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

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlaceQueryService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final PlaceRepository placeRepository;
    private final SlugResolver slugResolver;

    public Page<DraftPlanPlaceResponse> search(SearchCondition condition, Pageable pageable) {

        String slugJson = redisTemplate.opsForValue().get(RedisKeyUtils.slugKey(condition.uuid()
        ));
        log.info(slugJson);
        String slug;
        try {
            JsonNode node = objectMapper.readTree(slugJson);
            slug = node.path("slug").asText(null); // 없으면 null
        } catch (JsonProcessingException e) {
            log.warn("Invalid slug JSON: {}", slugJson, e);
            return Page.empty(pageable);
        }
        log.info(slug);
        List<Integer> cityIds = slugResolver.resolveCityIds(slug);
        log.info(cityIds.toString());
        Page<DraftPlanPlaceResponse> draftPlanPlaceResponses = placeRepository.searchByTitleInCities(condition.placeTypes(), cityIds.stream().mapToLong(Integer::longValue).boxed().toList(), condition.keyword(), pageable);
        log.info(draftPlanPlaceResponses.toString());
        return draftPlanPlaceResponses;
    }
    public Page<DraftPlanPlaceResponse> searchByLabel(LabelSearchCondition condition, Pageable pageable) {

        String slug= slugResolver.resolveSlugByLabel(condition.label());

        List<Integer> cityIds = slugResolver.resolveCityIds(slug);
        log.info(cityIds.toString());
        Page<DraftPlanPlaceResponse> draftPlanPlaceResponses = placeRepository.searchByTitleInCities(condition.placeTypes(), cityIds.stream().mapToLong(Integer::longValue).boxed().toList(), condition.keyword(), pageable);
        log.info(draftPlanPlaceResponses.toString());
        return draftPlanPlaceResponses;
    }


}
