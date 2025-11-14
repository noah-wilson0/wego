package com.wego.wego.domain.auth.controller;

import com.wego.wego.domain.auth.service.AuthService;
import com.wego.wego.domain.member.dto.SignInRequest;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.global.security.JwtToken;
import com.wego.wego.global.security.utils.JwtBlacklistService;
import com.wego.wego.global.security.utils.JwtCookieUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final JwtBlacklistService jwtBlacklistService;

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<String> getMember(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok().body(member.getName());
    }
    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody SignInRequest signupRequest) {
        log.info("signIn init");
        JwtToken jwtToken = authService.signIn(signupRequest);
        ResponseCookie atCookie = ResponseCookie.from("accessToken", jwtToken.accessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(14400) //3600 인데 테스트중에는 14400사용
                .sameSite("Strict")
                .build();

        ResponseCookie rtCookie = ResponseCookie.from("refreshToken", jwtToken.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(86400)
                .sameSite("Strict")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,atCookie.toString())
                .header(HttpHeaders.SET_COOKIE,rtCookie.toString())
                .body(jwtToken);
    }
    /**
     * @param refreshToken
     * @param accessToken
     * @return
     */
    @PostMapping("/sign-out")
    public ResponseEntity<?> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken,
                                    @CookieValue(value = "accessToken", required = false) String accessToken,
                                    @AuthenticationPrincipal Member member) {
        if (accessToken == null && refreshToken == null) {
            log.info("로그아웃 실패");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("로그아웃 진행");

        // 2. AT 블랙리스트 등록
        if (accessToken != null) {
            jwtBlacklistService.addLogoutBlacklistToken(accessToken,member.getUsername());
        }

        // 3. RT 블랙리스트 등록
        if (refreshToken != null) {
            jwtBlacklistService.addLogoutBlacklistToken(refreshToken,member.getUsername());
        }
        log.info("로그아웃 완료");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, JwtCookieUtils.deleteAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, JwtCookieUtils.deleteRefreshTokenCookie().toString())
                .body("로그아웃 완료");
    }



}
