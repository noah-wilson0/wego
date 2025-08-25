package com.wego.wego.domain.plan.repository;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.entity.TravelPlan;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    Optional<TravelPlan> findByMember(Member member);

    List<TravelPlan> findTravelPlansByMember(Member member);

    @Query("select tp from TravelPlan tp where tp.id = :id and tp.member.id = :memberId")
    Optional<TravelPlan> findOneByIdAndMember(@Param("id") Long id, @Param("memberId") Long memberId);


    @Query(value = """
                select *
                from travel_plan tp
                where tp.member_id = :memberId
                  and tp.start_date >= :today
                order by tp.start_date asc, tp.created_at desc
                limit 1
            """, nativeQuery = true)
    Optional<TravelPlan> findTravelPlanByMemberAndImminentDate(@Param("memberId") Long memberId, @Param("today") LocalDate today);

    @Query("""
                select tp
                from TravelPlan tp
                where tp.member.id = :memberId
                  and tp.startDate >= :today
                order by tp.startDate asc, tp.createdAt desc
            """)
    List<TravelPlan> findTravelPlansByMemberAndExpireDate(@Param("memberId") Long memberId, @Param("today") LocalDate today);



}
