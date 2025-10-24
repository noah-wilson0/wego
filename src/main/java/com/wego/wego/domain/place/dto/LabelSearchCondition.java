package com.wego.wego.domain.place.dto;

import java.util.List;

public record LabelSearchCondition(
        String label,
        String keyword,
        List<String> placeTypes
) {}
