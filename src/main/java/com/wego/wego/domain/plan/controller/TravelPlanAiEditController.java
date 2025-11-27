package com.wego.wego.domain.plan.controller;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.dto.edit.AutoEditGenerateRequest;
import com.wego.wego.domain.plan.dto.edit.TravelPlanNormalizeResponse;
import com.wego.wego.domain.plan.service.edit.TravelPlanLangGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/travel-plans/{travelPlanId}/revise/") // /travel-plans/{travelPlanId}/edit/ai
@RequiredArgsConstructor
public class TravelPlanAiEditController {

    private final TravelPlanLangGraphService travelPlanLangGraphService;
    @PostMapping("/generate")
    public ResponseEntity<?> autoEditTravelPlan(
            @AuthenticationPrincipal Member member,
            @PathVariable Long travelPlanId,
            @RequestBody AutoEditGenerateRequest autoEditGenerateRequest) {

        TravelPlanNormalizeResponse aiEditTravelPlan = travelPlanLangGraphService.getAiEditTravelPlan(member.getId(), travelPlanId, autoEditGenerateRequest.prompt());


        return ResponseEntity.ok(aiEditTravelPlan);
    }
}
