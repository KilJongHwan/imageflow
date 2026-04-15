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
        String watermarkAccentText,
        String watermarkStyle,
        String watermarkPosition,
        Integer watermarkOpacity,
        Integer watermarkScalePercent,
        String cropMode,
        Integer cropX,
        Integer cropY,
        Integer cropWidth,
        Integer cropHeight,
        Long sourceFileSizeBytes,
        Long resultFileSizeBytes,
        Long savedBytes,
        Double reductionRate,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ImageJobResponse from(ImageJob imageJob) {
        Long sourceFileSizeBytes = imageJob.getSourceFileSizeBytes();
        Long resultFileSizeBytes = imageJob.getResultFileSizeBytes();
        Long savedBytes = null;
        Double reductionRate = null;

        if (sourceFileSizeBytes != null && resultFileSizeBytes != null) {
            savedBytes = sourceFileSizeBytes - resultFileSizeBytes;
            if (sourceFileSizeBytes > 0) {
                reductionRate = (double) savedBytes / sourceFileSizeBytes;
            }
        }

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
                imageJob.getWatermarkAccentText(),
                imageJob.getWatermarkStyle(),
                imageJob.getWatermarkPosition(),
                imageJob.getWatermarkOpacity(),
                imageJob.getWatermarkScalePercent(),
                imageJob.getCropMode(),
                imageJob.getCropX(),
                imageJob.getCropY(),
                imageJob.getCropWidth(),
                imageJob.getCropHeight(),
                sourceFileSizeBytes,
                resultFileSizeBytes,
                savedBytes,
                reductionRate,
                imageJob.getFailureReason(),
                imageJob.getCreatedAt(),
                imageJob.getUpdatedAt()
        );
    }
}
