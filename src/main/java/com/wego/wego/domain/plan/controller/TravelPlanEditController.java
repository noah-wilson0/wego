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
    public ResponseEntity<?> saveEditTravelPlan(@PathVariable Long travelPlanId) {
        travelPlanEditService.updateTravelPlan(travelPlanId);
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
         travelPlanEditService.deleteTravelPlan(travelPlanId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/edit")
    public ResponseEntity<TravelPlanNormalizeResponse> travelPlan(@PathVariable Long travelPlanId) {
        TravelPlanNormalizeResponse travelPlanNormalizeResponse = travelPlanEditService.getNormalizeTravelPlan(travelPlanId);

        log.info(travelPlanNormalizeResponse.toString());
        return ResponseEntity.ok(travelPlanNormalizeResponse);
    }

    @PostMapping("/edit/days/{date}")
    public ResponseEntity<TravelPlanNormalizeResponse> editInsert(@PathVariable Long travelPlanId,
                                  @PathVariable LocalDate date,
                                  @RequestBody EditTravelPlanPlaceInsertRequest editTravelPlanPlaceInsertRequest) {
        TravelPlanNormalizeResponse travelPlanNormalizeResponse = travelPlanEditService.editInsert(travelPlanId, date, editTravelPlanPlaceInsertRequest);
        return ResponseEntity.ok(travelPlanNormalizeResponse);
    }

    @PatchMapping("/edit/days/{date}")
    public ResponseEntity<TravelPlanNormalizeResponse> editMove(@PathVariable Long travelPlanId,
                                  @PathVariable LocalDate date,
                                  @RequestBody EditTravelPlanPlaceMoveRequest editTravelPlanPlaceMoveRequest) {
        TravelPlanNormalizeResponse travelPlanNormalizeResponse = travelPlanEditService.editMove(travelPlanId, date, editTravelPlanPlaceMoveRequest);
        return ResponseEntity.ok(travelPlanNormalizeResponse);
    }

    @DeleteMapping("/edit/days/{date}")
    public ResponseEntity<TravelPlanNormalizeResponse> editDelete(@PathVariable Long travelPlanId,
                                  @PathVariable LocalDate date,
                                  @RequestBody EditTravelPlanPlaceDeleteRequest editTravelPlanPlaceDeleteRequest) {
        TravelPlanNormalizeResponse travelPlanNormalizeResponse = travelPlanEditService.editDelete(travelPlanId, date, editTravelPlanPlaceDeleteRequest);

        return ResponseEntity.ok(travelPlanNormalizeResponse);
    }


    @PatchMapping("/edit/days/{date}/time")
    public ResponseEntity<TravelPlanNormalizeResponse> updateStartTime(@PathVariable Long travelPlanId,
                                                                  @PathVariable LocalDate date,
                                                                  @RequestBody EditTravelPlanDayTimeRequest editTravelPlanDayTimeRequest) {
        TravelPlanNormalizeResponse travelPlanNormalizeResponse = travelPlanEditService.changeTravelPlanDayTime(travelPlanId, date, editTravelPlanDayTimeRequest);

        return ResponseEntity.ok(travelPlanNormalizeResponse);
    }

}
