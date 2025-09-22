package com.wego.wego.domain.place.repository;

import com.wego.wego.domain.plan.dto.DraftPlanPlaceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PlaceQueryRepository {
    Page<DraftPlanPlaceResponse> searchByTitleInCities(List<String> placeTypes,
                                      List<Long> cityCodeIds,
                                      String keyword,
                                      Pageable pageable);
}
