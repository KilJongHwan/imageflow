package com.imageflow.backend.domain.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.imageflow.backend.domain.user.User;

public record AuthUserResponse(
        UUID id,
        String email,
        String plan,
        int creditBalance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getPlan().name(),
                user.getCreditBalance(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
