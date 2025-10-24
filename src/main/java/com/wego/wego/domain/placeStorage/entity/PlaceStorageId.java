package com.wego.wego.domain.placeStorage.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceStorageId {
    private Long memberId;
    private Long travelPlanId;
    private Long placeId;
}
