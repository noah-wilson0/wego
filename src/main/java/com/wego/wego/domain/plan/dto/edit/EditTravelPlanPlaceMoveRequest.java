package com.wego.wego.domain.plan.dto.edit;

import java.time.LocalDate;

public record EditTravelPlanPlaceMoveRequest(
        int fromIndex,
        LocalDate toDay,
        int toIndex,
        String contentId
){}



