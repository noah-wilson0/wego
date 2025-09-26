package com.wego.wego.domain.plan.dto.draft.auto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class GeminiPlaceItemResponse {
    private String contentId;
    private String title;
    private String addr;
    private String tel;
    private double latitude;
    private double longitude;

    @JsonIgnore // 외부 응답에 숨기고 싶으면
    private Double averageRating;

    @JsonIgnore
    private Long likeCount;

    @QueryProjection
    public GeminiPlaceItemResponse(
            String contentId,
            String title,
            String addr,
            String tel,
            double latitude,
            double longitude,
            Double averageRating,
            Long likeCount
    ) {
        this.contentId = contentId;
        this.title = title;
        this.addr = addr;
        this.tel = tel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.averageRating = averageRating;
        this.likeCount = likeCount;
    }
}
