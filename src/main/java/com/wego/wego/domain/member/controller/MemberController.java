package com.wego.wego.domain.member.controller;

import com.wego.wego.domain.member.dto.SignInRequest;
import com.wego.wego.domain.member.dto.SignupRequest;
import com.wego.wego.domain.member.dto.UpdatePasswordRequest;
import com.wego.wego.domain.member.dto.UpdateUserInfoRequest;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.service.MemberService;
import com.wego.wego.global.security.utils.JwtBlacklistService;
import com.wego.wego.global.security.JwtToken;
import com.wego.wego.global.security.utils.JwtCookieUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/members")
@RestController
@RequiredArgsConstructor
public class MemberController {
    private final PasswordEncoder passwordEncoder;
    private final JwtBlacklistService jwtBlacklistService;

    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest) {
        memberService.persist(signupRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody SignInRequest signupRequest) {
        log.info("signIn init");
        JwtToken jwtToken = memberService.signIn(signupRequest);
        ResponseCookie atCookie = ResponseCookie.from("accessToken", jwtToken.accessToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(3600)
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

    @PostMapping("/check-password")
    public ResponseEntity<?> checkPassword(@RequestBody String passwordCheckRequestDto,
                                           @AuthenticationPrincipal Member member) {
        if (!passwordEncoder.matches(passwordCheckRequestDto, member.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("현재 비밀번호가 일치하지 않습니다.");
        }

        return ResponseEntity.ok("확인되었습니다.");

    }

    /**
     * @param member
     * @param updatePasswordRequest
     * @param accessToken
     * @param refreshToken
     * @return
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> updatePassword(@AuthenticationPrincipal Member member,
                                            @RequestBody UpdatePasswordRequest updatePasswordRequest,
                                            @CookieValue(value = "accessToken", required = false) String accessToken,
                                            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        log.info("비밀번호 변경 시작");
        if (accessToken == null && refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!updatePasswordRequest.newPassword().equals(updatePasswordRequest.confirmNewPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("비밀번호 변경 진행");
        memberService.updatePassword(member.getUsername(), updatePasswordRequest);
        log.info("비밀번호 변경 완료");
        // 2. AT 블랙리스트 등록
        if (accessToken != null) {
            jwtBlacklistService.addChangePWBlacklistToken(accessToken, member.getUsername());
        }
        // 3. RT 블랙리스트 등록
        if (refreshToken != null) {
            jwtBlacklistService.addBlacklistToken(refreshToken, member.getUsername());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, JwtCookieUtils.deleteAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, JwtCookieUtils.deleteRefreshTokenCookie().toString())
                .body("비밀번호 변경 완료");
    }

    /** 닉네임 변경
     * @param member
     * @param updateUserInfoRequest
     * @return
     */
    @PatchMapping("/change-info")
    public ResponseEntity<?> updateMemberInfo(@AuthenticationPrincipal Member member,
                                              @RequestBody UpdateUserInfoRequest updateUserInfoRequest){
        memberService.updateMemberInfo(updateUserInfoRequest, member);
        return ResponseEntity.ok().body("회원정보 업데이트 완료");

    }

    /**
     * @param refreshToken
     * @param accessToken
     * @return
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refreshToken", required = false) String refreshToken,
                                    @CookieValue(value = "accessToken", required = false) String accessToken) {
        if (accessToken == null && refreshToken == null) {
            log.info("로그아웃 실패");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("로그아웃 진행");

        // 2. AT 블랙리스트 등록
        if (accessToken != null) {
            jwtBlacklistService.addLogoutBlacklistToken(accessToken,"logout");
        }

        // 3. RT 블랙리스트 등록
        if (refreshToken != null) {
            jwtBlacklistService.addLogoutBlacklistToken(refreshToken,"logout");
        }
        log.info("로그아웃 완료");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, JwtCookieUtils.deleteAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, JwtCookieUtils.deleteRefreshTokenCookie().toString())
                .body("로그아웃 완료");
    }

    /**
     * @param member
     * @param refreshToken
     * @param accessToken
     * @return
     */
    @DeleteMapping
    public ResponseEntity<?> deleteMember(@AuthenticationPrincipal Member member,
                                          @CookieValue(value = "refreshToken", required = false) String refreshToken,
                                          @CookieValue(value = "accessToken", required = false) String accessToken) {
        if (accessToken == null && refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. AT 블랙리스트 등록
        if (accessToken != null) {
            jwtBlacklistService.addBlacklistToken(accessToken,"logout");
        }

        // 3. RT 블랙리스트 등록
        if (refreshToken != null) {
            jwtBlacklistService.addBlacklistToken(refreshToken,"logout");
        }

        memberService.deleteMember(member);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, JwtCookieUtils.deleteAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, JwtCookieUtils.deleteRefreshTokenCookie().toString())
                .body("회원 탈퇴가 완료되었습니다.");
    }


}
