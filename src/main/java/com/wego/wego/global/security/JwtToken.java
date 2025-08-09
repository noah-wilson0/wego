package com.wego.wego.global.security;

import lombok.Builder;
import lombok.Getter;


public record JwtToken (
    String accessToken,
    String refreshToken
){}
