package com.wego.wego.domain.member.controller;

import com.wego.wego.domain.member.dto.TravelPlanSimpleResponse;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.service.MeTravelPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequestMapping("/me")
@RestController
@RequiredArgsConstructor
public class MeTravelPlanController {
    private final MeTravelPlanService meTravelPlanService;

    /**
     * 전체 여행 일정 반환
     * @param member status
     * @return
     */
    /**
     * TODO: page적용하기
     */
    @GetMapping("/travel-plans")
    public ResponseEntity<List<TravelPlanSimpleResponse>> getTravelPlans(@RequestParam String status, @AuthenticationPrincipal Member member) {
        List<TravelPlanSimpleResponse> travelPlans = meTravelPlanService.getTravelPlans(member, status);
        log.info(travelPlans.toString());
        return ResponseEntity.ok(travelPlans);
    }

    /**
     * 마이페이지 - 단일 여행 일정 반환
     * @param member
     * @return
     */
    @GetMapping("/imminent")
    public ResponseEntity<TravelPlanSimpleResponse> getTravelPlan(@AuthenticationPrincipal Member member) {
        TravelPlanSimpleResponse travelPlan = meTravelPlanService.getTravelPlan(member);
        log.info("getTravelPlan:{}",travelPlan);
        return ResponseEntity.ok(travelPlan);
    }
}
