package com.wego.wego.domain.plan.controller.draft.auto;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.dto.DraftPlanResponse;
import com.wego.wego.domain.plan.service.TravelPlanAiMatchingService;
import com.wego.wego.domain.plan.service.draft.auto.DraftPlanLangGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/draft-plans")
@RequiredArgsConstructor
public class DraftPlanAutoController {
//    private final TravelPlanAiMatchingService travelPlanAiMatchingService;
    private final DraftPlanLangGraphService draftPlanLangGraphService;
    @PostMapping("/{uuid}/auto-schedule")
    public ResponseEntity<?> AutoTempTravelPlan(@PathVariable String uuid,
                                                @AuthenticationPrincipal Member member) {
//        travelPlanAiMatchingService.autoTempTravelPlan(uuid); //레거시
        DraftPlanResponse autoDraftPlan = draftPlanLangGraphService.createAutoDraftPlan(uuid, member);
        return ResponseEntity.ok().body(autoDraftPlan);
    }
}
