package com.imageflow.backend.domain.watermark.dto;

public record WatermarkPresetResponse(
        String id,
        String label,
        String tone,
        String brandText,
        String accentText,
        String style,
        String position,
        String backgroundStyle,
        String textColor,
        String accentColor,
        String backgroundColor,
        int recommendedOpacity,
        int recommendedScalePercent
) {
}
