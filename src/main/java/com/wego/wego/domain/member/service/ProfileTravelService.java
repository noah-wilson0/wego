package com.wego.wego.domain.member.service;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.repository.TravelPlanRepository;
import com.wego.wego.domain.member.dto.TravelPlanSimpleResponse;
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
public class ProfileTravelService {
    private final TravelPlanRepository travelPlanRepository;




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
