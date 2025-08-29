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
@RequestMapping("/travel_plan")
@RequiredArgsConstructor
public class TravelPlanController {
    private final TravelPlanService travelPlanService;


    @PostMapping("/slug/{uuid}")
    public void saveSlug(@PathVariable String uuid, @RequestBody String slug) {
        travelPlanService.saveTempSlug(slug, uuid);
    }

    /**
     * 단순한 임시 여행 일정 조회
     * @param uuid
     * @return
     */
    @GetMapping("/temp/schedule/{uuid}")
    public ResponseEntity<String> getTempSchedule(@PathVariable String uuid) {
        String tempTravelPlan = travelPlanService.getTempTravelPlan(uuid);
        return ResponseEntity.ok().body(tempTravelPlan);

    }


    /**
     *
     * 1. 회원 임시 여행일정 json을 영속화(persist db)
     *
     * 회원/비회원이든 redis에서 읽어서 저장하므로 permitAll하다. > 틀렸음 저장은 회원만 되므로 회원만 가능함
     */
    @PostMapping("/schedule/{uuid}")
    public ResponseEntity<Void> saveUserTravelPlan(@PathVariable String uuid, @AuthenticationPrincipal Member member) {
        log.info("saveUserTravelPlan.member: {}", member);
        travelPlanService.persistTravelPlan(uuid, member);

        return ResponseEntity.ok().build();
    }

    /**
     *  * 2. 애초에 영속된 여행일정을 편집 후 저장할 때 (update db)
     * @return
     */
    @PatchMapping("/{travel_plan_id}/schedule")
    public ResponseEntity<Void> updateTravelPlan() {

        return ResponseEntity.ok().build();
    }
    /**
     * 회원 일정 조회(from DB) 마이 페이지 메인
     *
     * @return
     */
    @GetMapping("/member/schedule")
    public ResponseEntity<TravelPlanResponse> getSchedule(@AuthenticationPrincipal Member member) {
        TravelPlanResponse travelPlan = travelPlanService.getTravelPlan(member);
        return ResponseEntity.ok().body(travelPlan);
    }

    /**
     * 여행 일정 단일 상세 조회
     */
    @GetMapping("/member/schedule/{travelPlanId}")
    public ResponseEntity<?> getTravelPlanScheduleFindOne(@PathVariable String travelPlanId, @AuthenticationPrincipal Member member) {
        TravelPlanResponse travelPlanResponse = travelPlanService.findOne(travelPlanId, member);

        return ResponseEntity.ok().body(travelPlanResponse);
    }

}
