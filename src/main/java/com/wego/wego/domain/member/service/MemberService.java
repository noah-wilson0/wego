package com.wego.wego.domain.member.service;

import com.wego.wego.domain.member.dto.SignInRequest;
import com.wego.wego.domain.member.dto.SignupRequest;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;
import com.wego.wego.global.security.JwtProvider;
import com.wego.wego.global.security.JwtToken;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class MemberService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final MemberRepository memberRepository;

    @Transactional
    public void persist(SignupRequest signupRequest) {
        memberRepository.save(Member.builder()
                        .username(signupRequest.username())
                        .password(passwordEncoder.encode(signupRequest.password()))
                        .name(signupRequest.name())
                .build());
    }

    @Transactional
    public JwtToken signIn(SignInRequest signInRequest) {
        log.info("signIn init");
        memberRepository.findByUsername(signInRequest.username())
                .orElseThrow(() -> {
                    throw new RuntimeException("회원가입 하지 않은 비회원입니다.");
                });
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(signInRequest.username(), signInRequest.password());
        log.info("jwt init");
        Authentication authentication = null;
        try {
           authentication = authenticationManagerBuilder.getObject().authenticate(usernamePasswordAuthenticationToken);
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("authentication실패");
        }
        log.info("authentication init");
        JwtToken jwtToken = jwtProvider.generateToken(authentication);
        log.info("jwt finish");
        redisTemplate.opsForValue().set(
                RedisKeyUtils.whiteListKey(signInRequest.username()),
                        jwtToken.refreshToken(),
                        86400000,
                        TimeUnit.MILLISECONDS
                );
        return jwtToken;
    }

    public Optional<Member> findByMemberId(long memberId) {
        return memberRepository.findById(memberId);
    }

}
