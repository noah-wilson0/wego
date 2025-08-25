package com.wego.wego.domain.profile.controller;

import com.wego.wego.domain.member.dto.MemberDetailResponse;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<MemberDetailResponse> getMemberDetail(@AuthenticationPrincipal Member member) {
        MemberDetailResponse memberDetail = memberService.getMemberDetail(member.getId());

        return ResponseEntity.ok(memberDetail);
    }



}
