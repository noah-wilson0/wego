package com.wego.wego.domain.member.service;

import com.wego.wego.domain.member.dto.TravelPlanSimpleResponse;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.repository.TravelPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class MeTravelPlanService {
    private final TravelPlanRepository travelPlanRepository;

    public List<TravelPlanSimpleResponse> getTravelPlans(Member member, String status) {
        switch (status) {
            case "all": return getAllTravelPlans(member);
            case "expired": return getExpireTravelPlans(member);
            default: throw new RuntimeException("{%s}상태 사용 불가".formatted(status));
        }
    }
    public TravelPlanSimpleResponse getTravelPlan(Member member) {
        TravelPlan travelPlan = travelPlanRepository.findTravelPlanByMemberAndImminentDate(member.getId(), LocalDate.now())
                .orElseThrow(() -> new RuntimeException("여행 일정 없음"));
        log.info("travelPlan: {}", travelPlan.toString());
        return  new TravelPlanSimpleResponse(travelPlan.getId(), travelPlan.getSlug(), travelPlan.getTitle(), travelPlan.getStartDate(), travelPlan.getEndDate(), travelPlan.getCreatedAt());
    }

    public List<TravelPlanSimpleResponse> getAllTravelPlans(Member member) {
        List<TravelPlan> travelPlansByMember = travelPlanRepository.findTravelPlansByMember(member);

        return travelPlansByMember.stream()
                .map(tp -> new TravelPlanSimpleResponse(tp.getId(), tp.getSlug(), tp.getTitle(), tp.getStartDate(), tp.getEndDate(), tp.getCreatedAt()))
                .toList();
    }
    private List<TravelPlanSimpleResponse> getExpireTravelPlans(Member member) {
        List<TravelPlan> expireTravelPlans = travelPlanRepository.findTravelPlansByMemberAndExpireDate(member.getId(), LocalDate.now());
        return expireTravelPlans.stream()
                .map(tp -> new TravelPlanSimpleResponse(tp.getId(), tp.getSlug(), tp.getTitle(), tp.getStartDate(), tp.getEndDate(), tp.getCreatedAt()))
                .toList();
    }
}
