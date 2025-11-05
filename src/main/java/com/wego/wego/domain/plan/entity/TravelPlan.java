package com.wego.wego.domain.plan.entity;

import com.wego.wego.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Table(name = "travel_plan")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class TravelPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "travel_plan_id")
    private Long id;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "slug",nullable = false)
//    private AreaSlug areaSlug;
    /** 조회 전용 FK(DB: REFERENCES area_slug(slug)) */
    @Column(name = "slug", nullable = false)
    private String slug;

    @Column(name = "title", nullable = false)
    @Builder.Default
    private String title="";

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDate createdAt=LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @ToString.Exclude
    private Member member;

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<TravelPlanDay> travelPlanDays = new ArrayList<>();


    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<TravelPlanShare> travelPlanShares = new ArrayList<>();

    public void changeTravelPlanDays(List<TravelPlanDay> travelPlanDays) {
        this.travelPlanDays = travelPlanDays;
    }

    public void addTravelPlanDay(TravelPlanDay newDay) {
        newDay.belongToTravelPlan(this);
        this.travelPlanDays.add(newDay);
    }
}
