package com.wego.wego.domain.plan.dto;

import java.time.LocalDate;
import java.util.List;

public record TravelPlanForGeminiRequest(
        LocalDate start_date,
        LocalDate end_date,
        List<TravelDayForAi> days
) {
    public record TravelDayForAi(
            String date,
            String start_time,
            String end_time
    ) {}
}