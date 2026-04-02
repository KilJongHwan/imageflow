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
        String cropMode,
        String outputObjectKey,
        String outputFilePath,
        String resultImageUrl
) {
}
