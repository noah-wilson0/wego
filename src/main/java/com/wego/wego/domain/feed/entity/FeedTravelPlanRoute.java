package com.wego.wego.domain.feed.entity;

import com.wego.wego.external.tourapi.place.entity.Place;
import com.wego.wego.global.enums.RouteType;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "feed_travel_plan_route")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class FeedTravelPlanRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_travel_plan_route_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_travel_plan_day_id", nullable=false)
    @ToString.Exclude
    private FeedTravelPlanDay feedTravelPlanDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @JoinColumn(name = "origin", nullable=false)
    private Place origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination", nullable = false)
    @ToString.Exclude
    private Place destination;

    @Column(nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type", nullable = false)
    private RouteType routeType;

    @Column(nullable = false)
    private int duration;

    public void changeFeedTravelPlanDay(FeedTravelPlanDay feedTravelPlanDay) {
        this.feedTravelPlanDay = feedTravelPlanDay;
    }


}
