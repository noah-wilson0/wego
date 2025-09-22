package com.wego.wego.domain.plan.service;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.dto.TravelPlanResponse;
import com.wego.wego.domain.plan.entity.*;
import com.wego.wego.domain.plan.repository.TravelPlanRepository;
import com.wego.wego.domain.plan.repository.TravelPlanShareRepository;
import com.wego.wego.domain.plan.service.support.SlugResolver;
import com.wego.wego.domain.plan.util.ShareTokenUtil;
import com.wego.wego.external.tourapi.place.service.PlaceService;
import com.wego.wego.global.enums.PlaceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TravelPlanShareService {
    private final PlaceService placeService;
    private final SlugResolver slugResolver;
    private final TravelPlanRepository travelPlanRepository;
    private final TravelPlanShareRepository travelPlanShareRepository;

    @Transactional
    public String createShareToken(String travelPlanId, Member member) {
        TravelPlan travelPlan = travelPlanRepository.findById(Long.valueOf(travelPlanId))
                .orElseThrow(() -> new RuntimeException("Travel plan not found"));
        
        //1) 토큰 생성
        String plainToken  = ShareTokenUtil.newBase64UrlToken(24);

        //2) 토큰 암호화
        String tokenHash = ShareTokenUtil.sha256Hex(plainToken);

        LocalDate expiresAt = travelPlan.getEndDate().plusDays(1);

        travelPlanShareRepository.save(TravelPlanShare.builder()
                        .travelPlan(travelPlan)
                        .member(member)
                        .token(tokenHash)
                        .expiresAt(expiresAt)
                .build());
        return plainToken;
        
    }

    public TravelPlanResponse getShareTravelPlan(String plainToken) {
        String tokenHash = ShareTokenUtil.sha256Hex(plainToken);
        TravelPlan travelPlan = travelPlanShareRepository
                .findTravelPlanByToken(tokenHash, LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException("invalid or expired token"));
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
                slugResolver.resolveLabel(travelPlan.getSlug()),
                travelPlan.getStartDate(),
                travelPlan.getEndDate(),
                days,
                routes,
                travelPlan.getCreatedAt()
        );
    }
}
