package com.wego.wego.domain.feed.dto;

import java.time.LocalDate;

public record FeedInitResponse(
        String slug,
        LocalDate startDate,
        LocalDate endDate
) { }
