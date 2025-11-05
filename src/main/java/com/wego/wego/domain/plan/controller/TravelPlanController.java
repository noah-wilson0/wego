package com.wego.wego.domain.plan.controller;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.dto.TravelPlanResponse;
import com.wego.wego.domain.plan.service.TravelPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/travel-plans")
@RequiredArgsConstructor
public class TravelPlanController {
    private final TravelPlanService travelPlanService;

    /**
     *  * 2. 애초에 영속된 여행일정을 편집 후 저장할 때 (update db)
     * @return
     */
    @PatchMapping("/{uuid}")
    public ResponseEntity<Void> updateTravelPlan() {

        return ResponseEntity.ok().build();
    }
//    /**
//     * 회원 일정 조회(from DB) 왜 있지?
//     *
//     * @return
//     */
//    @GetMapping("/member/schedule")
//    public ResponseEntity<TravelPlanResponse> getSchedule(@AuthenticationPrincipal Member member) {
//        TravelPlanResponse travelPlan = travelPlanService.getTravelPlan(member);
//        return ResponseEntity.ok().body(travelPlan);
//    }

    /**
     * 회원 여행 일정 상세 조회
     */
    @GetMapping("/{travelPlanId}/me")
    public ResponseEntity<?> getTravelPlanScheduleFindOne(@PathVariable String travelPlanId, @AuthenticationPrincipal Member member) {
        TravelPlanResponse travelPlanResponse = travelPlanService.findOne(travelPlanId, member);

        return ResponseEntity.ok().body(travelPlanResponse);
    }




}
