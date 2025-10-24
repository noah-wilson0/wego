package com.wego.wego.domain.placeStorage.repository;

import com.wego.wego.domain.placeStorage.dto.PlaceStorageItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlaceStorageQueryRepository {

    Page<PlaceStorageItemDto> findStorageItems(Long memberId, Long travelPlanId, Pageable pageable);
}
