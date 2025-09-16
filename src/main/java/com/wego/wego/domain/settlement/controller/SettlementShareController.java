package com.wego.wego.domain.settlement.controller;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.settlement.dto.SettlementItemRequest;
import com.wego.wego.domain.settlement.dto.SettlementResponse;
import com.wego.wego.domain.settlement.dto.SettlementResultResponse;
import com.wego.wego.domain.settlement.service.SettlementShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/travel-share-plans/{token}/settlements")
@RequiredArgsConstructor
public class SettlementShareController {
    private final SettlementShareService settlementShareService;

    /**
     * 예산 및 사용 내역 조회
     * @param token
     * @return
     */
    @GetMapping
    public ResponseEntity<SettlementResponse> getTravelPlanSettlements(@PathVariable String token) {
        SettlementResponse settlementResponse = settlementShareService.getAll(token);

        return ResponseEntity.ok(settlementResponse);
    }
    /**
     * 정산하기 예산 생성
     * @param token
     * @param budget
     * @return
     */

    @PostMapping
    public ResponseEntity<Integer> createTravelPlanBudget(@PathVariable String token, @RequestBody int budget) {
        int response = settlementShareService.createBudget(token, budget);
        return ResponseEntity.ok(response);
    }

    /**
     * 정산하기 예산 수정
     * @param token
     * @param budget
     * @return
     */
    @PatchMapping
    public ResponseEntity<Integer> updateTravelPlanBudget(@PathVariable String token, @RequestBody int budget) {
        int response = settlementShareService.updateBudget(token, budget);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원 사용 내역 저장
     * @param token
     * @param settlementItemRequest
     * @param member
     * @return
     */
    @PostMapping("/items")
    public ResponseEntity<String> createSettlementItem(@PathVariable String token, @RequestBody SettlementItemRequest settlementItemRequest, @AuthenticationPrincipal Member member) {
        settlementShareService.saveSettlementItem(token, settlementItemRequest, member);
        return ResponseEntity.ok().build();
    }

    /**
     * 정산하기
     * @return
     */

    @PostMapping("/result")
    public ResponseEntity<SettlementResultResponse> createSettlementResult(@PathVariable String token) {
        SettlementResultResponse settlementResultResponse = settlementShareService.createSettlementResult(token);
        return ResponseEntity.ok(settlementResultResponse);
    }

    /**
     * 정산 내역 조회
     * @param token
     * @return
     */
    @GetMapping("/result")
    public ResponseEntity<SettlementResultResponse> getSettlementResult(@PathVariable String token) {
        SettlementResultResponse settlementResultResponse = settlementShareService.getSettlementResult(token);
        return ResponseEntity.ok(settlementResultResponse);
    }
}
