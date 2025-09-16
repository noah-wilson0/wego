package com.wego.wego.domain.member.controller;

import com.wego.wego.domain.member.dto.SignupRequest;
import com.wego.wego.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/members")
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/sign-up")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest) {
        memberService.persist(signupRequest);
        return ResponseEntity.ok().build();
    }






}
