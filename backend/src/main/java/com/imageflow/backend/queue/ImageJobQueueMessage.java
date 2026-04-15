package com.imageflow.backend.queue;

import java.util.UUID;

public record ImageJobQueueMessage(
        UUID jobId,
        UUID userId,
        String sourceFilePath,
        String sourceImageUrl,
        String prompt,
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
        String outputObjectKey,
        String outputFilePath,
        String resultImageUrl
) {
}
