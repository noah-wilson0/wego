package com.wego.wego.domain.plan.entity;

import com.wego.wego.external.tourapi.place.entity.Place;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Table(name = "travel_plan_place")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class TravelPlanPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "travel_plan_place_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_plan_day_id" ,nullable = false)
    @ToString.Exclude
    private TravelPlanDay travelPlanDay;

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

    @Column
    @Builder.Default
    private String memo="";

    public void belongToTravelPlanDay(TravelPlanDay travelPlanDay) {
        this.travelPlanDay = travelPlanDay;
    }
}
