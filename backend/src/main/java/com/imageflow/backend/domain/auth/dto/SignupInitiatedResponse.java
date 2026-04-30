package com.imageflow.backend.domain.auth.dto;

import java.time.LocalDateTime;

public record SignupInitiatedResponse(
        String email,
        String message,
        LocalDateTime expiresAt
) {
}
