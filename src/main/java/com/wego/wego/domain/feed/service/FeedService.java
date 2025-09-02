package com.wego.wego.domain.feed.service;

import com.wego.wego.domain.chemi.entity.Chemi;
import com.wego.wego.domain.chemi.repository.ChemiRepository;
import com.wego.wego.domain.feed.dto.FeedCreateRequest;
import com.wego.wego.domain.feed.dto.FeedInitResponse;
import com.wego.wego.domain.feed.dto.FeedResponse;
import com.wego.wego.domain.feed.entity.*;
import com.wego.wego.domain.feed.repository.FeedRepository;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.entity.TravelPlanDay;
import com.wego.wego.domain.plan.entity.TravelPlanPlace;
import com.wego.wego.domain.plan.entity.TravelPlanRoute;
import com.wego.wego.domain.plan.repository.TravelPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class FeedService {
    private final TravelPlanRepository travelPlanRepository;
    private final FeedRepository feedRepository;
    private final ChemiRepository chemiRepository;

    public FeedInitResponse feedInit(Member member, String travelPlanId) {
        TravelPlan travelPlan = travelPlanRepository.findOneByIdAndMember(Long.valueOf(travelPlanId), member.getId())
                .orElseThrow(() -> {
                    throw new RuntimeException();
                });

        return new FeedInitResponse(travelPlan.getSlug(), travelPlan.getStartDate(), travelPlan.getEndDate());
    }

    @Transactional
    public void saveFeed(Member member, FeedCreateRequest feedCreateRequest, MultipartFile coverImage) {
        // TODO coverImage는 나중에 구현

        TravelPlan travelPlan = travelPlanRepository.findByMemberAndId(member, feedCreateRequest.travelPlanId())
                .orElseThrow(() -> new RuntimeException("여행 일정 찾을 수 없음"));

        Feed feed = Feed.builder()
                .member(member)
                .title(feedCreateRequest.title())
                .people(feedCreateRequest.people())
                .body(feedCreateRequest.description())
                .build();

        FeedTravelPlan feedTravelPlan = FeedTravelPlan.builder()
                .slug(travelPlan.getSlug())
                .startDate(travelPlan.getStartDate())
                .endDate(travelPlan.getEndDate())
                .build();

        feed.changeFeedTravelPlan(feedTravelPlan);

        for (TravelPlanDay travelPlanDay: travelPlan.getTravelPlanDays()) {
            FeedTravelPlanDay feedTravelPlanDay = FeedTravelPlanDay.builder()
                    .date(travelPlanDay.getDate())
                    .startTime(travelPlanDay.getStartTime())
                    .endTime(travelPlanDay.getEndTime())
                    .build();

            for (TravelPlanPlace travelPlanPlace: travelPlanDay.getTravelPlanPlaces()) {
                FeedTravelPlanPlace feedTravelPlanPlace = FeedTravelPlanPlace.builder()
                        .place(travelPlanPlace.getPlace())
                        .sequence(travelPlanPlace.getSequence())
                        .startTime(travelPlanPlace.getStartTime())
                        .endTime(travelPlanPlace.getEndTime())
                        .build();
                feedTravelPlanDay.addPlace(feedTravelPlanPlace);
            }

            for (TravelPlanRoute r : travelPlanDay.getTravelPlanRoutes()) {
                FeedTravelPlanRoute feedTravelPlanRoute = FeedTravelPlanRoute.builder()
                        .origin(r.getOrigin())
                        .destination(r.getDestination())
                        .sequence(r.getSequence())
                        .routeType(r.getRouteType())
                        .duration(r.getDuration())
                        .build();
                feedTravelPlanDay.addRoute(feedTravelPlanRoute);                        // 내부에서 fr.changeFeedTravelPlanDay(fday)
            }

            feedTravelPlan.addTravelPlanDay(feedTravelPlanDay);
        }

        List<Chemi> chemiList = chemiRepository.findAllById(feedCreateRequest.chemiIds());

        chemiList.stream().forEach(feed::addChemi);

        feedRepository.save(feed);

    }


    public Page<FeedResponse> getAll(Pageable pageable) {
        return feedRepository.findAll(pageable)   // Page<Feed>
                .map(FeedResponse::from); // Page<FeedResponse>
    }


    public FeedResponse getFeed(Member member, String feedId) {
        Feed feed = feedRepository.findByIdAndMember(Long.valueOf(feedId), member)
                .orElseThrow(() -> new RuntimeException("피드 찾을 수 없음"));
        return FeedResponse.from(feed);
    }
}
