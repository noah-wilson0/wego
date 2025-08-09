package com.wego.wego.domain.plan.repository;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    Optional<TravelPlan> findByMember(Member member);
}
