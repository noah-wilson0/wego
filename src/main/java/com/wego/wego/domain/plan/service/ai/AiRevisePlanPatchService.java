package com.wego.wego.domain.plan.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.ai.RevisePlanNormalize;
import com.wego.wego.domain.plan.dto.edit.TravelPlanEditGeminiResponse;
import com.wego.wego.domain.plan.dto.edit.TravelPlanNormalizeResponse;
import com.wego.wego.domain.plan.support.TravelPlanSnapshotStore;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.service.PlaceService;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiRevisePlanPatchService {
    private final ObjectMapper objectMapper;

    private final PlaceService placeService;
    private final TravelPlanSnapshotStore travelPlanSnapshotStore;


    public String revisePlanChangePlacePatch(Long memberId, String travelPlanId, String date, TravelPlanEditGeminiResponse geminiResponse) {
        String redisKey = RedisKeyUtils.reviseTravelPlanKey(String.valueOf(memberId), travelPlanId);

        RevisePlanNormalize revisePlanNormalize =
                travelPlanSnapshotStore.getNormalizeEditPlan(redisKey,
                        Long.valueOf(travelPlanId),
                        RevisePlanNormalize.class,
                        plan -> {
                            TravelPlanNormalizeResponse normalize = new TravelPlanNormalizeResponse(plan);
                            return RevisePlanNormalize.from(normalize);
                        }
                );


        List<String> allIds = geminiResponse.changes().stream()
                .flatMap(c -> Stream.of(
                        c.beforePlace().contentId(),
                        c.afterPlace().contentId()
                ))
                .collect(Collectors.toList());


        Map<String, Place> updatedPlaceMap = placeService.findAllByContentIdIn(allIds).stream()
                .collect(Collectors.toMap(Place::getContentId, p -> p));


        for (RevisePlanNormalize.DaySchedule daySchedule : revisePlanNormalize.getDays()) {

            List<RevisePlanNormalize.PlaceItem> places = daySchedule.getPlaces();

            for (TravelPlanEditGeminiResponse.ChangeItem changeItem : geminiResponse.changes()) {
                int changeIdx = changeItem.sequence() - 1;
                if (places.get(changeIdx).getContentId()
                        .equals(changeItem.beforePlace().contentId())) {
                    RevisePlanNormalize.PlaceItem target = places.get(changeIdx);
                    Place newPlace = updatedPlaceMap.get(changeItem.afterPlace().contentId());

                    target.setContentId(newPlace.getContentId());
                    target.setTitle(newPlace.getTitle());
                    target.setPlaceType(newPlace.getPlaceType());
                    target.setImage(newPlace.getImage());
                    target.setLongitude(Double.parseDouble(newPlace.getLongitude()));
                    target.setLatitude(Double.parseDouble(newPlace.getLatitude()));
                }
            }
        }
        travelPlanSnapshotStore.saveByKey(redisKey, revisePlanNormalize);

        log.info("AI CHANGE_PLACE 패치 적용 완료 - memberId={}, travelPlanId={}", memberId, travelPlanId);
        String response = null;
        try {
            response = objectMapper.writeValueAsString(revisePlanNormalize);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return response;
    }
}
