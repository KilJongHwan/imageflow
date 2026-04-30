package com.imageflow.backend.domain.auth.dto;

public record VerifyEmailRequest(
        String email,
        String code
) {
}
