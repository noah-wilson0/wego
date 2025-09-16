package com.wego.wego.domain.plan.controller;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.dto.TravelPlanResponse;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.service.TravelPlanShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * TODO security config 추가
 */
@Slf4j
@RestController
@RequestMapping("/travel-plans")
@RequiredArgsConstructor
public class TravelPlanShareController {
    private final TravelPlanShareService travelPlanShareService;

    @PostMapping("/{travelPlanId}/share")
    public ResponseEntity<String> createShareToken(@PathVariable String travelPlanId, @AuthenticationPrincipal Member member){
        String shareToken = travelPlanShareService.createShareToken(travelPlanId, member);
        return ResponseEntity.ok().body(shareToken);
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> getShareTravelPlan(@PathVariable String token){
        TravelPlan shareTravelPlan = travelPlanShareService.getShareTravelPlan(token);
        TravelPlanResponse travelPlanResponse = TravelPlanResponse.from(shareTravelPlan);
        return ResponseEntity.ok().body(travelPlanResponse);
    }

}
