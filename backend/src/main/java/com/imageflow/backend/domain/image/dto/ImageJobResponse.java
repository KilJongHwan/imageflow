package com.imageflow.backend.domain.image.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.imageflow.backend.domain.image.ImageJob;

public record ImageJobResponse(
        UUID id,
        UUID userId,
        String prompt,
        String sourceImageUrl,
        String resultImageUrl,
        String status,
        int creditsUsed,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ImageJobResponse from(ImageJob imageJob) {
        return new ImageJobResponse(
                imageJob.getId(),
                imageJob.getUser().getId(),
                imageJob.getPrompt(),
                imageJob.getSourceImageUrl(),
                imageJob.getResultImageUrl(),
                imageJob.getStatus().name(),
                imageJob.getCreditsUsed(),
                imageJob.getFailureReason(),
                imageJob.getCreatedAt(),
                imageJob.getUpdatedAt()
        );
    }
}
