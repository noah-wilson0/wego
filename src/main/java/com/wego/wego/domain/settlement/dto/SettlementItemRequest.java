package com.wego.wego.domain.settlement.dto;

import com.wego.wego.global.enums.SettlementCategory;

public record SettlementItemRequest(
        SettlementCategory category,
        String participant,
        int paid
){}
