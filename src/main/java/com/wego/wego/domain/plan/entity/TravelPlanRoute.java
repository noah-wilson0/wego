package com.wego.wego.domain.plan.entity;

import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.global.enums.RouteType;
import jakarta.persistence.*;
import lombok.*;

@Table
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class TravelPlanRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "travel_plan_route_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_plan_day_id", nullable=false)
    @ToString.Exclude
    private TravelPlanDay travelPlanDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JoinColumn(name = "origin", nullable=false)
    private Place origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination")
    @ToString.Exclude
    private Place destination;

    @Column(nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type", nullable = false)
    private RouteType routeType;


    @Column(nullable = false)
    private int duration;


    public void belongToTravelPlanDay(TravelPlanDay travelPlanDay) {
        this.travelPlanDay = travelPlanDay;
    }
}
