package com.wego.wego.domain.plan.util;

import com.wego.wego.domain.plan.dto.DraftPlanResponse;
import com.wego.wego.domain.plan.dto.draft.route.DraftPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.draft.route.RoutingSummary;
import com.wego.wego.external.tourapi.place.entity.Place;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 경로 요약(RouteLeg)과 하루 운행 시간(시작/종료)을 바탕으로
 * 각 방문지(명소/식당/카페/숙소)의 체류시간(start_time/end_time)을 계산하고
 * DraftPlanResponse의 DaySchedule/PlaceItem/AccommodationItem로 매핑하는 유틸.
 */
public final class RouteScheduleUtil {

    private RouteScheduleUtil() {}

    /** 편의 메서드: DaySpec + 구간 리스트 → DaySchedule 완성 */
    public static DraftPlanResponse.DaySchedule toDaySchedule(
            DraftPlanRoutingRequest.RoutingDaySpec daySpec,
            List<RoutingSummary.RouteLeg> legs
    ) {
        PlaceItems pi = toPlaceItems(daySpec, legs);
        return new DraftPlanResponse.DaySchedule(
                daySpec.date(),
                daySpec.start_time(),
                daySpec.end_time(),
                pi.places(),
                pi.accommodation()
        );
    }

    /** DaySpec + 구간 리스트 → PlaceItem 리스트와 AccommodationItem 계산 */
    public static PlaceItems toPlaceItems(
            DraftPlanRoutingRequest.RoutingDaySpec daySpec,
            List<RoutingSummary.RouteLeg> legs
    ) {
        // 방문 순서 리스트(원본 보호 위해 복사)
        List<Place> ordered = new ArrayList<>(daySpec.places());
        if (daySpec.accommodation() != null) {
            ordered.add(daySpec.accommodation());
        }

        // 시간창(체류 구간) 계산
        List<StayWindow> wins = allocateStayWindows(
                daySpec.start_time(),
                daySpec.end_time(),
                ordered,
                legs == null ? Collections.emptyList() : legs
        );

        List<DraftPlanResponse.PlaceItem> places = new ArrayList<>();
        DraftPlanResponse.AccommodationItem accommodation = null;

        for (int i = 0; i < ordered.size(); i++) {
            Place p = ordered.get(i);
            StayWindow w = wins.get(i);
            int seq = i + 1;

            if ("B01".equals(p.getPlaceType())) {
                accommodation = new DraftPlanResponse.AccommodationItem(
                        p.getContentId(),
                        p.getPlaceType(),
                        p.getTitle(),
                        p.getImage(),
                        seq,
                        safeDouble(p.getLongitude()),
                        safeDouble(p.getLatitude()),
                        w.start(),
                        w.end()
                );
            } else {
                places.add(new DraftPlanResponse.PlaceItem(
                        p.getContentId(),
                        p.getPlaceType(),
                        p.getTitle(),
                        p.getImage(),
                        seq,
                        safeDouble(p.getLongitude()),
                        safeDouble(p.getLatitude()),
                        w.start(),
                        w.end()
                ));
            }
        }

        return new PlaceItems(places, accommodation);
    }

    /**
     * 하루 운행 시작/종료 시각, 방문 순서, 구간 이동시간을 이용해
     * 각 아이템의 체류 시간창을 계산한다.
     * - 총 체류가능시간 = (dayEnd - dayStart) - Σ이동시간
     * - 각 아이템에 균등 분배, 마지막 아이템은 dayEnd로 정규화
     * - 이동시간 합이 너무 커도(체류시간 음수) 안전하게 클램프
     */
    public static List<StayWindow> allocateStayWindows(
            LocalTime dayStart,
            LocalTime dayEnd,
            List<Place> ordered,
            List<RoutingSummary.RouteLeg> legs
    ) {
        int n = ordered == null ? 0 : ordered.size();
        if (n == 0) return Collections.emptyList();

        long totalSec = Math.max(0L, Duration.between(dayStart, dayEnd).getSeconds());
        long moveSec = legs == null ? 0L : legs.stream().mapToLong(RoutingSummary.RouteLeg::duration).sum();
        long stayPool = Math.max(0L, totalSec - moveSec);
        long stayPer = stayPool / n;

        List<StayWindow> out = new ArrayList<>(n);
        LocalTime cursor = dayStart;

        for (int i = 0; i < n; i++) {
            LocalTime s = cursor;
            LocalTime e = plusSecondsSafe(s, stayPer);

            // 마지막 아이템은 종료를 dayEnd로 고정하고 시작이 넘어가면 클램프
            if (i == n - 1) {
                if (s.isAfter(dayEnd)) s = dayEnd;
                e = dayEnd;
            }

            out.add(new StayWindow(s, e));

            // 다음 시작 = 현재 종료 + 이동시간
            if (i < n - 1) {
                long leg = (legs != null && i < legs.size()) ? legs.get(i).duration() : 0L;
                cursor = plusSecondsSafe(e, leg);
            }
        }
        return out;
    }

    /* ========================= helpers ========================= */


    private static LocalTime plusSecondsSafe(LocalTime base, long sec) {
        if (sec <= 0) return base;
        return base.plusSeconds(sec);
    }

    private static double safeDouble(Object v) {
        if (v == null) return 0d;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0d; }
    }
}

