package com.wego.wego.domain.member.dto;

public record UpdatePasswordRequest(
        String newPassword,
        String confirmNewPassword
){}