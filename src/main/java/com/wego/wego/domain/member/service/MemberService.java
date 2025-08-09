package com.wego.wego.domain.member.service;

import com.wego.wego.domain.member.dto.SignInRequest;
import com.wego.wego.domain.member.dto.SignupRequest;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.repository.MemberRepository;
import com.wego.wego.global.security.JwtProvider;
import com.wego.wego.global.security.JwtToken;
import com.wego.wego.global.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class MemberService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final MemberRepository memberRepository;

    @Transactional
    public void persist(SignupRequest signupRequest) {
        memberRepository.save(Member.builder()
                        .username(signupRequest.username())
                        .password(signupRequest.password())
                        .name(signupRequest.name())
                .build());
    }

    @Transactional
    public JwtToken signIn(SignInRequest signInRequest) {
        Member member = memberRepository.findByUsername(signInRequest.username())
                .orElseThrow(() -> {
                    throw new RuntimeException("회원가입 하지 않은 비회원입니다.");
                });
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(member, signInRequest.password());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(usernamePasswordAuthenticationToken);

        JwtToken jwtToken = jwtProvider.generateToken(authentication);

        redisTemplate.opsForValue().set(
                RedisKeyUtils.whiteListKey(signInRequest.username()),
                        jwtToken.refreshToken(),
                        86400000,
                        TimeUnit.MILLISECONDS
                );
        return jwtToken;
    }

}
