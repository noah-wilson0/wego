package com.wego.wego.domain.place.repository;

import com.wego.wego.domain.plan.dto.DraftPlanPlaceResponse;
import com.wego.wego.domain.plan.dto.draft.auto.GeminiPlaceItemResponse;
import com.wego.wego.external.tourapi.place.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PlaceQueryRepository {
    Page<DraftPlanPlaceResponse> searchByTitleInCities(List<String> placeTypes,
                                      List<Long> cityCodeIds,
                                      String keyword,
                                      Pageable pageable);

    Page<GeminiPlaceItemResponse> searchGeminiPlaceItemResponseByTitleInCities(List<String> placeTypes,
                                                        List<Long> cityCodeIds,
                                                        Pageable pageable);

}
