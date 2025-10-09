package com.wego.wego.domain.plan.dto.draft.auto;

import java.time.LocalDate;
import java.util.List;


public record DraftPlanGeminiResponse(
        String label,
        LocalDate start_date,
        LocalDate end_date,
        List<DraftPlanGeminiResponse.Days> days
) {

    public record Days(
            String date,
            String start_time,
            String end_time,
            List<DraftPlanGeminiResponse.Days.Places> places,
            DraftPlanGeminiResponse.Days.Accommodation accommodation

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
