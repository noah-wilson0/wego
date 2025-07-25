package com.wego.wego.domain.plan.dto;

import java.time.LocalDate;

public record TempTravelDateRequest(
        LocalDate startDate,
        LocalDate endDate
) { }