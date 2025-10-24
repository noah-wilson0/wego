package com.wego.wego.domain.placeStorage.entity;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.external.tourapi.place.entity.Place;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "travel_plan_place_storage")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class PlaceStorage {

    @EmbeddedId
    private PlaceStorageId id;

    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @MapsId("travelPlanId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_plan_id")
    private TravelPlan travelPlan;

    @MapsId("placeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    private int sequence;

}
