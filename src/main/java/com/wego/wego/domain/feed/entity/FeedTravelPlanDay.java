package com.wego.wego.domain.feed.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "feed_travel_plan_day")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class FeedTravelPlanDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_travel_plan_day_id")
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_travel_plan_id", nullable = false)
    @ToString.Exclude
    private FeedTravelPlan feedTravelPlan;

    @OneToMany(mappedBy = "feedTravelPlanDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<FeedTravelPlanPlace> travelPlanPlaces = new ArrayList<>();

    @OneToMany(mappedBy = "feedTravelPlanDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<FeedTravelPlanRoute> travelPlanRoutes = new ArrayList<>();

    public void changeFeedTravelPlan(FeedTravelPlan feedTravelPlan) {
        this.feedTravelPlan = feedTravelPlan;
    }

    public void addPlace(FeedTravelPlanPlace feedTravelPlanPlace) {
        this.travelPlanPlaces.add(feedTravelPlanPlace);
        feedTravelPlanPlace.changeFeedTravelPlanDay(this);
    }
    public void addRoute(FeedTravelPlanRoute feedTravelPlanRoute) {
        this.travelPlanRoutes.add(feedTravelPlanRoute);
        feedTravelPlanRoute.changeFeedTravelPlanDay(this);
    }
}
