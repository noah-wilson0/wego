package com.wego.wego.domain.settlement.repository;

import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.settlement.entity.Settlement;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("select s from Settlement s left join fetch SettlementItem si on(s.id=si.settlement.id) where s.travelPlan.id = :travelPlanId")
    Optional<Settlement> findWithItemsByTravelPlanId(@Param("travelPlanId") Long travelPlanId);

    Optional<Settlement> findByTravelPlan_Id(Long travelPlanId);
}
