package com.wego.wego.domain.member.dto;

import java.time.LocalDate;

public record TravelPlanSimpleResponse(
        Long id,
        String slug,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate createdAt
){ }
