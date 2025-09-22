package com.wego.wego.domain.plan.controller.draft;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.dto.DraftPlanMetaResponse;
import com.wego.wego.domain.plan.dto.DraftTravelPlanResponse;
import com.wego.wego.domain.plan.service.draft.DraftPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/draft-plans")
@RequiredArgsConstructor
public class DraftPlanController {

    private final DraftPlanService draftPlanService;

    @PostMapping("/slug/{uuid}")
    public void saveSlug(@PathVariable String uuid, @RequestBody String slug) {
        draftPlanService.saveTempSlug(slug, uuid);
    }

    @GetMapping("{uuid}/meta")
    public ResponseEntity<DraftPlanMetaResponse> getMeta(@PathVariable String uuid) {
        DraftPlanMetaResponse metaResponse = draftPlanService.getSlugAndDates(uuid);
        return ResponseEntity.ok(metaResponse);
    }


    /**
     * 비회원 임시 여행 일정 조회
     * @param uuid
     * @return
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<DraftTravelPlanResponse> getTempSchedule(@PathVariable String uuid) {
        log.info("/draft-plans/{}", uuid);
        DraftTravelPlanResponse tempTravelPlan = draftPlanService.getTempTravelPlan(uuid);
        return ResponseEntity.ok().body(tempTravelPlan);

    }

    /**
     *
     * 1. 회원 임시 여행일정 json을 영속화(persist db)
     *
     * 회원/비회원이든 redis에서 읽어서 저장하므로 permitAll하다. > 틀렸음 저장은 회원만 되므로 회원만 가능함
     */
    @PostMapping("/{uuid}")
    public ResponseEntity<Void> saveUserTravelPlan(@PathVariable String uuid, @AuthenticationPrincipal Member member) {
        log.info("saveUserTravelPlan.member: {}", member);
        draftPlanService.persistTravelPlan(uuid, member);

        return ResponseEntity.ok().build();
    }


}
