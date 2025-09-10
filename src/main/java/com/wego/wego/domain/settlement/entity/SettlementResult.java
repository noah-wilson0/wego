package com.wego.wego.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "settlement_result")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class SettlementResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_result_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id")
    private Settlement settlement;

    @Builder.Default
    @OneToMany(mappedBy = "settlementResult", cascade = CascadeType.ALL, orphanRemoval = true)
    List<SettlementParticipant> settlementParticipants = new ArrayList<>();

    public void addParticipant(SettlementParticipant settlementParticipant) {
        settlementParticipants.add(settlementParticipant);
        settlementParticipant.setSettlementResult(this);
    }

}
