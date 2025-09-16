package com.wego.wego.domain.settlement.controller;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.settlement.dto.SettlementItemRequest;
import com.wego.wego.domain.settlement.dto.SettlementResponse;
import com.wego.wego.domain.settlement.dto.SettlementResultResponse;
import com.wego.wego.domain.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/travel-plans/{travelPlanId}/settlements")
@RequiredArgsConstructor
public class SettlementController {
    private final SettlementService settlementService;

    /**
     * 예산 및 사용 내역 조회
     * @param travelPlanId
     * @return
     */
    @GetMapping
    public ResponseEntity<SettlementResponse> getTravelPlanSettlements(@PathVariable("travelPlanId") String travelPlanId) {
        SettlementResponse settlementResponse = settlementService.getAll(travelPlanId);

        return ResponseEntity.ok(settlementResponse);
    }

    /**
     * 정산하기 예산 생성
     * @param travelPlanId
     * @param budget
     * @return
     */

    @PostMapping
    public ResponseEntity<Integer> createTravelPlanBudget(@PathVariable("travelPlanId") String travelPlanId, @RequestBody int budget) {
        int response = settlementService.createBudget(travelPlanId, budget);
        return ResponseEntity.ok(response);
    }

    /**
     * 정산하기 예산 수정
     * @param travelPlanId
     * @param budget
     * @return
     */
    @PatchMapping
    public ResponseEntity<Integer> updateTravelPlanBudget(@PathVariable("travelPlanId") String travelPlanId, @RequestBody int budget) {
        int response = settlementService.updateBudget(travelPlanId, budget);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원 사용 내역 저장
     * @param travelPlanId
     * @param settlementItemRequest
     * @param member
     * @return
     */
    @PostMapping("/items")
    public ResponseEntity<String> createSettlementItem(@PathVariable("travelPlanId") String travelPlanId, @RequestBody SettlementItemRequest settlementItemRequest, @AuthenticationPrincipal Member member) {
        settlementService.saveSettlementItem(travelPlanId, settlementItemRequest, member);
        return ResponseEntity.ok().build();
    }

    /**
     * 정산하기
     * @return
     */
    @PostMapping("/result")
    public ResponseEntity<SettlementResultResponse> createSettlementResult(@PathVariable("travelPlanId") String travelPlanId) {
        SettlementResultResponse settlementResultResponse = settlementService.createSettlementResult(travelPlanId);
        return ResponseEntity.ok(settlementResultResponse);
    }

    /**
     * 정산 내역 조회
     * @param travelPlanId
     * @return
     */
    @GetMapping("/result")
    public ResponseEntity<SettlementResultResponse> getSettlementResult(@PathVariable("travelPlanId") String travelPlanId) {
        SettlementResultResponse settlementResultResponse = settlementService.getSettlementResult(travelPlanId);
        return ResponseEntity.ok(settlementResultResponse);
    }




}
