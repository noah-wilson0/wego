package com.wego.wego.domain.settlement.repository;

import com.wego.wego.domain.settlement.entity.SettlementResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SettlementResultRepository extends JpaRepository<SettlementResult, Long> {


    @Query("""
            select 
            sr
             from SettlementResult sr 
            join fetch sr.settlement s
            join fetch sr.settlementParticipants sp
            where sr.settlement.id = :settlementId
            """)
    Optional<SettlementResult> findBySettlementId(@Param("settlementId")Long settlementId);
}
