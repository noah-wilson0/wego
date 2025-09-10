package com.wego.wego.domain.settlement.entity;

import com.wego.wego.domain.plan.entity.TravelPlan;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "settlement")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_plan_id")
    private TravelPlan travelPlan;

    @Builder.Default
    @Column(name = "total_paid", nullable = false)
    private int totalPaid=0;

    @Column(name = "budget", nullable = false)
    private int budget;

    @Builder.Default
    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementItem> settlementItems = new ArrayList<>();


    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    List<SettlementResult> settlementResults = new ArrayList<>();

    public Settlement changeTotalPaid(int totalPaid) {
        this.totalPaid = totalPaid;

        return this;
    }
    public int changeBudget(int budget) {
        this.budget = budget;

        return this.budget;
    }
}
