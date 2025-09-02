package com.wego.wego.domain.feed.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "feed_travel_plan")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class FeedTravelPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_travel_plan_id")
    private Long id;

    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt=LocalDateTime.now();

    @OneToMany(mappedBy = "feedTravelPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<FeedTravelPlanDay> feedTravelPlanDays = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false, unique = true)
    @ToString.Exclude
    private Feed feed;

    public void addTravelPlanDay(FeedTravelPlanDay feedTravelPlanDay) {
        this.feedTravelPlanDays.add(feedTravelPlanDay);
        feedTravelPlanDay.changeFeedTravelPlan(this);
    }

    public void changeFeed(Feed feed) {
        this.feed = feed;
    }

}
