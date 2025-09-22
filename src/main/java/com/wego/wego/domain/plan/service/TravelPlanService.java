package com.wego.wego.domain.plan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.service.MemberService;
import com.wego.wego.domain.plan.dto.TravelPlanResponse;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.entity.TravelPlanDay;
import com.wego.wego.domain.plan.entity.TravelPlanPlace;
import com.wego.wego.domain.plan.entity.TravelPlanRoute;
import com.wego.wego.domain.plan.repository.TravelPlanRepository;
import com.wego.wego.domain.plan.service.support.SlugResolver;
import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.external.tourapi.place.service.PlaceService;
import com.wego.wego.global.enums.PlaceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TravelPlanService {

    private final PlaceService placeService;
    private final TravelPlanRepository travelPlanRepository;
    private final SlugResolver slugResolver;
    private final MemberService memberService;

    private final RedisTemplate<String,String > redisTemplate;
    private final ObjectMapper objectMapper;

//    public TravelPlanResponse getTravelPlan(Member member) {
//        Member findMember = memberService.findByMemberId(member.getId())
//                .orElseThrow(() -> new RuntimeException("등록되지 않은 회원"));
//
//        TravelPlan travelPlan = travelPlanRepository.findByMember(findMember)
//                .orElseThrow(() -> new RuntimeException("여행 일정 없음"));
//
//        List<TravelPlanResponse.DaySchedule> days = new ArrayList<>();
//        List<TravelPlanResponse.RouteInfo> routes = new ArrayList<>();
//
//        List<TravelPlanDay> travelPlanDays = travelPlan.getTravelPlanDays();
//
//        for (TravelPlanDay travelPlanDay : travelPlanDays) {
//            // ---------- 장소(Place) 구성 ----------
//            List<TravelPlanResponse.PlaceItem> placeItems = new ArrayList<>();
//            TravelPlanResponse.AccommodationItem accommodationItem = null;
//
//            // sequence 기준 정렬
//            List<TravelPlanPlace> dayPlaces = new ArrayList<>(travelPlanDay.getTravelPlanPlaces());
//            dayPlaces.sort(Comparator.comparingInt(TravelPlanPlace::getSequence));
//
//            for (TravelPlanPlace tpp : dayPlaces) {
//                // 숙소는 accommodation 필드로만 보내고, places에는 넣지 않음(중복 방지)
//                boolean isAccommodation = PlaceType.ACCOMMODATION.getCode()
//                        .equals(tpp.getPlace().getPlaceType());
//
//                if (isAccommodation && accommodationItem == null) {
//                    accommodationItem = new TravelPlanResponse.AccommodationItem(
//                            tpp.getPlace().getContentId(),
//                            tpp.getPlace().getPlaceType(),
//                            tpp.getPlace().getTitle(),
//                            tpp.getPlace().getImage(),
//                            tpp.getSequence(),
//                            Double.valueOf(tpp.getPlace().getLongitude()),
//                            Double.valueOf(tpp.getPlace().getLatitude()),
//                            tpp.getStartTime(),
//                            tpp.getEndTime()
//                    );
//                } else {
//                    placeItems.add(new TravelPlanResponse.PlaceItem(
//                            tpp.getPlace().getContentId(),
//                            tpp.getPlace().getPlaceType(),
//                            tpp.getPlace().getTitle(),
//                            tpp.getPlace().getImage(),
//                            tpp.getSequence(),
//                            Double.valueOf(tpp.getPlace().getLongitude()),
//                            Double.valueOf(tpp.getPlace().getLatitude()),
//                            tpp.getStartTime(),
//                            tpp.getEndTime()
//                    ));
//                }
//            }
//
//            // ---------- 경로(Route) 구성 ----------
//            Map<LocalDate, List<TravelPlanResponse.RouteDetail>> dailyRoutes = new HashMap<>();
//            List<TravelPlanResponse.RouteDetail> routeDetails = new ArrayList<>();
//
//            List<TravelPlanRoute> dayRoutes = new ArrayList<>(travelPlanDay.getTravelPlanRoutes());
//            dayRoutes.sort(Comparator.comparingInt(TravelPlanRoute::getSequence));
//
//            for (TravelPlanRoute tpr : dayRoutes) {
//                routeDetails.add(new TravelPlanResponse.RouteDetail(
//                        tpr.getSequence(),
//                        tpr.getOrigin().getContentId(),
//                        tpr.getDestination().getContentId(),
//                        tpr.getDuration()
//                ));
//            }
//
//            if (!routeDetails.isEmpty()) {
//                dailyRoutes.put(travelPlanDay.getDate(), routeDetails);
//            }
//
//            String routeType = String.valueOf(dayRoutes.getFirst().getRouteType());
//
//            // ---------- DaySchedule / RouteInfo 생성 ----------
//            TravelPlanResponse.DaySchedule daySchedule = new TravelPlanResponse.DaySchedule(
//                    travelPlanDay.getDate(),
//                    travelPlanDay.getStartTime(),
//                    travelPlanDay.getEndTime(),
//                    placeItems,
//                    accommodationItem
//            );
//            days.add(daySchedule);
//
//            TravelPlanResponse.RouteInfo routeInfo = new TravelPlanResponse.RouteInfo(
//                    routeType,
//                    dailyRoutes
//            );
//            routes.add(routeInfo);
//        }
//
//        return new TravelPlanResponse(
//                travelPlan.getStartDate(),
//                travelPlan.getEndDate(),
//                days,
//                routes,
//                travelPlan.getCreatedAt()
//        );
//    }


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
                            Double.valueOf(tpp.getPlace().getLongitude()),
                            Double.valueOf(tpp.getPlace().getLatitude()),
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
                            Double.valueOf(tpp.getPlace().getLongitude()),
                            Double.valueOf(tpp.getPlace().getLatitude()),
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
                slugResolver.resolveLabel(travelPlan.getSlug()),
                travelPlan.getStartDate(),
                travelPlan.getEndDate(),
                days,
                routes,
                travelPlan.getCreatedAt()
        );
    }

}
