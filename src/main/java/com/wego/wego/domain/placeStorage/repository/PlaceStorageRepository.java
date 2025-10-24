package com.wego.wego.domain.placeStorage.repository;

import com.wego.wego.domain.placeStorage.entity.PlaceStorage;
import com.wego.wego.domain.placeStorage.entity.PlaceStorageId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceStorageRepository extends JpaRepository<PlaceStorage, PlaceStorageId>, PlaceStorageQueryRepository {

    void deleteByMemberIdAndTravelPlanId(Long memberId, Long planId);
}
