package com.wego.wego.domain.plan.controller;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.dto.edit.*;
import com.wego.wego.domain.plan.service.edit.TravelPlanEditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/travel-plans/{travelPlanId}") ///travel-plans/{travelPlanId}/edit/days/{date}
@RequiredArgsConstructor
public class TravelPlanEditController {
    private final TravelPlanEditService travelPlanEditService;

    /** 저장
     * 편집된 여행 일정 저장(DB에 업데이트)
     * @param travelPlanId
     * @return
     */
    @PatchMapping("/me")
    public ResponseEntity<?> saveEditTravelPlan(@AuthenticationPrincipal Member member,
                                                @PathVariable Long travelPlanId) {
        travelPlanEditService.updateTravelPlan(member.getId(), travelPlanId);
        return ResponseEntity.ok().build();
    }

    /**
     * 취소 redis에 저장된 여행 일정 삭제
     * @param travelPlanId
     * @param member
     * @return
     */

    @DeleteMapping("/me")
    public ResponseEntity<?> getTravelPlanScheduleFindOne(@PathVariable String travelPlanId, @AuthenticationPrincipal Member member) {
         travelPlanEditService.deleteTravelPlan(member.getId(), travelPlanId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/edit")
    public ResponseEntity<TravelPlanNormalizeResponse> travelPlan(@AuthenticationPrincipal Member member,
                                                                  @PathVariable Long travelPlanId) {
        TravelPlanNormalizeResponse travelPlanNormalizeResponse = travelPlanEditService.getNormalizeTravelPlan(member.getId(),travelPlanId);

        log.info(travelPlanNormalizeResponse.toString());
        return ResponseEntity.ok(travelPlanNormalizeResponse);
    }

    @PostMapping("/edit/days/{date}")
    public ResponseEntity<TravelPlanNormalizeResponse> editInsert(@AuthenticationPrincipal Member member,
                                                                  @PathVariable Long travelPlanId,
                                  @PathVariable LocalDate date,
                                  @RequestBody EditTravelPlanPlaceInsertRequest editTravelPlanPlaceInsertRequest) {
        TravelPlanNormalizeResponse travelPlanNormalizeResponse = travelPlanEditService.editInsert(member.getId(), travelPlanId, date, editTravelPlanPlaceInsertRequest);
        return ResponseEntity.ok(travelPlanNormalizeResponse);
    }

    @PatchMapping("/edit/days/{date}")
    public ResponseEntity<TravelPlanNormalizeResponse> editMove(@AuthenticationPrincipal Member member,
                                                                @PathVariable Long travelPlanId,
                                  @PathVariable LocalDate date,
                                  @RequestBody EditTravelPlanPlaceMoveRequest editTravelPlanPlaceMoveRequest) {
        TravelPlanNormalizeResponse travelPlanNormalizeResponse = travelPlanEditService.editMove(member.getId(), travelPlanId, date, editTravelPlanPlaceMoveRequest);
        return ResponseEntity.ok(travelPlanNormalizeResponse);
    }

    @DeleteMapping("/edit/days/{date}")
    public ResponseEntity<TravelPlanNormalizeResponse> editDelete(@AuthenticationPrincipal Member member,
                                                                  @PathVariable Long travelPlanId,
                                  @PathVariable LocalDate date,
                                  @RequestBody EditTravelPlanPlaceDeleteRequest editTravelPlanPlaceDeleteRequest) {
        TravelPlanNormalizeResponse travelPlanNormalizeResponse = travelPlanEditService.editDelete(member.getId(), travelPlanId, date, editTravelPlanPlaceDeleteRequest);

        return ResponseEntity.ok(travelPlanNormalizeResponse);
    }


    @PatchMapping("/edit/days/{date}/time")
    public ResponseEntity<TravelPlanNormalizeResponse> updateStartTime(@AuthenticationPrincipal Member member,
                                                                    @PathVariable Long travelPlanId,
                                                                  @PathVariable LocalDate date,
                                                                  @RequestBody EditTravelPlanDayTimeRequest editTravelPlanDayTimeRequest) {
        TravelPlanNormalizeResponse travelPlanNormalizeResponse = travelPlanEditService.changeTravelPlanDayTime(member.getId(), travelPlanId, date, editTravelPlanDayTimeRequest);

        return ResponseEntity.ok(travelPlanNormalizeResponse);
    }

}
