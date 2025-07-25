package com.wego.wego.global.enums;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum PlaceType {
    TOURIST_SPOT("A01", "명소"),
    RESTAURANT("A02", "음식점"),
    CAFE("A03", "카페"),
    ACCOMMODATION("B01", "숙소");

    private final String code;
    private final String description;

    PlaceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static List<String> getPlaceTypes() {
        return Stream.of(PlaceType.values())
                .map(PlaceType::getCode)
                .collect(Collectors.toList());
    }

    public static PlaceType fromCode(String code) {
        for (PlaceType type : PlaceType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PlaceType code: " + code);
    }
}

