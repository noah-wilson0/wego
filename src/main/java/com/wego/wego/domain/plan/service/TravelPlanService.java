package com.wego.wego.domain.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.service.MemberService;
import com.wego.wego.domain.plan.dto.TempTravelPlanResponse;
import com.wego.wego.domain.plan.dto.TravelPlanResponse;
import com.wego.wego.domain.plan.dto.TravelPlanRouteJson;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.entity.TravelPlanDay;
import com.wego.wego.domain.plan.entity.TravelPlanPlace;
import com.wego.wego.domain.plan.entity.TravelPlanRoute;
import com.wego.wego.domain.plan.repository.TravelPlanRepository;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.service.PlaceService;
import com.wego.wego.global.enums.PlaceType;
import com.wego.wego.global.enums.RouteType;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TravelPlanService {

    private final PlaceService placeService;
    private final TravelPlanRepository travelPlanRepository;

    private final MemberService memberService;

    private final RedisTemplate<String,String > redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveTempSlug(String slug, String uuid) {
        redisTemplate.opsForValue().set(RedisKeyUtils.slugKey(uuid), slug,6, TimeUnit.HOURS);
    }

    public String getTempTravelPlan(String uuid) {

        String routes = redisTemplate.opsForValue().get(RedisKeyUtils.routeKey(uuid));
        TravelPlanRouteJson travelPlanRouteJson;
        try {
            travelPlanRouteJson = objectMapper.readValue(routes, TravelPlanRouteJson.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        List<TempTravelPlanResponse.DaySchedule> newDays=new ArrayList<>();
        for (int i = 0; i < travelPlanRouteJson.days().size(); i++) {
            TravelPlanRouteJson.DaySchedule daySchedule = travelPlanRouteJson.days().get(i);

            //places 생성
            List<TempTravelPlanResponse.PlaceItem> newPlaces=new ArrayList<>();
            for (int j = 0; j < daySchedule.places().size(); j++) {
                TravelPlanRouteJson.PlaceItem placeItem = daySchedule.places().get(j);
                Place place = placeService.findByContentId(placeItem.content_id()).get();
                newPlaces.add(new TempTravelPlanResponse.PlaceItem(
                        placeItem.content_id(),
                        place.getPlaceType(),
                        place.getTitle(),
                        place.getImage(),
                        placeItem.sequence(),
                        placeItem.start_time(),
                        placeItem.end_time()
                ));
            }

            //accommodation 생성
            TempTravelPlanResponse.AccommodationItem newAccommodationItem = null;

            if (daySchedule.accommodation() != null) {
                Place accommodation = placeService.findByContentId(daySchedule.accommodation().content_id()).get();
                newAccommodationItem=new TempTravelPlanResponse.AccommodationItem(
                        daySchedule.accommodation().content_id(),
                        accommodation.getPlaceType(),
                        accommodation.getTitle(),
                        accommodation.getImage(),
                        daySchedule.accommodation().sequence(),
                        daySchedule.accommodation().start_time(),
                        daySchedule.accommodation().end_time()
                );
            }

            //DaySchedule 추가
            newDays.add(new TempTravelPlanResponse.DaySchedule(
                    daySchedule.date(),
                    daySchedule.start_time(),
                    daySchedule.end_time(),
                    newPlaces,
                    newAccommodationItem
            ));
        }

        List<TempTravelPlanResponse.RouteInfo> newRoutes=new ArrayList<>();
        for (int i = 0; i < travelPlanRouteJson.routes().size(); i++) {
            TravelPlanRouteJson.RouteInfo routeInfo = travelPlanRouteJson.routes().get(i);

            Map<LocalDate,List<TempTravelPlanResponse.RouteDetail>> newDailyRoutes=new HashMap<>();
            List<TempTravelPlanResponse.RouteDetail> newRouteDetails = new ArrayList<>();

            List<TravelPlanRouteJson.RouteDetail> routeDetails = routeInfo.daily_route().get(travelPlanRouteJson.days().get(i).date());
            for (int j = 0; j < routeDetails.size(); j++) {
                TempTravelPlanResponse.RouteDetail newRouteDetail = new TempTravelPlanResponse.RouteDetail(
                        routeDetails.get(j).sequence(),
                        routeDetails.get(j).origin(),
                        routeDetails.get(j).destination(),
                        routeDetails.get(j).duration()
                );
                newRouteDetails.add(newRouteDetail);
            }
            newDailyRoutes.put(travelPlanRouteJson.days().get(i).date(),newRouteDetails);
            newRoutes.add(new TempTravelPlanResponse.RouteInfo(routeInfo.route_type(), newDailyRoutes));
        }

        TempTravelPlanResponse tempTravelPlanResponse=new TempTravelPlanResponse(
                travelPlanRouteJson.slug(),
                travelPlanRouteJson.start_date(),
                travelPlanRouteJson.end_date(),
                newDays,
                newRoutes
        );
        String response;
        try {
            response = objectMapper.writeValueAsString(tempTravelPlanResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        redisTemplate.opsForValue().set(RedisKeyUtils.tempScheduleKey(uuid), response,6, TimeUnit.HOURS);

        return response;
    }

    @Transactional
    public void persistTravelPlan(String uuid, Member member) {
        String json = redisTemplate.opsForValue().get(RedisKeyUtils.tempScheduleKey(uuid));
        if (json == null) {
            throw new IllegalStateException("임시 일정이 없습니다. uuid=" + uuid);
        }

        TempTravelPlanResponse tempTravelPlanResponse;
        try {
            tempTravelPlanResponse = objectMapper.readValue(json, TempTravelPlanResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("임시 일정 파싱 실패", e);
        }
        Member managed = memberService.findByMemberId(member.getId())
                .orElseThrow(() -> new RuntimeException("등록되지 않은 회원"));
        // ↓ 이 managed를 쓰세요
        // 1) plan 만들기 (title, createdAt은 엔티티 default 활용)
        TravelPlan plan = TravelPlan.builder()
                .slug(tempTravelPlanResponse.slug())
                .startDate(tempTravelPlanResponse.start_date())
                .endDate(tempTravelPlanResponse.end_date())
                .member(managed)
                .build();

        // 2) days & places
        List<TravelPlanDay> travelPlanDays = new ArrayList<>();
        Map<LocalDate, TravelPlanDay> dayByDate = new HashMap<>();

        for (int i = 0; i < tempTravelPlanResponse.days().size(); i++) {
            TempTravelPlanResponse.DaySchedule daySchedule = tempTravelPlanResponse.days().get(i);

            // ✅ 부모 plan을 Day에 즉시 연결 (FK 세팅)
            TravelPlanDay travelPlanDay = TravelPlanDay.builder()
                    .travelPlan(plan)
                    .date(daySchedule.date())
                    .startTime(daySchedule.start_time())
                    .endTime(daySchedule.end_time())
                    .build();

            List<TravelPlanPlace> placesForThisDay = new ArrayList<>();

            // 장소들
            for (int j = 0; j < daySchedule.places().size(); j++) {
                TempTravelPlanResponse.PlaceItem placeItem = daySchedule.places().get(j);

                Place place = placeService.findByContentId(placeItem.content_id())
                        .orElseThrow(() -> new RuntimeException("장소 없음: " + placeItem.content_id()));

                TravelPlanPlace travelPlanPlace = TravelPlanPlace.builder()
                        .travelPlanDay(travelPlanDay)  // FK(자식 -> 부모)
                        .place(place)
                        .sequence(placeItem.sequence())
                        .startTime(placeItem.start_time())
                        .endTime(placeItem.end_time())
                        .build();

                placesForThisDay.add(travelPlanPlace);
            }

            // 숙소(있을 때만)
            if (daySchedule.accommodation() != null) {
                TempTravelPlanResponse.AccommodationItem acc = daySchedule.accommodation();

                Place accPlace = placeService.findByContentId(acc.content_id())
                        .orElseThrow(() -> new RuntimeException("숙소 없음: " + acc.content_id()));

                TravelPlanPlace accRow = TravelPlanPlace.builder()
                        .travelPlanDay(travelPlanDay)
                        .place(accPlace)
                        .sequence(acc.sequence())
                        .startTime(acc.start_time())
                        .endTime(acc.end_time())
                        .build();

                placesForThisDay.add(accRow);
            }

            // Day에 장소들 세팅
            travelPlanDay.changeTravelPlanPlaces(placesForThisDay);

            travelPlanDays.add(travelPlanDay);
            dayByDate.put(daySchedule.date(), travelPlanDay);
        }

        // 3) routes
        for (int i = 0; i < tempTravelPlanResponse.routes().size(); i++) {
            TempTravelPlanResponse.RouteInfo routeInfo = tempTravelPlanResponse.routes().get(i);

            RouteType routeType;
            try {
                routeType = RouteType.valueOf(routeInfo.route_type().toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                throw new IllegalArgumentException("알 수 없는 route_type: " + routeInfo.route_type());
            }

            for (Map.Entry<LocalDate, List<TempTravelPlanResponse.RouteDetail>> entry
                    : routeInfo.daily_route().entrySet()) {

                LocalDate date = entry.getKey();
                TravelPlanDay day = dayByDate.get(date);
                if (day == null) {
                    throw new IllegalStateException("해당 날짜의 Day 없음: " + date);
                }

                List<TempTravelPlanResponse.RouteDetail> details = entry.getValue();
                for (int k = 0; k < details.size(); k++) {
                    TempTravelPlanResponse.RouteDetail rd = details.get(k);

                    Place origin = placeService.findByContentId(rd.origin())
                            .orElseThrow(() -> new RuntimeException("출발지 없음: " + rd.origin()));
                    Place dest = placeService.findByContentId(rd.destination())
                            .orElseThrow(() -> new RuntimeException("도착지 없음: " + rd.destination()));

                    TravelPlanRoute tr = TravelPlanRoute.builder()
                            .travelPlanDay(day)   // FK(자식 -> 부모)
                            .origin(origin)
                            .destination(dest)
                            .sequence(rd.sequence())
                            .routeType(routeType)  // 엔티티 필드명이 routeType인 경우
                            .duration(rd.duration())
                            .build();

                    // 해당 Day 컬렉션에 바로 add
                    day.getTravelPlanRoutes().add(tr);
                }
            }
        }

        // 4) plan에 days 연결 (plan -> day 컬렉션 교체)
        plan.changeTravelPlanDays(travelPlanDays);

        // 5) 저장 (cascade로 하위까지 INSERT)
        travelPlanRepository.save(plan);

        // 6) 임시 키 삭제
        redisTemplate.delete(List.of(
                RedisKeyUtils.slugKey(uuid),
                RedisKeyUtils.dateKey(uuid),
                RedisKeyUtils.timeKey(uuid),
                RedisKeyUtils.placesKey(uuid),
                RedisKeyUtils.accommodationsKey(uuid),
                RedisKeyUtils.routeKey(uuid),
                RedisKeyUtils.recommendKey(uuid),
                RedisKeyUtils.tempScheduleKey(uuid)
        ));
    }

    public TravelPlanResponse getTravelPlan(Member member) {
        Member findMember = memberService.findByMemberId(member.getId())
                .orElseThrow(() -> new RuntimeException("등록되지 않은 회원"));

        TravelPlan travelPlan = travelPlanRepository.findByMember(findMember)
                .orElseThrow(() -> new RuntimeException("여행 일정 없음"));

        List<TravelPlanResponse.DaySchedule> days = new ArrayList<>();
        List<TravelPlanResponse.RouteInfo> routes = new ArrayList<>();

        List<TravelPlanDay> travelPlanDays = travelPlan.getTravelPlanDays();

        for (TravelPlanDay travelPlanDay : travelPlanDays) {
            // ---------- 장소(Place) 구성 ----------
            List<TravelPlanResponse.PlaceItem> placeItems = new ArrayList<>();
            TravelPlanResponse.AccommodationItem accommodationItem = null;

            // sequence 기준 정렬
            List<TravelPlanPlace> dayPlaces = new ArrayList<>(travelPlanDay.getTravelPlanPlaces());
            dayPlaces.sort(Comparator.comparingInt(TravelPlanPlace::getSequence));

            for (TravelPlanPlace tpp : dayPlaces) {
                // 숙소는 accommodation 필드로만 보내고, places에는 넣지 않음(중복 방지)
                boolean isAccommodation = PlaceType.ACCOMMODATION.getCode()
                        .equals(tpp.getPlace().getPlaceType());

                if (isAccommodation && accommodationItem == null) {
                    accommodationItem = new TravelPlanResponse.AccommodationItem(
                            tpp.getPlace().getContentId(),
                            tpp.getPlace().getPlaceType(),
                            tpp.getPlace().getTitle(),
                            tpp.getPlace().getImage(),
                            tpp.getSequence(),
                            tpp.getStartTime(),
                            tpp.getEndTime()
                    );
                } else {
                    placeItems.add(new TravelPlanResponse.PlaceItem(
                            tpp.getPlace().getContentId(),
                            tpp.getPlace().getPlaceType(),
                            tpp.getPlace().getTitle(),
                            tpp.getPlace().getImage(),
                            tpp.getSequence(),
                            tpp.getStartTime(),
                            tpp.getEndTime()
                    ));
                }
            }

            // ---------- 경로(Route) 구성 ----------
            Map<LocalDate, List<TravelPlanResponse.RouteDetail>> dailyRoutes = new HashMap<>();
            List<TravelPlanResponse.RouteDetail> routeDetails = new ArrayList<>();

            List<TravelPlanRoute> dayRoutes = new ArrayList<>(travelPlanDay.getTravelPlanRoutes());
            dayRoutes.sort(Comparator.comparingInt(TravelPlanRoute::getSequence));

            for (TravelPlanRoute tpr : dayRoutes) {
                routeDetails.add(new TravelPlanResponse.RouteDetail(
                        tpr.getSequence(),
                        tpr.getOrigin().getContentId(),
                        tpr.getDestination().getContentId(),
                        tpr.getDuration()
                ));
            }

            if (!routeDetails.isEmpty()) {
                dailyRoutes.put(travelPlanDay.getDate(), routeDetails);
            }

            String routeType = String.valueOf(dayRoutes.getFirst().getRouteType());

            // ---------- DaySchedule / RouteInfo 생성 ----------
            TravelPlanResponse.DaySchedule daySchedule = new TravelPlanResponse.DaySchedule(
                    travelPlanDay.getDate(),
                    travelPlanDay.getStartTime(),
                    travelPlanDay.getEndTime(),
                    placeItems,
                    accommodationItem
            );
            days.add(daySchedule);

            TravelPlanResponse.RouteInfo routeInfo = new TravelPlanResponse.RouteInfo(
                    routeType,
                    dailyRoutes
            );
            routes.add(routeInfo);
        }

        return new TravelPlanResponse(
                travelPlan.getStartDate(),
                travelPlan.getEndDate(),
                days,
                routes,
                travelPlan.getCreatedAt()
        );
    }


    public TravelPlanResponse findOne(String travelPlanId, Member member) {

        // 1) 본인 소유 일정만 조회
        TravelPlan travelPlan = travelPlanRepository
                .findOneByIdAndMember(Long.valueOf(travelPlanId), member.getId())
                .orElseThrow(() -> new RuntimeException("여행 일정 없음"));

        List<TravelPlanResponse.DaySchedule> days = new ArrayList<>();
        List<TravelPlanResponse.RouteInfo> routes = new ArrayList<>();

        // 2) Day 단위로 장소/숙소/경로 가공
        for (TravelPlanDay travelPlanDay : travelPlan.getTravelPlanDays()) {

            // --- Places / Accommodation ---
            List<TravelPlanResponse.PlaceItem> placeItems = new ArrayList<>();
            TravelPlanResponse.AccommodationItem accommodationItem = null;

            List<TravelPlanPlace> dayPlaces = new ArrayList<>(travelPlanDay.getTravelPlanPlaces());
            dayPlaces.sort(Comparator.comparingInt(TravelPlanPlace::getSequence));

            for (TravelPlanPlace tpp : dayPlaces) {
                boolean isAccommodation =
                        PlaceType.ACCOMMODATION.getCode().equals(tpp.getPlace().getPlaceType());

                if (isAccommodation && accommodationItem == null) {
                    accommodationItem = new TravelPlanResponse.AccommodationItem(
                            tpp.getPlace().getContentId(),
                            tpp.getPlace().getPlaceType(),
                            tpp.getPlace().getTitle(),
                            tpp.getPlace().getImage(),
                            tpp.getSequence(),
                            tpp.getStartTime(),
                            tpp.getEndTime()
                    );
                } else {
                    placeItems.add(new TravelPlanResponse.PlaceItem(
                            tpp.getPlace().getContentId(),
                            tpp.getPlace().getPlaceType(),
                            tpp.getPlace().getTitle(),
                            tpp.getPlace().getImage(),
                            tpp.getSequence(),
                            tpp.getStartTime(),
                            tpp.getEndTime()
                    ));
                }
            }

            // --- Routes ---
            Map<LocalDate, List<TravelPlanResponse.RouteDetail>> dailyRoutes = new HashMap<>();
            List<TravelPlanResponse.RouteDetail> routeDetails = new ArrayList<>();

            List<TravelPlanRoute> dayRoutes = new ArrayList<>(travelPlanDay.getTravelPlanRoutes());
            dayRoutes.sort(Comparator.comparingInt(TravelPlanRoute::getSequence));

            for (TravelPlanRoute tpr : dayRoutes) {
                routeDetails.add(new TravelPlanResponse.RouteDetail(
                        tpr.getSequence(),
                        tpr.getOrigin().getContentId(),
                        tpr.getDestination().getContentId(),
                        tpr.getDuration()
                ));
            }
            if (!routeDetails.isEmpty()) {
                dailyRoutes.put(travelPlanDay.getDate(), routeDetails);
            }

            String routeType = dayRoutes.isEmpty() ? null : String.valueOf(dayRoutes.get(0).getRouteType());

            // --- assemble ---
            days.add(new TravelPlanResponse.DaySchedule(
                    travelPlanDay.getDate(),
                    travelPlanDay.getStartTime(),
                    travelPlanDay.getEndTime(),
                    placeItems,
                    accommodationItem
            ));

            routes.add(new TravelPlanResponse.RouteInfo(
                    routeType,
                    dailyRoutes
            ));
        }

        // 3) 최종 응답
        return new TravelPlanResponse(
                travelPlan.getStartDate(),
                travelPlan.getEndDate(),
                days,
                routes,
                travelPlan.getCreatedAt()
        );
    }

}
