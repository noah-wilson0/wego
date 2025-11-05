package com.wego.wego.global.util;

public class RedisKeyUtils {

    private static final String DRAFT_PLAN_BASE_PREFIX = "wego:draft-plan";
    private static final String TRAVEL_PLAN_BASE_PREFIX = "wego:travel-plan";

    // 여행 일정 날짜 저장 키
    public static String slugKey(String uuid) {
        return String.format("%s:%s:slug", DRAFT_PLAN_BASE_PREFIX, uuid);
    }

    // 여행 일정 날짜 저장 키
    public static String dateKey(String uuid) {
        return String.format("%s:%s:date", DRAFT_PLAN_BASE_PREFIX, uuid);
    }

    // 여행 일정 시간 저장 키
    public static String timeKey(String uuid) {
        return String.format("%s:%s:time", DRAFT_PLAN_BASE_PREFIX, uuid);
    }

    // 여행지 리스트 저장 키
    public static String placesKey(String uuid) {
        return String.format("%s:%s:places", DRAFT_PLAN_BASE_PREFIX, uuid);
    }

    // 숙소 리스트 저장 키
    public static String accommodationsKey(String uuid) {
        return String.format("%s:%s:accommodations", DRAFT_PLAN_BASE_PREFIX, uuid);
    }

    // 대중교통 경로 정보 저장 키
    public static String routeKey(String uuid) {
        return String.format("%s:%s:route", DRAFT_PLAN_BASE_PREFIX, uuid);
    }

    // 자동 추천 결과 저장 키
    public static String recommendKey(String uuid) {
        return String.format("%s:%s:recommend", DRAFT_PLAN_BASE_PREFIX, uuid);
    }

    // 임시 여행 일정 조회 키
    public static String tempScheduleKey(String uuid) {
        return String.format("%s:%s:temp", DRAFT_PLAN_BASE_PREFIX, uuid);
    }

    public static String editTravelPlanKey(String travelPlanId) {
        return String.format("%s:%s", TRAVEL_PLAN_BASE_PREFIX, travelPlanId);
    }


    public static String whiteListKey(String username) {
        return String.format("wego:auth:whitelist:%s", username);
    }

    public static String changePasswordBlackListKey(String username) {
        return String.format("wego:auth:blacklist:pw-change:%s", username);
    }
    public static String logoutBlackListKey(String username) {
        return String.format("wego:auth:blacklist:logout:%s", username);
    }

    public static String blackListKey(String username) {
        return String.format("wego:auth:blacklist:%s", username);
    }

    // 전체 prefix 반환 (디버깅, keys 조회용)
    public static String allSchedulePrefix() {
        return TRAVEL_PLAN_BASE_PREFIX + ":*";
    }
}

