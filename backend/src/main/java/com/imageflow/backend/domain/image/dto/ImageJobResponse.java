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
        String outputObjectKey,
        String status,
        int creditsUsed,
        Integer targetWidth,
        Integer targetHeight,
        int quality,
        String outputFormat,
        String aspectRatio,
        String watermarkText,
        String cropMode,
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
                imageJob.getOutputObjectKey(),
                imageJob.getStatus().name(),
                imageJob.getCreditsUsed(),
                imageJob.getTargetWidth(),
                imageJob.getTargetHeight(),
                imageJob.getQuality(),
                imageJob.getOutputFormat(),
                imageJob.getAspectRatio(),
                imageJob.getWatermarkText(),
                imageJob.getCropMode(),
                imageJob.getFailureReason(),
                imageJob.getCreatedAt(),
                imageJob.getUpdatedAt()
        );
    }
}
