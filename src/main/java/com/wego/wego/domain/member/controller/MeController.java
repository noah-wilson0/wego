package com.wego.wego.domain.member.controller;

import com.wego.wego.domain.feed.dto.FeedResponse;
import com.wego.wego.domain.member.dto.MemberDetailResponse;
import com.wego.wego.domain.member.dto.UpdatePasswordRequest;
import com.wego.wego.domain.member.dto.UpdateUserInfoRequest;
import com.wego.wego.domain.member.entity.Member;
import com.wego.wego.domain.member.service.MeService;
import com.wego.wego.global.security.utils.JwtBlacklistService;
import com.wego.wego.global.security.utils.JwtCookieUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/me")
@RestController
@RequiredArgsConstructor
public class MeController {
    private final PasswordEncoder passwordEncoder;
    private final JwtBlacklistService jwtBlacklistService;

    private final MeService meService;

    @GetMapping
    public ResponseEntity<MemberDetailResponse> getMemberDetail(@AuthenticationPrincipal Member member) {
        MemberDetailResponse memberDetail = meService.getMemberDetail(member.getId());

        return ResponseEntity.ok(memberDetail);
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
        meService.updatePassword(member.getUsername(), updatePasswordRequest);
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
        meService.updateMemberInfo(updateUserInfoRequest, member);
        return ResponseEntity.ok().body("회원정보 업데이트 완료");

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

        meService.deleteMember(member);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, JwtCookieUtils.deleteAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, JwtCookieUtils.deleteRefreshTokenCookie().toString())
                .body("회원 탈퇴가 완료되었습니다.");
    }

    /**
     * 마이 페이지(메인) 회원 피드 리스트 조회
     * 마이 페이지 피드 페이지 회원 피드 리스트 조회
     * @param member
     * @return
     *
     * TODO: 마이 페이지 피드 페이지를 조회하기 위한 페이징 처리 필요
     *  마이페이지 메인은 피드 2개만 출력해주는데 피드 2개는 가장 최근에 생성한 피드 이므로 컨트롤러 분해 필요
     */
    @GetMapping("/feeds")
    public ResponseEntity<List<FeedResponse>> getMemberFeeds(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(meService.getFeedsByMember(member));
    }
}
