package com.wego.wego.domain.plan.service.edit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.plan.dto.edit.*;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.entity.TravelPlanDay;
import com.wego.wego.domain.plan.entity.TravelPlanPlace;
import com.wego.wego.domain.plan.entity.TravelPlanRoute;
import com.wego.wego.domain.plan.repository.TravelPlanRepository;
import com.wego.wego.domain.plan.service.support.SlugResolver;
import com.wego.wego.external.route.dto.RouteResult;
import com.wego.wego.external.route.kakao.sevice.KaKaoMobilityFetchService;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.service.PlaceService;
import com.wego.wego.global.enums.RouteType;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanEditService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private final TravelPlanRepository travelPlanRepository;
    private final PlaceService placeService;
    private final KaKaoMobilityFetchService kaKaoMobilityFetchService;

    private final SlugResolver slugResolver;



    @Transactional
    public void updateTravelPlan(Long travelPlanId) {
        // 1) Redis에서 최신 편집본 로드
        String key = RedisKeyUtils.editTravelPlanKey(String.valueOf(travelPlanId));
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new IllegalStateException("편집본이 Redis에 없습니다: key=" + key);
        }

        TravelPlanNormalizeResponse dto;
        try {
            dto = objectMapper.readValue(json, TravelPlanNormalizeResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 편집본 파싱 실패", e);
        }

        // 2) 부모 엔티티 로드
        TravelPlan plan = travelPlanRepository.findById(travelPlanId)
                .orElseThrow(() -> new IllegalArgumentException("찾을 수 없는 여행 일정: " + travelPlanId));

        // 3) 기존 Day/Place/Route 모두 제거(orphanRemoval=true 덕에 하위도 함께 삭제)
        if (plan.getTravelPlanDays() != null) {
            // 안전하게 끊고 비우기
            for (Iterator<TravelPlanDay> it = plan.getTravelPlanDays().iterator(); it.hasNext(); ) {
                TravelPlanDay day = it.next();
                it.remove();
                // day가 가진 place/route는 orphanRemoval=true라 함께 제거됨
            }
        }

        // 4) DTO → 엔티티 재구성
        // 4-1) Day 생성
        Map<LocalDate, TravelPlanDay> dayIndex = new LinkedHashMap<>();
        for (TravelPlanNormalizeResponse.DaySchedule dayDto : dto.days()) {
            TravelPlanDay day = TravelPlanDay.builder()
                    .date(dayDto.date())
                    .startTime(dayDto.start_time())
                    .endTime(dayDto.end_time())
                    .build();

            // 부모-자식 연관 연결
            plan.addTravelPlanDay(day);
            dayIndex.put(day.getDate(), day);

            // 4-2) Place 생성 및 연결
            if (dayDto.places() != null) {
                for (TravelPlanNormalizeResponse.PlaceItem p : dayDto.places()) {
                    Place place = placeService.findByContentId(p.content_id())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소: " + p.content_id()));

                    TravelPlanPlace tp = TravelPlanPlace.builder()
                            .place(place)
                            .sequence(p.sequence())
                            .startTime(p.start_time())
                            .endTime(p.end_time())
                            .build();

                    day.addTravelPlanPlace(tp);
                }
            }
        }

        // 4-3) Route 생성 및 연결
        // dto.routes()는 통상 1개(RouteType + 일자별 경로목록) 구조라고 가정(비어있을 수도 있으니 방어)
        if (dto.routes() != null && !dto.routes().isEmpty()) {
            TravelPlanNormalizeResponse.RouteInfo ri = dto.routes().getFirst();

            // route_type 문자열 → Enum
            RouteType routeType = null;
            if (ri.route_type() != null) {
                // 예: "car"/"transit" → RouteType.CAR/TRANSIT
                routeType = RouteType.valueOf(ri.route_type().toUpperCase());
            }

            Map<LocalDate, List<TravelPlanNormalizeResponse.RouteItem>> dailyRoutes = ri.dailyRoutes();
            if (dailyRoutes != null) {
                for (Map.Entry<LocalDate, List<TravelPlanNormalizeResponse.RouteItem>> entry : dailyRoutes.entrySet()) {
                    LocalDate date = entry.getKey();
                    TravelPlanDay day = dayIndex.get(date);
                    if (day == null) {
                        // 일자가 없는데 경로가 온 경우: 스킵하거나 예외 처리 선택
                        // 여기선 스킵
                        continue;
                    }

                    List<TravelPlanNormalizeResponse.RouteItem> items = entry.getValue();
                    if (items == null) continue;

                    for (TravelPlanNormalizeResponse.RouteItem r : items) {
                        Place origin = placeService.findByContentId(r.origin())
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소(origin): " + r.origin()));
                        Place destination = placeService.findByContentId(r.destination())
                                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소(destination): " + r.destination()));

                        TravelPlanRoute tr = TravelPlanRoute.builder()
                                .origin(origin)
                                .destination(destination)
                                .sequence(r.sequence())
                                .routeType(routeType != null ? routeType : RouteType.CAR) // 기본값 필요 시 지정
                                .duration(r.duration())
                                .build();

                        day.addTravelPlanRoute(tr); // 내부에서 tr.belongToTravelPlanDay(this)
                    }
                }
            }
        }

        // 5) 저장(cascade=ALL 이므로 day/place/route 함께 저장)
        travelPlanRepository.save(plan);

         redisTemplate.delete(key);

    }


    public void deleteTravelPlan(String travelPlanId) {
        redisTemplate.delete(RedisKeyUtils.editTravelPlanKey(String.valueOf(travelPlanId)));
    }

    public TravelPlanNormalizeResponse editInsert(
            Long travelPlanId,
            LocalDate date,
            EditTravelPlanPlaceInsertRequest req
    ) {
        TravelPlanNormalizeResponse plan = loadOrCreateEditPlan(travelPlanId);

        int dayIdx = indexOfDay(plan, date);
        if (dayIdx < 0) throw new IllegalArgumentException("해당 날짜 day가 없습니다: " + date);

        TravelPlanNormalizeResponse.DaySchedule day = plan.days().get(dayIdx);

        // Place 조회 및 새 아이템 구성
        Place p = placeService.findByContentId(req.contentId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 컨텐츠: " + req.contentId()));

        TravelPlanNormalizeResponse.PlaceItem newItem = new TravelPlanNormalizeResponse.PlaceItem(
                p.getContentId(),
                p.getPlaceType(),
                p.getTitle(),
                p.getImage(),
                /* sequence */ 0,
                safeDouble(p.getLongitude()),
                safeDouble(p.getLatitude()),
                null,
                null
        );

        // 삽입 + 재시퀀싱
        List<TravelPlanNormalizeResponse.PlaceItem> newPlaces = new ArrayList<>(day.places());
        int insertAt = Math.max(0, Math.min(req.index(), newPlaces.size()));
        newPlaces.add(insertAt, newItem);
        newPlaces = resequence(newPlaces);

        // day 교체(임시, 시간을 아직 재계산 전)
        TravelPlanNormalizeResponse.DaySchedule tempNewDay = new TravelPlanNormalizeResponse.DaySchedule(
                day.date(), day.start_time(), day.end_time(), newPlaces
        );

        // 라우트 재계산 + 체류시간 재분배
        String routeType = getRouteTypeOrDefault(plan);
        List<TravelPlanNormalizeResponse.RouteItem> legs = buildRoutesForDay(tempNewDay, routeType);
        TravelPlanNormalizeResponse.DaySchedule newDay = applyStayTimes(tempNewDay, legs);

        // days 교체
        List<TravelPlanNormalizeResponse.DaySchedule> newDays = replaceDay(plan.days(), dayIdx, newDay);

        // routes 갱신(해당 날짜만)
        Map<LocalDate, List<TravelPlanNormalizeResponse.RouteItem>> updatedDailyRoutes =
                new HashMap<>(firstRouteDailyMap(plan));
        updatedDailyRoutes.put(date, legs);

        TravelPlanNormalizeResponse updated = new TravelPlanNormalizeResponse(
                plan.label(), plan.start_date(), plan.end_date(),
                newDays,
                List.of(new TravelPlanNormalizeResponse.RouteInfo(routeType, updatedDailyRoutes)),
                plan.createdAt()
        );
        saveToRedis(travelPlanId, updated);
        return updated;
    }

    public TravelPlanNormalizeResponse editDelete(
            Long travelPlanId,
            LocalDate date,
            EditTravelPlanPlaceDeleteRequest req
    ) {
        TravelPlanNormalizeResponse plan = loadOrCreateEditPlan(travelPlanId);

        int dayIdx = indexOfDay(plan, date);
        if (dayIdx < 0) throw new IllegalArgumentException("해당 날짜 day가 없습니다: " + date);

        TravelPlanNormalizeResponse.DaySchedule day = plan.days().get(dayIdx);

        if (req.index() < 0 || req.index() >= day.places().size()) {
            throw new IllegalArgumentException("index 범위 오류");
        }
        if (req.contentId() != null) {
            String cidAtIndex = day.places().get(req.index()).content_id();
            if (!Objects.equals(cidAtIndex, req.contentId())) {
                throw new IllegalArgumentException("index와 contentId가 일치하지 않습니다");
            }
        }

        List<TravelPlanNormalizeResponse.PlaceItem> newPlaces = new ArrayList<>(day.places());
        newPlaces.remove(req.index());
        newPlaces = resequence(newPlaces);

        TravelPlanNormalizeResponse.DaySchedule tempNewDay = new TravelPlanNormalizeResponse.DaySchedule(
                day.date(), day.start_time(), day.end_time(), newPlaces
        );

        String routeType = getRouteTypeOrDefault(plan);
        List<TravelPlanNormalizeResponse.RouteItem> legs = buildRoutesForDay(tempNewDay, routeType);
        TravelPlanNormalizeResponse.DaySchedule newDay = applyStayTimes(tempNewDay, legs);

        List<TravelPlanNormalizeResponse.DaySchedule> newDays = replaceDay(plan.days(), dayIdx, newDay);

        Map<LocalDate, List<TravelPlanNormalizeResponse.RouteItem>> updatedDailyRoutes =
                new HashMap<>(firstRouteDailyMap(plan));
        updatedDailyRoutes.put(date, legs);

        TravelPlanNormalizeResponse updated = new TravelPlanNormalizeResponse(
                plan.label(), plan.start_date(), plan.end_date(),
                newDays,
                List.of(new TravelPlanNormalizeResponse.RouteInfo(routeType, updatedDailyRoutes)),
                plan.createdAt()
        );
        saveToRedis(travelPlanId, updated);
        return updated;
    }

    public TravelPlanNormalizeResponse editMove(
            Long travelPlanId,
            LocalDate date, // fromDay
            EditTravelPlanPlaceMoveRequest req
    ) {
        TravelPlanNormalizeResponse plan = loadOrCreateEditPlan(travelPlanId);

        int fromDayIdx = indexOfDay(plan, date);
        if (fromDayIdx < 0) throw new IllegalArgumentException("해당 날짜(from) day가 없습니다: " + date);

        int toDayIdx = indexOfDay(plan, req.toDay());
        if (toDayIdx < 0) throw new IllegalArgumentException("해당 날짜(to) day가 없습니다: " + req.toDay());

        TravelPlanNormalizeResponse.DaySchedule fromDay = plan.days().get(fromDayIdx);
        TravelPlanNormalizeResponse.DaySchedule toDay   = plan.days().get(toDayIdx);

        if (req.fromIndex() < 0 || req.fromIndex() >= fromDay.places().size()) {
            throw new IllegalArgumentException("fromIndex 범위 오류");
        }
        if (req.contentId() != null) {
            String cidAtFrom = fromDay.places().get(req.fromIndex()).content_id();
            if (!Objects.equals(cidAtFrom, req.contentId())) {
                throw new IllegalArgumentException("fromIndex와 contentId가 일치하지 않습니다");
            }
        }

        // 1) 추출
        List<TravelPlanNormalizeResponse.PlaceItem> fromPlaces = new ArrayList<>(fromDay.places());
        TravelPlanNormalizeResponse.PlaceItem moving = fromPlaces.remove(req.fromIndex());

        // 같은 날 이동이면 toIndex 보정
        int toIndex = req.toIndex();
        if (date.equals(req.toDay()) && req.fromIndex() < req.toIndex()) {
            toIndex = req.toIndex() - 1;
        }

        // 2) 삽입
        List<TravelPlanNormalizeResponse.PlaceItem> toPlaces =
                date.equals(req.toDay()) ? fromPlaces : new ArrayList<>(toDay.places());
        int insertAt = Math.max(0, Math.min(toIndex, toPlaces.size()));
        toPlaces.add(insertAt, moving);

        // 3) 재시퀀싱
        fromPlaces = resequence(fromPlaces);
        toPlaces   = resequence(toPlaces);

        // 4) day 스냅샷
        TravelPlanNormalizeResponse.DaySchedule tempFromDay = new TravelPlanNormalizeResponse.DaySchedule(
                fromDay.date(), fromDay.start_time(), fromDay.end_time(), fromPlaces
        );
        TravelPlanNormalizeResponse.DaySchedule tempToDay = new TravelPlanNormalizeResponse.DaySchedule(
                toDay.date(), toDay.start_time(), toDay.end_time(), toPlaces
        );

        // 5) 라우트 재계산 + 체류시간 재배치
        String routeType = getRouteTypeOrDefault(plan);
        List<TravelPlanNormalizeResponse.RouteItem> fromLegs = buildRoutesForDay(tempFromDay, routeType);
        List<TravelPlanNormalizeResponse.RouteItem> toLegs   = date.equals(req.toDay())
                ? fromLegs
                : buildRoutesForDay(tempToDay, routeType);

        TravelPlanNormalizeResponse.DaySchedule newFromDay = applyStayTimes(tempFromDay, fromLegs);
        TravelPlanNormalizeResponse.DaySchedule newToDay   = date.equals(req.toDay())
                ? newFromDay
                : applyStayTimes(tempToDay, toLegs);

        // 6) days 교체
        List<TravelPlanNormalizeResponse.DaySchedule> newDays = new ArrayList<>(plan.days());
        newDays.set(fromDayIdx, newFromDay);
        newDays.set(toDayIdx, newToDay);

        // 7) routes 갱신(같은 날이면 1개, 다르면 2개)
        Map<LocalDate, List<TravelPlanNormalizeResponse.RouteItem>> updatedDailyRoutes =
                new HashMap<>(firstRouteDailyMap(plan));
        updatedDailyRoutes.put(newFromDay.date(), fromLegs);
        if (!newFromDay.date().equals(newToDay.date())) {
            updatedDailyRoutes.put(newToDay.date(), toLegs);
        }

        TravelPlanNormalizeResponse updated = new TravelPlanNormalizeResponse(
                plan.label(), plan.start_date(), plan.end_date(),
                newDays,
                List.of(new TravelPlanNormalizeResponse.RouteInfo(routeType, updatedDailyRoutes)),
                plan.createdAt()
        );
        saveToRedis(travelPlanId, updated);
        return updated;
    }

    public TravelPlanNormalizeResponse changeTravelPlanDayTime(Long travelPlanId,
                                                               LocalDate date,
                                                               EditTravelPlanDayTimeRequest req) {
        final String key = RedisKeyUtils.editTravelPlanKey(String.valueOf(travelPlanId));
        final String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new IllegalStateException("편집본이 Redis에 없습니다: key=" + key);
        }

        final TravelPlanNormalizeResponse dto;
        try {
            dto = objectMapper.readValue(json, TravelPlanNormalizeResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 편집본 파싱 실패", e);
        }

        var targetDay = dto.days().stream()
                .filter(d -> d.date().equals(date))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 일정이 없습니다: " + date));

        LocalTime newStart = (req.startTime() != null) ? req.startTime() : targetDay.start_time();
        LocalTime newEnd   = (req.endTime() != null) ? req.endTime() : targetDay.end_time();


        if (!newStart.isBefore(newEnd)) {
            throw new IllegalArgumentException("시작시간은 종료시간보다 이전이어야 합니다. start=" + newStart + ", end=" + newEnd);
        }

        List<TravelPlanNormalizeResponse.RouteItem> legsForDate = List.of();
        if (dto.routes() != null && !dto.routes().isEmpty()) {
            var routeInfo = dto.routes().get(0); // 보통 하나 존재
            if (routeInfo != null && routeInfo.dailyRoutes() != null) {
                var list = routeInfo.dailyRoutes().get(date);
                if (list != null) {
                    legsForDate = list.stream()
                            .sorted(Comparator.comparingInt(TravelPlanNormalizeResponse.RouteItem::sequence))
                            .toList();
                }
            }
        }

        TravelPlanNormalizeResponse.DaySchedule temp = new TravelPlanNormalizeResponse.DaySchedule(
                targetDay.date(),
                newStart,
                newEnd,
                targetDay.places()
        );

        var updatedDay = applyStayTimes(temp, legsForDate);

        final var finalUpdatedDay = updatedDay;
        List<TravelPlanNormalizeResponse.DaySchedule> newDays = dto.days().stream()
                .map(d -> d.date().equals(date) ? finalUpdatedDay : d)
                .toList();

        // 8) 새 DTO 조립
        TravelPlanNormalizeResponse updatedDto = new TravelPlanNormalizeResponse(
                dto.label(),
                dto.start_date(),
                dto.end_date(),
                newDays,
                dto.routes(),   // routes는 그대로 유지
                dto.createdAt()
        );

        // 9) Redis 저장
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(updatedDto));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("편집본 직렬화 실패", e);
        }

        // 10) 최신본 반환
        return updatedDto;
    }





    public TravelPlanNormalizeResponse getNormalizeTravelPlan(Long travelPlanId) {
        return loadOrCreateEditPlan(travelPlanId);
    }

    private TravelPlanNormalizeResponse loadOrCreateEditPlan(Long travelPlanId) {
        String key = RedisKeyUtils.editTravelPlanKey(String.valueOf(travelPlanId));

        if (!redisTemplate.hasKey(key)) {
            TravelPlan travelPlan = travelPlanRepository.findById(travelPlanId)
                    .orElseThrow(() -> new RuntimeException("존재 하지 않는 여행 일정"));

            try {
                TravelPlanNormalizeResponse travelPlanNormalizeResponse = new TravelPlanNormalizeResponse(travelPlan);
                String label = slugResolver.resolveLabel(travelPlan.getSlug());
                String json = objectMapper.writeValueAsString(travelPlanNormalizeResponse.withLabel(label));
                redisTemplate.opsForValue().set(key, json);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        String json = redisTemplate.opsForValue().get(key);
        try {
            return objectMapper.readValue(json, TravelPlanNormalizeResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis travelPlan 파싱 실패", e);
        }
    }

    private void saveToRedis(Long travelPlanId, TravelPlanNormalizeResponse updated) {
        String key = RedisKeyUtils.editTravelPlanKey(String.valueOf(travelPlanId));
        try {
            String json = objectMapper.writeValueAsString(updated);
            redisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Redis 저장 실패", e);
        }
    }

    /* ===================== helpers ===================== */

    private int indexOfDay(TravelPlanNormalizeResponse plan, LocalDate date) {
        for (int i = 0; i < plan.days().size(); i++) {
            if (plan.days().get(i).date().equals(date)) return i;
        }
        return -1;
    }

    private List<TravelPlanNormalizeResponse.DaySchedule> replaceDay(
            List<TravelPlanNormalizeResponse.DaySchedule> days,
            int idx,
            TravelPlanNormalizeResponse.DaySchedule newDay
    ) {
        List<TravelPlanNormalizeResponse.DaySchedule> list = new ArrayList<>(days);
        list.set(idx, newDay);
        return list;
    }

    /** sequence를 1..n으로 다시 매기기 */
    private List<TravelPlanNormalizeResponse.PlaceItem> resequence(
            List<TravelPlanNormalizeResponse.PlaceItem> items
    ) {
        List<TravelPlanNormalizeResponse.PlaceItem> out = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            TravelPlanNormalizeResponse.PlaceItem p = items.get(i);
            out.add(new TravelPlanNormalizeResponse.PlaceItem(
                    p.content_id(),
                    p.placeType(),
                    p.title(),
                    p.image(),
                    i + 1,
                    p.longitude(),
                    p.latitude(),
                    p.start_time(),
                    p.end_time()
            ));
        }
        return out;
    }

    /** 기존 라우트 정보(Map<date, routes>) 추출 (없으면 빈 맵) */
    private Map<LocalDate, List<TravelPlanNormalizeResponse.RouteItem>> firstRouteDailyMap(
            TravelPlanNormalizeResponse plan
    ) {
        if (plan.routes() == null || plan.routes().isEmpty() || plan.routes().get(0) == null) {
            return new HashMap<>();
        }
        return new HashMap<>(plan.routes().get(0).dailyRoutes());
    }

    private String getRouteTypeOrDefault(TravelPlanNormalizeResponse plan) {
        if (plan.routes() != null && !plan.routes().isEmpty() && plan.routes().get(0) != null) {
            String rt = plan.routes().get(0).route_type();
            if (rt != null) return rt;
        }
        return "car"; // 기본값
    }

    /** 해당 Day의 places를 인접쌍으로 묶어 카카오 경로를 호출해 RouteItem 리스트 구성 */
    private List<TravelPlanNormalizeResponse.RouteItem> buildRoutesForDay(
            TravelPlanNormalizeResponse.DaySchedule day,
            String routeType
    ) {
        List<TravelPlanNormalizeResponse.PlaceItem> places = day.places();
        if (places == null || places.size() < 2) return List.of();

        // contentId → Place 매핑
        List<String> cids = places.stream().map(TravelPlanNormalizeResponse.PlaceItem::content_id).distinct().toList();
        Map<String, Place> placeMap = placeService.findByContentIdIn(cids).stream()
                .collect(Collectors.toMap(Place::getContentId, p -> p));

        List<TravelPlanNormalizeResponse.RouteItem> legs = new ArrayList<>();
        for (int i = 0; i < places.size() - 1; i++) {
            String originId = places.get(i).content_id();
            String destId   = places.get(i + 1).content_id();

            Place origin = placeMap.get(originId);
            Place dest   = placeMap.get(destId);

            if (origin == null || dest == null) {
                log.warn("Place 누락으로 해당 구간 스킵: {} -> {}", originId, destId);
                continue;
            }

            RouteResult rr = kaKaoMobilityFetchService.fetchKaKaoMobilityData(origin, dest);
            legs.add(new TravelPlanNormalizeResponse.RouteItem(
                    i + 1,
                    rr.getOriginId(),
                    rr.getDestinationId(),
                    rr.getDuration()
            ));
        }
        return legs;
    }

    /** === 체류시간 재분배(가중치 방식) 후 DaySchedule 갱신 === */
    private TravelPlanNormalizeResponse.DaySchedule applyStayTimes(
            TravelPlanNormalizeResponse.DaySchedule day,
            List<TravelPlanNormalizeResponse.RouteItem> legs
    ) {
        List<TravelPlanNormalizeResponse.PlaceItem> places = day.places();
        int n = places == null ? 0 : places.size();
        if (n == 0) return day;

        LocalTime dayStart = day.start_time();
        LocalTime dayEnd   = day.end_time();
        long totalSec = Math.max(0L, Duration.between(dayStart, dayEnd).getSeconds());
        long moveSec = legs == null ? 0L : legs.stream().mapToLong(TravelPlanNormalizeResponse.RouteItem::duration).sum();
        long stayPool = Math.max(0L, totalSec - moveSec);

        // 가중치
        double[] weights = new double[n];
        double weightSum = 0d;
        for (int i = 0; i < n; i++) {
            weights[i] = typeWeight(places.get(i).placeType());
            weightSum += weights[i];
        }
        if (weightSum <= 0) {
            Arrays.fill(weights, 1.0);
            weightSum = n;
        }

        // 분배
        long[] staySecs = new long[n];
        long allocated = 0;
        for (int i = 0; i < n; i++) {
            long chunk = Math.round(stayPool * (weights[i] / weightSum));
            staySecs[i] = chunk;
            allocated += chunk;
        }
        long diff = stayPool - allocated;
        int idx = 0;
        while (diff != 0 && n > 0) {
            if (diff > 0) { staySecs[idx] += 1; diff--; }
            else { if (staySecs[idx] > 0) { staySecs[idx] -= 1; diff++; } }
            idx = (idx + 1) % n;
        }

        // 타임라인 생성
        List<TravelPlanNormalizeResponse.PlaceItem> out = new ArrayList<>(n);
        LocalTime cursor = dayStart;

        for (int i = 0; i < n; i++) {
            LocalTime s = cursor;
            LocalTime e = plusSecondsSafe(s, staySecs[i]);

            // 마지막 아이템은 종료를 dayEnd로 고정
            if (i == n - 1) {
                if (s.isAfter(dayEnd)) s = dayEnd;
                e = dayEnd;
            }

            TravelPlanNormalizeResponse.PlaceItem p = places.get(i);
            out.add(new TravelPlanNormalizeResponse.PlaceItem(
                    p.content_id(),
                    p.placeType(),
                    p.title(),
                    p.image(),
                    i + 1,
                    p.longitude(),
                    p.latitude(),
                    s,
                    e
            ));

            // 다음 시작 = 현재 종료 + 이동시간
            if (i < n - 1) {
                long leg = (legs != null && i < legs.size()) ? legs.get(i).duration() : 0L;
                cursor = plusSecondsSafe(e, leg);
            }
        }

        return new TravelPlanNormalizeResponse.DaySchedule(day.date(), dayStart, dayEnd, out);
    }

    /* === small utils === */

    private static LocalTime plusSecondsSafe(LocalTime base, long sec) {
        if (sec <= 0) return base;
        return base.plusSeconds(sec);
    }

    private static double safeDouble(Object v) {
        if (v == null) return 0d;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0d; }
    }

    private static double typeWeight(String placeType) {
        if (placeType == null) return 1.0;
        return switch (placeType) {
            case "A01" -> 1.0; // 명소
            case "A02" -> 0.8; // 식당
            case "A03" -> 0.6; // 카페
            case "B01" -> 0.2; // 숙소
            default -> 0.7;
        };
    }


}
