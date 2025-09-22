package com.wego.wego.domain.plan.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
public class DraftPlanPlaceResponse {
    private String contentId;
    private String title;
    private String image;
    private String placeType;
    private String addr;
    private double longitude;
    private double latitude;
    private Double averageRating;
    private Long likeCount;

    @QueryProjection
    public DraftPlanPlaceResponse(String contentId, String title, String image,
                                  String placeType, String addr,
                                  double longitude, double latitude,
                                  Double averageRating, Long likeCount) {
        this.contentId = contentId;
        this.title = title;
        this.image = image;
        this.placeType = placeType;
        this.addr = addr;
        this.longitude = longitude;
        this.latitude = latitude;
        this.averageRating = averageRating;
        this.likeCount = likeCount;
    }
}
