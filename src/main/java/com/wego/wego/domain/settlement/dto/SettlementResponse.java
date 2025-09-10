package com.wego.wego.domain.settlement.dto;


import com.wego.wego.domain.settlement.entity.Settlement;
import com.wego.wego.domain.settlement.entity.SettlementItem;

import java.time.LocalDateTime;
import java.util.List;

public record SettlementResponse(
        Long settlementId,
        int budget,
        int totalPaid,
        List<SettlementItemResponse> items
) {
    public record SettlementItemResponse(
            Long id,
            String participant,
            String category,
            int paid,
            LocalDateTime paidAt
    ) {
        public static SettlementItemResponse from(SettlementItem settlementItem) {
            return new SettlementItemResponse(
                    settlementItem.getId(),
                    settlementItem.getMember().getName(),
                    settlementItem.getCategory().name(),
                    settlementItem.getPaid(),
                    settlementItem.getPaidAt()

            );
        }
    }


    public static SettlementResponse from(Settlement settlement) {
        List<SettlementItemResponse> list = settlement.getSettlementItems().stream()
                .map(SettlementItemResponse::from).toList();

        return new SettlementResponse(
                settlement.getId(),
                settlement.getBudget(),
                settlement.getTotalPaid(),
                list
        );
    }
}


