package com.wego.wego.domain.plan.repository;

import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.entity.TravelPlanShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface TravelPlanShareRepository extends JpaRepository<TravelPlanShare, Long> {

    @Query("""
        SELECT ts.travelPlan
        FROM TravelPlanShare ts
        WHERE ts.token = :tokenHash
    """)
    Optional<TravelPlan> findTravelPlanByToken(@Param("tokenHash") String tokenHash,
                                                    @Param("now") LocalDate now);
}
