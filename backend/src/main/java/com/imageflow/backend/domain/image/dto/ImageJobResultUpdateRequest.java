package com.imageflow.backend.domain.image.dto;

public record ImageJobResultUpdateRequest(
        String status,
        String resultImageUrl,
        String outputObjectKey,
        String failureReason
) {
}
