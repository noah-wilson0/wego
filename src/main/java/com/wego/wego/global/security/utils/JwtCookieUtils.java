package com.wego.wego.global.security.utils;

import org.springframework.http.ResponseCookie;

public class JwtCookieUtils {

    public static ResponseCookie deleteAccessTokenCookie() {
        return createAccessTokenCookie("", 0);
    }

    public static ResponseCookie deleteRefreshTokenCookie() {
        return createRefreshTokenCookie("", 0);
    }
    public static ResponseCookie createAccessTokenCookie(String value, int maxAgeSec) {
        return ResponseCookie.from("accessToken", value)
                .httpOnly(true)
                .secure(true) // 운영/개발 환경 분기 처리 가능
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite("Strict")
                .build();
    }

    public static ResponseCookie createRefreshTokenCookie(String value, int maxAgeSec) {
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite("Strict")
                .build();
    }
}
