package com.wego.wego.domain.settlement.entity;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.global.enums.SettlementCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="settlement_item")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class SettlementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id ")
    private Settlement settlement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private SettlementCategory category;  //값 타입 컬렉션 적용

    @Column(name = "paid", nullable = false)
    private int paid;

    @Builder.Default
    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt= LocalDateTime.now();

}
