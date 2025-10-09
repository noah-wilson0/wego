package com.wego.wego.domain.plan.dto.draft.auto;

public record DraftPlanCorrectionRequest(

        DraftPlanGeminiResponse draftPlanGeminiResponse,
        CorrectionPlace correctionPlace //장소/숙소 혼합 매핑

) {
    public record CorrectionPlace(
            String title,
            String addr,
            String tel
    ){}

}
