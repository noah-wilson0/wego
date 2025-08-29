package com.wego.wego.domain.plan.entity;

import com.wego.wego.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import javax.xml.stream.Location;
import java.time.LocalDate;

@Table(name = "travel_plan_share")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
public class TravelPlanShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "travel_plan_share_id ")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_plan_id",nullable = false)
    private TravelPlan travelPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_member_id ", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDate expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate createdAt=LocalDate.now();


}
