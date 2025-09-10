package com.wego.wego.domain.settlement.repository;

import com.wego.wego.domain.settlement.entity.Settlement;
import com.wego.wego.domain.settlement.entity.SettlementItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementItemRepository extends JpaRepository<SettlementItem, Long> {
}
