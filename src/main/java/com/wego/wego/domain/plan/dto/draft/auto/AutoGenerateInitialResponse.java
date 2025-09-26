package com.wego.wego.domain.plan.dto.draft.auto;

public record AutoGenerateInitialResponse(
        Long member_id,
        String region_name,
        String start_date,
        String end_date,
        ChemiSummaryForAiDto chemi
) {}