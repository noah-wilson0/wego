package com.wego.wego.domain.settlement.entity;

import com.wego.wego.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settlement_participant")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class SettlementParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_participant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_result_id")
    private SettlementResult settlementResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private Member member;

    @Column(name = "paid_total", nullable = false)
    private int paidTotal;

    @Column(name = "share", nullable = false)
    private int share;

    @Column(name = "diff", nullable = false)
    private int diff;

    public void setSettlementResult(SettlementResult settlementResult) {
        this.settlementResult = settlementResult;
    }

}
