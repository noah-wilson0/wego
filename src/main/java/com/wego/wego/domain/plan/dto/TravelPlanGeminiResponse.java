package com.wego.wego.domain.plan.dto;

import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;


public record TravelPlanGeminiResponse (
        LocalDate start_date,
        LocalDate end_date,
        List<TravelPlanGeminiResponse.Days> days
) {
    public record Days(
            String date,
            String start_time,
            String end_time,
            List<TravelPlanGeminiResponse.Days.Places> places,
            List<TravelPlanGeminiResponse.Days.Accommodation> accommodations

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
