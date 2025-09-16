package com.wego.wego.domain.plan.controller;

import com.wego.wego.domain.plan.service.TravelPlanAiMatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/draft-plans")
@RequiredArgsConstructor
public class DraftPlanAutoController {
    private final TravelPlanAiMatchingService travelPlanAiMatchingService;

    @PostMapping("/{uuid}/auto-schedule")
    public ResponseEntity<?> AutoTempTravelPlan(@PathVariable String uuid) {
        travelPlanAiMatchingService.autoTempTravelPlan(uuid);
        return ResponseEntity.ok().body("성공");
    }
}
