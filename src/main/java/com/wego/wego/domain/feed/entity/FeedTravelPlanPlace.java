package com.wego.wego.domain.feed.entity;

import com.wego.wego.external.tourapi.place.entity.Place;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Table(name = "feed_travel_plan_place")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class FeedTravelPlanPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_travel_plan_place_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_travel_plan_day_id" ,nullable = false)
    @ToString.Exclude
    private FeedTravelPlanDay feedTravelPlanDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable=false)
    @ToString.Exclude
    private Place place;

    @Column(nullable = false)
    private int sequence;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    public void changeFeedTravelPlanDay(FeedTravelPlanDay feedTravelPlanDay) {
        this.feedTravelPlanDay = feedTravelPlanDay;

    }


}
