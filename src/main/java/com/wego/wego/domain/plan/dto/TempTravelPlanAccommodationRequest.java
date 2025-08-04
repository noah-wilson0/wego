package com.wego.wego.domain.plan.dto;

import java.time.LocalDate;

public record TempTravelPlanAccommodationRequest(
        LocalDate date,
        String contentId
){ }
