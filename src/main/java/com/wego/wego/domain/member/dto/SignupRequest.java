package com.wego.wego.domain.member.dto;

public record SignupRequest (
        String name,
        String username,
        String password
){}
