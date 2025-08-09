package com.wego.wego.domain.plan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "travel_plan_day")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class TravelPlanDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "travel_plan_day_id")
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_plan_id", nullable = false)
    @ToString.Exclude
    private TravelPlan travelPlan;

    @OneToMany(mappedBy = "travelPlanDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<TravelPlanPlace> travelPlanPlaces = new ArrayList<>();

    @OneToMany(mappedBy = "travelPlanDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<TravelPlanRoute> travelPlanRoutes = new ArrayList<>();

    public void changeTravelPlanPlaces(List<TravelPlanPlace> travelPlanPlaces) {
        this.travelPlanPlaces = travelPlanPlaces;
    }

    public void changeTravelTravelPlanRoutes(List<TravelPlanRoute> travelPlanRoutes) {
        this.travelPlanRoutes = travelPlanRoutes;
    }
}
