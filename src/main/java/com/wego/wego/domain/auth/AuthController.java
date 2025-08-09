package com.wego.wego.domain.auth;

import com.wego.wego.domain.member.entity.Member;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/auth")
@RestController
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<String> getMember(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok().body(member.getUsername());
    }
}
