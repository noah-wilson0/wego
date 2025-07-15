package com.wego.wego.domain.plan.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TravelTimeRequest(List<TravelDayTimes> travelDayTimes) {

    public record TravelDayTimes(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {}
}
