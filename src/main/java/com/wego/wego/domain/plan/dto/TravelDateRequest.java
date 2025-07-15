package com.wego.wego.domain.plan.dto;

import java.time.LocalDate;

public record TravelDateRequest(
        LocalDate startDate,
        LocalDate endDate
) { }