package com.imageflow.backend.domain.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.imageflow.backend.domain.user.User;

public record UserResponse(
        UUID id,
        String email,
        String apiKey,
        String plan,
        int creditBalance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getApiKey(),
                user.getPlan().name(),
                user.getCreditBalance(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
