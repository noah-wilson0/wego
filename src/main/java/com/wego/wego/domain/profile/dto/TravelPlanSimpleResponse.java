package com.wego.wego.domain.profile.dto;

import java.time.LocalDate;

public record TravelPlanSimpleResponse(
        Long id,
        String slug,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate createdAt
){ }
