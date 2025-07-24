package com.wego.wego.domain.plan.dto;

public record RetryFailedDay(
        String date, // 예: "2025-07-15"
        String startTime,
        String endTime,
        FailedPlace failedPlaces
) {
    public record FailedPlace(
            String title,
            String addr,
            String tel
    ) {}
}


