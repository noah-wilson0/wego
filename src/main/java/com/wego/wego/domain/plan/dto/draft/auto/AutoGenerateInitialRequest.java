package com.wego.wego.domain.plan.dto.draft.auto;

import lombok.ToString;

import java.util.List;


public record AutoGenerateInitialRequest(
        Long member_id,
        String region_name,
        String start_date,
        String end_date,
        ChemiSummaryForAiDto chemi,
        List<DayTime> day_times
) {
    public record DayTime(
            String date,        // "YYYY-MM-DD"
            String start_time,  // "HH:mm"
            String end_time     // "HH:mm"
    ) {}
}