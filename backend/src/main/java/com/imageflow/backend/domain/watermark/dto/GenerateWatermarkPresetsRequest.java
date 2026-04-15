package com.imageflow.backend.domain.watermark.dto;

public record GenerateWatermarkPresetsRequest(
        String brandText,
        String accentText,
        String tone
) {
}
