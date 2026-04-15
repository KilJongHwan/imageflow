package com.imageflow.backend.domain.watermark;

import java.util.List;

import org.springframework.stereotype.Service;

import com.imageflow.backend.common.exception.BadRequestException;
import com.imageflow.backend.domain.watermark.dto.GenerateWatermarkPresetsRequest;
import com.imageflow.backend.domain.watermark.dto.WatermarkPresetResponse;

@Service
public class WatermarkPresetService {

    public List<WatermarkPresetResponse> generatePresets(GenerateWatermarkPresetsRequest request) {
        String brandText = normalizeRequiredText(request.brandText(), "brandText");
        String accentText = normalizeOptionalText(request.accentText());
        String tone = normalizeTone(request.tone());

        return switch (tone) {
            case "clean" -> List.of(
                    new WatermarkPresetResponse("clean-signature", "Clean Signature", tone, brandText, accentText, "signature", "bottom-right", "soft-pill", "#ffffff", "#dbeafe", "#0f172a", 58, 18),
                    new WatermarkPresetResponse("clean-outline", "Clean Outline", tone, brandText, accentText, "outline", "bottom-left", "outline-box", "#f8fafc", "#bfdbfe", "#1e293b", 48, 20),
                    new WatermarkPresetResponse("clean-badge", "Clean Badge", tone, brandText, accentText, "badge", "top-right", "solid-badge", "#ffffff", "#bfdbfe", "#111827", 62, 16)
            );
            case "premium" -> List.of(
                    new WatermarkPresetResponse("premium-plaque", "Premium Plaque", tone, brandText, accentText, "plaque", "bottom-right", "dark-glass", "#f8fafc", "#f5d58b", "#111827", 66, 20),
                    new WatermarkPresetResponse("premium-monogram", "Premium Monogram", tone, brandText, accentText, "monogram", "center", "center-mark", "#fff7ed", "#f5d58b", "#1f2937", 34, 24),
                    new WatermarkPresetResponse("premium-ribbon", "Premium Ribbon", tone, brandText, accentText, "ribbon", "bottom-left", "soft-ribbon", "#fefce8", "#facc15", "#1f2937", 54, 19)
            );
            case "playful" -> List.of(
                    new WatermarkPresetResponse("playful-pop", "Playful Pop", tone, brandText, accentText, "pop", "top-right", "color-pop", "#ffffff", "#fda4af", "#9f1239", 68, 18),
                    new WatermarkPresetResponse("playful-sticker", "Playful Sticker", tone, brandText, accentText, "sticker", "bottom-right", "sticker", "#fff7ed", "#fdba74", "#7c2d12", 64, 17),
                    new WatermarkPresetResponse("playful-label", "Playful Label", tone, brandText, accentText, "label", "bottom-left", "split-label", "#ffffff", "#93c5fd", "#1d4ed8", 56, 18)
            );
            default -> throw new BadRequestException("tone must be one of clean, premium, playful");
        };
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }

        String trimmed = value.trim();
        if (trimmed.length() > 40) {
            throw new BadRequestException(fieldName + " must be 40 characters or fewer");
        }
        return trimmed;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.length() > 40) {
            throw new BadRequestException("accentText must be 40 characters or fewer");
        }
        return trimmed;
    }

    private String normalizeTone(String tone) {
        if (tone == null || tone.isBlank()) {
            return "clean";
        }
        return tone.trim().toLowerCase();
    }
}
