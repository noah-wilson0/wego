package com.wego.wego.domain.plan.util;

import com.wego.wego.domain.plan.dto.edit.TravelPlanNormalizeResponse;
import com.wego.wego.domain.plan.dto.edit.route.EditPlanRoutingRequest;
import com.wego.wego.domain.plan.dto.edit.route.EditPlanRoutingResponse;
import com.wego.wego.external.tourapi.place.entity.Place;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 편집 이후(교체/이동 등) 하루 단위 체류시간을 재계산하여
 * TravelPlanNormalizeResponse.DaySchedule 형태로 반환하는 전용 유틸.
 *
 * - 입력은 DraftPlanRoutingRequest / RoutingSummary를 그대로 사용
 * - Accommodation(B01)을 별도로 뒤에 붙이지 않으며, daySpec.places()만 대상으로 체류시간 계산
 * - 분배 로직은 RouteScheduleUtil과 동일한 가중치 기반(또는 균등 분배) 방식을 적용
 */
@NoArgsConstructor
public final class NormalizeStayTimeAllocator {

    /** 해당 날짜의 legs를 직접 넘겨 호출(가중치 기반). */
    public static TravelPlanNormalizeResponse.DaySchedule allocateTravelPlanWeightedStayTimes(
            EditPlanRoutingRequest.DailyRouteRequest day,
            List<EditPlanRoutingResponse.RouteEdge> routeEdges
    ) {
        // 1) places만 사용(숙소는 뒤에 붙이지 않음)
        List<Place> ordered = new ArrayList<>(day.getPlaces() == null ? List.of() : day.getPlaces());

        // 2) 시간창 계산(가중치)
        List<StayWindow> wins = allocateDailyWeightedStayTimes(
                day.getStart_time(),
                day.getEnd_time(),
                ordered,
                routeEdges == null ? Collections.emptyList() : routeEdges
        );

        // 3) wins → Normalize.PlaceItem으로 매핑
        List<TravelPlanNormalizeResponse.PlaceItem> placeItems = buildPlaceItems(ordered, wins);

        return new TravelPlanNormalizeResponse.DaySchedule(
                day.getDate(),
                day.getStart_time(),
                day.getEnd_time(),
                placeItems
        );
    }



    /* ========================= Core allocation ========================= */

    /** (가중치) 하루 운행 시작/종료, 방문 순서, 구간 이동시간 → 체류 시간창 */
    public static List<StayWindow> allocateDailyWeightedStayTimes(
            LocalTime dayStart,
            LocalTime dayEnd,
            List<Place> ordered,
            List<EditPlanRoutingResponse.RouteEdge> routeEdges
    ) {
        final int n = ordered == null ? 0 : ordered.size();
        if (n == 0) return Collections.emptyList();

        long totalSec = Math.max(0L, Duration.between(dayStart, dayEnd).getSeconds());
        long moveSec = routeEdges == null ? 0L : routeEdges.stream().mapToLong(EditPlanRoutingResponse.RouteEdge::duration).sum();
        long stayPool = Math.max(0L, totalSec - moveSec);

        // 타입별 가중치
        double[] weights = new double[n];
        double weightSum = 0.0;
        for (int i = 0; i < n; i++) {
            double w = typeWeight(ordered.get(i));
            weights[i] = w;
            weightSum += w;
        }
        if (weightSum <= 0) {
            Arrays.fill(weights, 1.0);
            weightSum = n;
        }

        long[] staySecs = new long[n];
        long allocated = 0;
        for (int i = 0; i < n; i++) {
            long chunk = Math.round(stayPool * (weights[i] / weightSum));
            staySecs[i] = chunk;
            allocated += chunk;
        }
        // 총합 보정(±1초씩)
        long diff = stayPool - allocated;
        int idx = 0;
        while (diff != 0 && n > 0) {
            if (diff > 0) { staySecs[idx] += 1; diff--; }
            else { if (staySecs[idx] > 0) { staySecs[idx] -= 1; diff++; } }
            idx = (idx + 1) % n;
        }

        // 시간창 생성
        List<StayWindow> out = new ArrayList<>(n);
        LocalTime cursor = dayStart;
        for (int i = 0; i < n; i++) {
            LocalTime s = cursor;
            LocalTime e = plusSecondsSafe(s, staySecs[i]);

            // 마지막 아이템은 종료를 dayEnd로 고정
            if (i == n - 1) {
                if (s.isAfter(dayEnd)) s = dayEnd;
                e = dayEnd;
            }
            out.add(new StayWindow(s, e));

            // 다음 시작 = 현재 종료 + 이동시간
            if (i < n - 1) {
                long leg = (routeEdges != null && i < routeEdges.size()) ? routeEdges.get(i).duration() : 0L;
                cursor = plusSecondsSafe(e, leg);
            }
        }
        return out;
    }


    /* ========================= Mapping ========================= */

    private static List<TravelPlanNormalizeResponse.PlaceItem> buildPlaceItems(
            List<Place> ordered,
            List<StayWindow> wins
    ) {
        int n = Math.min(ordered.size(), wins.size());
        List<TravelPlanNormalizeResponse.PlaceItem> out = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            Place p = ordered.get(i);
            StayWindow w = wins.get(i);
            int seq = i + 1;

            out.add(new TravelPlanNormalizeResponse.PlaceItem(
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
        return out;
    }

    /* ========================= Helpers ========================= */

    private static double typeWeight(Place p) {
        if (p == null || p.getPlaceType() == null) return 1.0;
        return switch (p.getPlaceType()) {
            case "A01" -> 1.0; // 명소
            case "A02" -> 0.8; // 음식
            case "A03" -> 0.6; // 카페
            case "B01" -> 0.2; // 숙소(여기선 포함 안 되더라도 방어값 유지)
            default -> 0.7;
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

    /** 내부 전달용(체류 시간창) */
    public record StayWindow(LocalTime start, LocalTime end) {}
}

