package com.imageflow.backend.domain.image.dto;

public record ImageJobResultUpdateRequest(
        String status,
        String resultImageUrl,
        String outputObjectKey,
        Long sourceFileSizeBytes,
        Long resultFileSizeBytes,
        String failureReason
) {
}
