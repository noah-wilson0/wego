
//package com.wego.wego.domain.plan.util;
//
//import com.wego.wego.domain.plan.dto.DraftPlanResponse;
//import com.wego.wego.domain.plan.dto.draft.route.DraftPlanRoutingRequest;
//import com.wego.wego.domain.plan.dto.draft.route.RoutingSummary;
//import com.wego.wego.external.tourapi.place.entity.Place;
//
//import java.time.Duration;
//import java.time.LocalTime;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
///**
// * 경로 요약(RouteLeg)과 하루 운행 시간(시작/종료)을 바탕으로
// * 각 방문지(명소/식당/카페/숙소)의 체류시간(start_time/end_time)을 계산하고
// * DraftPlanResponse의 DaySchedule/PlaceItem/AccommodationItem로 매핑하는 유틸.
// *
// * 소요시간 제외 균등 시간 분배 방식
// */
//
//public final class RouteScheduleUtil {
//
//    private RouteScheduleUtil() {}
//
//    /** 편의 메서드: DaySpec + 구간 리스트 → DaySchedule 완성 */
//    public static DraftPlanResponse.DaySchedule toDaySchedule(
//            DraftPlanRoutingRequest.RoutingDaySpec daySpec,
//            List<RoutingSummary.RouteLeg> legs
//    ) {
//        PlaceItems pi = toPlaceItems(daySpec, legs);
//        return new DraftPlanResponse.DaySchedule(
//                daySpec.date(),
//                daySpec.start_time(),
//                daySpec.end_time(),
//                pi.places(),
//                pi.accommodation()
//        );
//    }
//
//    /** DaySpec + 구간 리스트 → PlaceItem 리스트와 AccommodationItem 계산 */
//    public static PlaceItems toPlaceItems(
//            DraftPlanRoutingRequest.RoutingDaySpec daySpec,
//            List<RoutingSummary.RouteLeg> legs
//    ) {
//        // 방문 순서 리스트(원본 보호 위해 복사)
//        List<Place> ordered = new ArrayList<>(daySpec.places());
//        if (daySpec.accommodation() != null) {
//            ordered.add(daySpec.accommodation());
//        }
//
//        // 시간창(체류 구간) 계산
//        List<StayWindow> wins = allocateStayWindows(
//                daySpec.start_time(),
//                daySpec.end_time(),
//                ordered,
//                legs == null ? Collections.emptyList() : legs
//        );
//
//        List<DraftPlanResponse.PlaceItem> places = new ArrayList<>();
//        DraftPlanResponse.AccommodationItem accommodation = null;
//
//        for (int i = 0; i < ordered.size(); i++) {
//            Place p = ordered.get(i);
//            StayWindow w = wins.get(i);
//            int seq = i + 1;
//
//            if ("B01".equals(p.getPlaceType())) {
//                accommodation = new DraftPlanResponse.AccommodationItem(
//                        p.getContentId(),
//                        p.getPlaceType(),
//                        p.getTitle(),
//                        p.getImage(),
//                        seq,
//                        safeDouble(p.getLongitude()),
//                        safeDouble(p.getLatitude()),
//                        w.start(),
//                        w.end()
//                );
//            } else {
//                places.add(new DraftPlanResponse.PlaceItem(
//                        p.getContentId(),
//                        p.getPlaceType(),
//                        p.getTitle(),
//                        p.getImage(),
//                        seq,
//                        safeDouble(p.getLongitude()),
//                        safeDouble(p.getLatitude()),
//                        w.start(),
//                        w.end()
//                ));
//            }
//        }
//
//        return new PlaceItems(places, accommodation);
//    }
//
//    /**
//     * 하루 운행 시작/종료 시각, 방문 순서, 구간 이동시간을 이용해
//     * 각 아이템의 체류 시간창을 계산한다.
//     * - 총 체류가능시간 = (dayEnd - dayStart) - Σ이동시간
//     * - 각 아이템에 균등 분배, 마지막 아이템은 dayEnd로 정규화
//     * - 이동시간 합이 너무 커도(체류시간 음수) 안전하게 클램프
//     */
//    public static List<StayWindow> allocateStayWindows(
//            LocalTime dayStart,
//            LocalTime dayEnd,
//            List<Place> ordered,
//            List<RoutingSummary.RouteLeg> legs
//    ) {
//        int n = ordered == null ? 0 : ordered.size();
//        if (n == 0) return Collections.emptyList();
//
//        long totalSec = Math.max(0L, Duration.between(dayStart, dayEnd).getSeconds());
//        long moveSec = legs == null ? 0L : legs.stream().mapToLong(RoutingSummary.RouteLeg::duration).sum();
//        long stayPool = Math.max(0L, totalSec - moveSec);
//        long stayPer = stayPool / n;
//
//        List<StayWindow> out = new ArrayList<>(n);
//        LocalTime cursor = dayStart;
//
//        for (int i = 0; i < n; i++) {
//            LocalTime s = cursor;
//            LocalTime e = plusSecondsSafe(s, stayPer);
//
//            // 마지막 아이템은 종료를 dayEnd로 고정하고 시작이 넘어가면 클램프
//            if (i == n - 1) {
//                if (s.isAfter(dayEnd)) s = dayEnd;
//                e = dayEnd;
//            }
//
//            out.add(new StayWindow(s, e));
//
//            // 다음 시작 = 현재 종료 + 이동시간
//            if (i < n - 1) {
//                long leg = (legs != null && i < legs.size()) ? legs.get(i).duration() : 0L;
//                cursor = plusSecondsSafe(e, leg);
//            }
//        }
//        return out;
//    }
//
//    /* ========================= helpers ========================= */
//
//
//    private static LocalTime plusSecondsSafe(LocalTime base, long sec) {
//        if (sec <= 0) return base;
//        return base.plusSeconds(sec);
//    }
//
//    private static double safeDouble(Object v) {
//        if (v == null) return 0d;
//        if (v instanceof Number n) return n.doubleValue();
//        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0d; }
//    }
//}

/**
 * 시간대 분류별 분배 로직
 *  장소 타입별 가중치 기반으로 체류시간 분배
 */
package com.wego.wego.domain.plan.util;

import com.wego.wego.domain.plan.dto.DraftPlanResponse;
import com.wego.wego.domain.plan.dto.draft.route.DraftPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.draft.route.RoutingSummary;
import com.wego.wego.external.tourapi.place.entity.Place;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

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

        // 시간창(체류 구간) 계산: ★ 가중치 기반 분배 로직 적용
        List<StayWindow> wins = allocateStayWindowsWeighted(
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
     * ★ 가중치 기반 체류시간 분배
     * - 총 체류가능시간 = (dayEnd - dayStart) - Σ이동시간
     * - 각 아이템에 타입별 weight 적용(A01 > A02 > A03 > B01)
     * - weight 합으로 정규화하여 per-item 체류시간 배분
     * - 마지막 아이템은 종료시각을 dayEnd로 강제 정렬(오버런/언더런 방지)
     */
    public static List<StayWindow> allocateStayWindowsWeighted(
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

        // 타입별 가중치(필요 시 조정 가능)
        // A01(명소) 1.0, A02(음식) 0.8, A03(카페) 0.6, B01(숙소) 0.2 (마지막날 null이면 없음)
        double[] weights = new double[n];
        double weightSum = 0.0;
        for (int i = 0; i < n; i++) {
            double w = typeWeight(ordered.get(i));
            weights[i] = w;
            weightSum += w;
        }
        if (weightSum <= 0) {
            // 비정상 상황 방어: 전부 1.0로 균등 분배
            Arrays.fill(weights, 1.0);
            weightSum = n;
        }

        // per-item 체류시간(초) 계산
        long[] staySecs = new long[n];
        long allocated = 0;
        for (int i = 0; i < n; i++) {
            long chunk = Math.round(stayPool * (weights[i] / weightSum));
            staySecs[i] = chunk;
            allocated += chunk;
        }
        // 합이 stayPool과 다를 수 있으니 보정
        long diff = stayPool - allocated;
        // 앞쪽부터 1초씩 채우거나 빼서 총합을 맞춤
        int idx = 0;
        while (diff != 0 && n > 0) {
            if (diff > 0) { staySecs[idx] += 1; diff--; }
            else { // diff < 0
                if (staySecs[idx] > 0) { staySecs[idx] -= 1; diff++; }
            }
            idx = (idx + 1) % n;
        }

        // 시간창 생성
        List<StayWindow> out = new ArrayList<>(n);
        LocalTime cursor = dayStart;

        for (int i = 0; i < n; i++) {
            LocalTime s = cursor;
            LocalTime e = plusSecondsSafe(s, staySecs[i]);

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

    private static double typeWeight(Place p) {
        if (p == null || p.getPlaceType() == null) return 1.0;
        return switch (p.getPlaceType()) {
            case "A01" -> 1.0; // 명소: 가장 길게
            case "A02" -> 0.8; // 음식: 명소보다는 약간 짧게
            case "A03" -> 0.6; // 카페: 비교적 짧게
            case "B01" -> 0.2; // 숙소: 최소 체류(경로 연결용)
            default -> 0.7;    // 기타 방어값
        };
    }

    private static LocalTime plusSecondsSafe(LocalTime base, long sec) {
        if (sec <= 0) return base;
        return base.plusSeconds(sec);
    }

    private static double safeDouble(Object v) {
        if (v == null) return 0d;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0d; }
    }

    /** 내부 전달용 DTO */
    public record StayWindow(LocalTime start, LocalTime end) {}

    /** 내부 전달용 DTO */
    public record PlaceItems(List<DraftPlanResponse.PlaceItem> places,
                             DraftPlanResponse.AccommodationItem accommodation) {}
}
