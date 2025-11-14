package com.wego.wego.domain.plan.dto.edit;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 편집 결과: 기존 여행 일정 중 일부 장소(slot)에 대한 '부분 수정 정보'를 리스트로 제공.
 * - changes: 수정된 항목(ChangeItem)들의 목록
 *   └ date: 수정이 발생한 날짜
 *   └ sequence: 수정된 슬롯 번호 (1..N)
 *   └ beforePlace: 수정 전 장소 정보
 *   └ afterPlace: 수정 후 장소 정보
 */
public record TravelPlanEditGeminiResponse(
        List<ChangeItem> changes
) {
    public record ChangeItem(
            LocalDate date,
            int sequence,
            PlacePatch beforePlace,
            PlacePatch afterPlace
    ) {}

    public record PlacePatch(
            String contentId,
            String title
    ) {}
}
