package com.wego.wego.domain.plan.service;

import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.plan.entity.TravelPlan;
import com.wego.wego.domain.plan.entity.TravelPlanShare;
import com.wego.wego.domain.plan.repository.TravelPlanRepository;
import com.wego.wego.domain.plan.repository.TravelPlanShareRepository;
import com.wego.wego.domain.plan.util.ShareTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TravelPlanShareService {
    private final TravelPlanRepository travelPlanRepository;
    private final TravelPlanShareRepository travelPlanShareRepository;

    @Transactional
    public String createShareToken(String travelPlanId, Member member) {
        TravelPlan travelPlan = travelPlanRepository.findById(Long.valueOf(travelPlanId))
                .orElseThrow(() -> new RuntimeException("Travel plan not found"));
        
        //1) 토큰 생성
        String plainToken  = ShareTokenUtil.newBase64UrlToken(24);

        //2) 토큰 암호화
        String tokenHash = ShareTokenUtil.sha256Hex(plainToken);

        LocalDate expiresAt = travelPlan.getEndDate().plusDays(1);

        travelPlanShareRepository.save(TravelPlanShare.builder()
                        .travelPlan(travelPlan)
                        .member(member)
                        .token(tokenHash)
                        .expiresAt(expiresAt)
                .build());
        return plainToken;
        
    }

    public TravelPlan getShareTravelPlan(String plainToken) {
        String tokenHash = ShareTokenUtil.sha256Hex(plainToken);
        return travelPlanShareRepository
                .findTravelPlanByToken(tokenHash, LocalDate.now())
                .orElseThrow(() -> new IllegalArgumentException("invalid or expired token"));
    }
}
