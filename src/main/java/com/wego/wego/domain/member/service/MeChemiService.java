package com.wego.wego.domain.member.service;

import com.wego.wego.domain.chemi.dto.ChemiDto;
import com.wego.wego.domain.chemi.entity.Chemi;
import com.wego.wego.domain.chemi.repository.ChemiRepository;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeChemiService {
    private final MemberRepository memberRepository;
    private final ChemiRepository chemiRepository;

    @Transactional
    public void persistMemberChemi(String chemi, Member member) {
        log.info(chemi);
        Chemi findChemi = chemiRepository.findByName(chemi)
                .orElseThrow(() -> {
                    throw new RuntimeException("케미 못참음");
                });

        Member findMember = memberRepository.findByUsername(member.getUsername())
                .orElseThrow(() -> {
                    throw new RuntimeException("유저 못참음");
                });

        findMember.updateChemiId(findChemi.getId());
    }

    public ChemiDto findByMemberChemi(Member member) {
        Member findMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없음"));
        Long chemiId = findMember.getChemiId();
        if (chemiId == null) {
            throw new RuntimeException("케미 테스트 해야 됨");
        }else {
            Chemi chemi = chemiRepository.findById(chemiId)
                    .orElseThrow(() -> new RuntimeException("케미 없음"));

            return new ChemiDto(chemi.getName(), chemi.getImage(), chemi.getDescription());
        }
    }

}
