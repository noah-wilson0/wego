package com.wego.wego.domain.settlement.repository;

import com.wego.wego.domain.settlement.entity.SettlementParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementParticipantRepository extends JpaRepository<SettlementParticipant, Long> {
}
