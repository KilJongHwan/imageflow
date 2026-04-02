package com.imageflow.backend.queue;

import java.util.UUID;

public record ImageJobQueueMessage(
        UUID jobId,
        UUID userId,
        String sourceFilePath,
        String sourceImageUrl,
        String prompt,
        Integer targetWidth,
        int quality,
        String outputFormat,
        String outputObjectKey,
        String outputFilePath,
        String resultImageUrl
) {
}
