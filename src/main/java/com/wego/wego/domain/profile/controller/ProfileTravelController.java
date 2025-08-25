package com.wego.wego.domain.profile.controller;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.profile.dto.TravelPlanSimpleResponse;
import com.wego.wego.domain.profile.service.ProfileTravelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileTravelController {
    private final ProfileTravelService profileTravelService;


    /**
     * 전체 여행 일정 반환
     * @param member status
     * @return
     */
    /**
     * TODO: 추후 공유된 일정 status도 추가
     */
    @GetMapping("/travel-plans/all")
    public ResponseEntity<List<TravelPlanSimpleResponse>> getTravelPlans(@RequestParam String status, @AuthenticationPrincipal Member member) {
        List<TravelPlanSimpleResponse> travelPlans = profileTravelService.getTravelPlans(member, status);
        log.info(travelPlans.toString());
        return ResponseEntity.ok(travelPlans);
    }

    /**
     * 마이페이지 - 단일 여행 일정 반환
     * @param member
     * @return
     */
    @GetMapping("/travel-plans")
    public ResponseEntity<TravelPlanSimpleResponse> getTravelPlan(@AuthenticationPrincipal Member member) {
        TravelPlanSimpleResponse travelPlan = profileTravelService.getTravelPlan(member);
        log.info("getTravelPlan:{}",travelPlan);
        return ResponseEntity.ok(travelPlan);
    }


}
