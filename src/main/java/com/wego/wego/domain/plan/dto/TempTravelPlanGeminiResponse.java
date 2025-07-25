package com.wego.wego.domain.plan.dto;

import java.time.LocalDate;
import java.util.List;


public record TempTravelPlanGeminiResponse(
        LocalDate start_date,
        LocalDate end_date,
        List<TempTravelPlanGeminiResponse.Days> days
) {
    public record Days(
            String date,
            String start_time,
            String end_time,
            List<TempTravelPlanGeminiResponse.Days.Places> places,
            List<TempTravelPlanGeminiResponse.Days.Accommodation> accommodations

    ) {
        public record Places(
                String title,
                String addr,
                String tel
        ){}
        public record Accommodation(
                String title,
                String addr,
                String tel
        ){}
    }
}
