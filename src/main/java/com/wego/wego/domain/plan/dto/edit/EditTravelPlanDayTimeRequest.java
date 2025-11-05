package com.wego.wego.domain.plan.dto.edit;

import java.time.LocalTime;

public record EditTravelPlanDayTimeRequest(
        LocalTime startTime,
        LocalTime endTime
){ }
