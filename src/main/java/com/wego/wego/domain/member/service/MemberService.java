package com.wego.wego.domain.member.service;

import com.wego.wego.domain.member.dto.SignupRequest;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class MemberService {
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    @Transactional
    public void persist(SignupRequest signupRequest) {
        memberRepository.save(Member.builder()
                        .username(signupRequest.username())
                        .password(passwordEncoder.encode(signupRequest.password()))
                        .name(signupRequest.name())
                .build());
    }

    public Optional<Member> findByMemberId(long memberId) {
        return memberRepository.findById(memberId);
    }

}
