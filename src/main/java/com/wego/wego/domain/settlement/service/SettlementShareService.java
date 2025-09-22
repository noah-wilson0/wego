package com.wego.wego.domain.settlement.service;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.repository.TravelPlanRepository;
import com.wego.wego.domain.plan.repository.TravelPlanShareRepository;
import com.wego.wego.domain.plan.util.ShareTokenUtil;
import com.wego.wego.domain.settlement.dto.SettlementItemRequest;
import com.wego.wego.domain.settlement.dto.SettlementResponse;
import com.wego.wego.domain.settlement.dto.SettlementResultResponse;
import com.wego.wego.domain.settlement.entity.Settlement;
import com.wego.wego.domain.settlement.entity.SettlementItem;
import com.wego.wego.domain.settlement.entity.SettlementParticipant;
import com.wego.wego.domain.settlement.entity.SettlementResult;
import com.wego.wego.domain.settlement.repository.SettlementItemRepository;
import com.wego.wego.domain.settlement.repository.SettlementRepository;
import com.wego.wego.domain.settlement.repository.SettlementResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class SettlementShareService {
    private final MemberRepository memberRepository;

    private final TravelPlanRepository travelPlanRepository;
    private final TravelPlanShareRepository travelPlanShareRepository;

    private final SettlementRepository settlementRepository;
    private final SettlementItemRepository settlementItemRepository;

    private final SettlementResultRepository settlementResultRepository;

    public SettlementResponse getAll(String token) {
        TravelPlan travelPlan = travelPlanShareRepository.findTravelPlanByToken(ShareTokenUtil.sha256Hex(token), LocalDate.now())
                .orElseThrow(() -> new RuntimeException("travel plan not found"));

        Settlement findSettlement = settlementRepository.findWithItemsByTravelPlanId(Long.valueOf(travelPlan.getId()))
                .orElseThrow(() -> new RuntimeException("Settlement not found for travelPlanId=" + travelPlan.getId()));

        return SettlementResponse.from(findSettlement);

    }

    @Transactional
    public void saveSettlementItem(String token, SettlementItemRequest settlementItemRequest, Member member) {

        TravelPlan travelPlan = travelPlanShareRepository.findTravelPlanByToken(ShareTokenUtil.sha256Hex(token), LocalDate.now())
                .orElseThrow(() -> new RuntimeException("travel plan not found"));

        Settlement findSettlement = settlementRepository.findWithItemsByTravelPlanId(Long.valueOf(travelPlan.getId()))
                .orElseThrow(() -> new RuntimeException("Settlement not found for travelPlanId=" + travelPlan.getId()));

        Member findMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new IllegalArgumentException("member not found: "));

        findSettlement.changeTotalPaid(findSettlement.getTotalPaid() + settlementItemRequest.paid());

        settlementItemRepository.save(SettlementItem.builder()
                .settlement(findSettlement)
                .member(findMember)
                .category(settlementItemRequest.category())
                .paid(settlementItemRequest.paid())
                .build());

    }

    @Transactional
    public int createBudget(String token, int budget) {

        TravelPlan travelPlan = travelPlanShareRepository.findTravelPlanByToken(ShareTokenUtil.sha256Hex(token), LocalDate.now())
                .orElseThrow(() -> new RuntimeException("travel plan not found"));
        settlementRepository.save(Settlement.builder()
                .travelPlan(travelPlan)
                .budget(budget)
                .build());

        return budget;
    }

    @Transactional
    public int updateBudget(String token, int budget) {
        TravelPlan travelPlan = travelPlanShareRepository.findTravelPlanByToken(ShareTokenUtil.sha256Hex(token), LocalDate.now())
                .orElseThrow(() -> new RuntimeException("travel plan not found"));

        Settlement findSettlement = settlementRepository.findWithItemsByTravelPlanId(Long.valueOf(travelPlan.getId()))
                .orElseThrow(() -> new RuntimeException("Settlement not found for travelPlanId=" + travelPlan.getId()));

        int chageBudget = findSettlement.changeBudget(budget);

        return chageBudget;
    }
    @Transactional
    public SettlementResultResponse createSettlementResult(String token) {
        TravelPlan travelPlan = travelPlanShareRepository.findTravelPlanByToken(ShareTokenUtil.sha256Hex(token), LocalDate.now())
                .orElseThrow(() -> new RuntimeException("travel plan not found"));

        Settlement findSettlement = settlementRepository.findWithItemsByTravelPlanId(Long.valueOf(travelPlan.getId()))
                .orElseThrow(() -> new RuntimeException("Settlement not found for travelPlanId=" + travelPlan.getId()));

        int budget = findSettlement.getBudget();

        Map<Member, Integer> settlements = new HashMap<>();
        findSettlement.getSettlementItems().stream().forEach(si ->
                settlements.merge(
                        si.getMember(), si.getPaid(), (oldValue, newValue) -> oldValue + newValue)
        );
        int people = settlements.size();

        Map<Member, Integer> shareMap = new HashMap<>();
        settlements.forEach((k, v) -> {
            shareMap.put(k, ceilToTen((double) v / settlements.size()));
        });

        SettlementResult result = SettlementResult.builder()
                .settlement(findSettlement)
                .build();

        List<SettlementResultResponse.Participant> participants = new ArrayList();
        shareMap.forEach((k, v) -> {
            int diff = getDiff(k, v, settlements);
            Integer paidTotal = settlements.get(k);
            participants.add(
                    new SettlementResultResponse.Participant(k.getName(), paidTotal, v, diff));


            result.addParticipant(
                    SettlementParticipant.builder()
                            .member(k)
                            .paidTotal(paidTotal)
                            .share(v)
                            .diff(diff)
                            .build()
            );
        });


        settlementResultRepository.save(result);

        return SettlementResultResponse.from(budget, people, participants);

    }


    public SettlementResultResponse getSettlementResult(String token) {
        TravelPlan travelPlan = travelPlanShareRepository.findTravelPlanByToken(ShareTokenUtil.sha256Hex(token), LocalDate.now())
                .orElseThrow(() -> new RuntimeException("travel plan not found"));

        Settlement findSettlement = settlementRepository.findWithItemsByTravelPlanId(Long.valueOf(travelPlan.getId()))
                .orElseThrow(() -> new RuntimeException("Settlement not found for travelPlanId=" + travelPlan.getId()));


        SettlementResult settlementResult = settlementResultRepository.findBySettlementId(findSettlement.getId())
                .orElseThrow(() -> new RuntimeException("SettlementResult not found for settlementId=" + findSettlement.getId()));


        int budget = settlementResult.getSettlement().getBudget();
        List<SettlementParticipant> participants = settlementResult.getSettlementParticipants();
        int people = participants.size();

        List<SettlementResultResponse.Participant> participantResponses = participants.stream()
                .map(p -> SettlementResultResponse.Participant.from(
                        p.getMember().getName(),
                        p.getPaidTotal(),
                        p.getShare(),
                        p.getDiff()
                ))
                .toList();


        return SettlementResultResponse.from(budget, people, participantResponses);
    }

    private static int getDiff(Member member, Integer v, Map<Member, Integer> settlements) {
        return v - settlements.get(member);
    }


    private int ceilToTen(double amount) {
        return (int) (Math.ceil(amount / 10.0) * 10);
    }
}
