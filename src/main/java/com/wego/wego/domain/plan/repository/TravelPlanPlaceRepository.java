package com.wego.wego.domain.plan.repository;

import com.wego.wego.domain.plan.entity.TravelPlanPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TravelPlanPlaceRepository extends JpaRepository<TravelPlanPlace, Long> {


}
