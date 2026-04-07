package com.imageflow.backend.domain.auth.dto;

public record AuthResponse(
        String token,
        AuthUserResponse user
) {
}
