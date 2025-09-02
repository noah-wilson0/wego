package com.wego.wego.domain.feed.dto;

import java.util.List;

public record FeedCreateRequest(
        String title,
        String description,
        int people,
        Long travelPlanId,
        List<Long> chemiIds           // [1, 2, ...]
) { }
