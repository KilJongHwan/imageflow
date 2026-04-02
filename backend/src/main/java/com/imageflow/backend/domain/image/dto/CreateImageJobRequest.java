package com.imageflow.backend.domain.image.dto;

import java.util.UUID;

public record CreateImageJobRequest(
        UUID userId,
        String prompt,
        String sourceImageUrl,
        Integer creditsToUse,
        Integer targetWidth,
        Integer quality,
        String outputFormat
) {
}
