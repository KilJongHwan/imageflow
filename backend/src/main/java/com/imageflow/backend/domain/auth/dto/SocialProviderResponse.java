package com.imageflow.backend.domain.auth.dto;

public record SocialProviderResponse(
        String provider,
        boolean enabled,
        String authUrl
) {
}
