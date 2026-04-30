package com.imageflow.backend.domain.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.imageflow.backend.domain.user.User;

public record AuthUserResponse(
        UUID id,
        String email,
        String plan,
        String role,
        String authProvider,
        boolean emailVerified,
        int creditBalance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getPlan().name(),
                user.getRole().name(),
                user.getAuthProvider().name(),
                user.isEmailVerified(),
                user.getCreditBalance(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
