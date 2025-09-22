package com.wego.wego.domain.place.dto;

import java.util.List;

public record SearchCondition(
        String uuid,
        String keyword,
        List<String> placeTypes
) {}
