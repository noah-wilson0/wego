package com.wego.wego.global.enums;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum PlaceType {
    TOURIST_SPOT("12", "관광지"),
    CULTURAL_FACILITY("14", "문화시설"),
    FESTIVAL_PERFORMANCE_EVENT("15", "축제/공연/행사"),
    ACCOMMODATION("32", "숙박"),
    SHOPPING("38", "쇼핑"),
    RESTAURANT("39", "음식점");

    private final String code;
    private final String description;

    PlaceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String  getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
    public static List<String> getPlaceTypes(){
        return Stream.of(PlaceType.values())
                .map(PlaceType::getCode)
                .collect(Collectors.toList());
    }

    // code로 PlaceType 찾기
    public static PlaceType fromCode(String code) {
        for (PlaceType type : PlaceType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PlaceType code: " + code);
    }
}
