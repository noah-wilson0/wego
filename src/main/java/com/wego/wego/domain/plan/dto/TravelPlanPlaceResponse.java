package com.wego.wego.domain.plan.dto;

import lombok.Builder;

@Builder
public record TravelPlanPlaceResponse(
        String contentId,
        String title,
        String image,
        String placeType,
        String addr,
        Double averageRating,
        Long likeCount
){}
