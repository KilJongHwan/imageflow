package com.imageflow.backend.domain.auth.dto;

public record SignupRequest(
        String email,
        String password
) {
}
