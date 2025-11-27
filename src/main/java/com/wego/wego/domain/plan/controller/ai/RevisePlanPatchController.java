package com.wego.wego.domain.plan.controller.ai;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.dto.edit.TravelPlanEditGeminiResponse;
import com.wego.wego.domain.plan.service.ai.AiRevisePlanPatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/travel-plans/{travelPlanId}/revise") // /travel-plans/{travelPlanId}/revise
@RequiredArgsConstructor
public class RevisePlanPatchController {
    private final AiRevisePlanPatchService aiRevisePlanPatchService;

//    @PostMapping("/days/{date}/places")
//    public ResponseEntity<?> addPlace(@AuthenticationPrincipal Member member,
//                                      @PathVariable("travelPlanId") String travelPlanId,
//                                      @PathVariable("date") String date) {
//
//        return ResponseEntity.ok().build();
//    }

//    @PostMapping("/days/{date}/places")
//    public ResponseEntity<?> movePlace(@AuthenticationPrincipal Member member,
//                                      @PathVariable("travelPlanId") String travelPlanId,
//                                      @PathVariable("date") String date) {
//
//        return ResponseEntity.ok().build();
//    }
    @PostMapping("/days/{date}/places")
    public ResponseEntity<?> changePlace(@AuthenticationPrincipal Member member,
                                       @PathVariable("travelPlanId") String travelPlanId,
                                       @PathVariable("date") String date, @RequestBody TravelPlanEditGeminiResponse travelPlanEditGeminiResponse) {
        aiRevisePlanPatchService.revisePlanChangePlacePatch(member.getId(),travelPlanId, date, travelPlanEditGeminiResponse);
        return ResponseEntity.ok().build();
    }
//    @DeleteMapping("/days/{date}/places")
//    public ResponseEntity<?> deletePlace(@AuthenticationPrincipal Member member,
//                                         @PathVariable("travelPlanId") String travelPlanId,
//                                         @PathVariable("date") String date) {
//
//        return ResponseEntity.ok().build();
//    }

}
