package com.imageflow.backend.domain.auth.dto;

public record LoginRequest(
        String email,
        String password
) {
}
