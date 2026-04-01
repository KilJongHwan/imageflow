package com.imageflow.backend.domain.user.dto;

public record CreateUserRequest(
        String email,
        String plan,
        Integer initialCredits
) {
}
