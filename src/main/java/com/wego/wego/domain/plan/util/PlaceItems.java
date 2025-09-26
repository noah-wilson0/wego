package com.wego.wego.domain.plan.util;

import com.wego.wego.domain.plan.dto.DraftPlanResponse;

import java.util.List;

public record PlaceItems(
        List<DraftPlanResponse.PlaceItem> places,
        DraftPlanResponse.AccommodationItem accommodation
) {}
