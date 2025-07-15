package com.wego.wego.global.util;

public class RedisKeyUtils {

    private static final String SCHEDULE_BASE_PREFIX = "wego:schedule";

    // 여행 일정 날짜 저장 키
    public static String dateKey(String uuid) {
        return String.format("%s:%s:date", SCHEDULE_BASE_PREFIX, uuid);
    }

    // 여행 일정 시간 저장 키
    public static String timeKey(String uuid) {
        return String.format("%s:%s:time", SCHEDULE_BASE_PREFIX, uuid);
    }

    // 여행지 리스트 저장 키
    public static String placesKey(String uuid) {
        return String.format("%s:%s:places", SCHEDULE_BASE_PREFIX, uuid);
    }

    // 숙소 리스트 저장 키
    public static String accommodationsKey(String uuid) {
        return String.format("%s:%s:accommodations", SCHEDULE_BASE_PREFIX, uuid);
    }

    // 경로 정보 저장 키 (ex: 자동차 or 대중교통 구분 필요 시 routeType 추가 가능)
    public static String routeKey(String uuid) {
        return String.format("%s:%s:route", SCHEDULE_BASE_PREFIX, uuid);
    }

    // 자동 추천 결과 저장 키
    public static String recommendKey(String uuid) {
        return String.format("%s:%s:recommend", SCHEDULE_BASE_PREFIX, uuid);
    }

    // 특정 추천 카드 ID로 저장할 경우 (선택사항)
    public static String recommendKey(String uuid, String cardId) {
        return String.format("%s:%s:recommend:%s", SCHEDULE_BASE_PREFIX, uuid, cardId);
    }

    // 전체 prefix 반환 (디버깅, keys 조회용)
    public static String allSchedulePrefix() {
        return SCHEDULE_BASE_PREFIX + ":*";
    }
}

