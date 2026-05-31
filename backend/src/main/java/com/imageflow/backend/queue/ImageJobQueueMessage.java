package com.imageflow.backend.queue;

import java.util.UUID;
import java.nio.file.Path;

import com.imageflow.backend.domain.image.ImageJob;
import com.imageflow.backend.common.storage.StoredFile;

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
        String watermarkFontFamily,
        String watermarkAccentText,
        String watermarkImageUrl,
        String watermarkImageFilePath,
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
    public static ImageJobQueueMessage from(
            ImageJob job,
            String sourceFilePath,
            StoredFile watermarkStoredFile,
            String outputFilename,
            Path outputPath,
            String resultImageUrl
    ) {
        return new ImageJobQueueMessage(
                job.getId(),
                job.getUser().getId(),
                sourceFilePath,
                job.getSourceImageUrl(),
                job.getPrompt(),
                job.getTargetWidth(),
                job.getTargetHeight(),
                job.getQuality(),
                job.getOutputFormat(),
                job.getAspectRatio(),
                job.getWatermarkText(),
                job.getWatermarkFontFamily(),
                job.getWatermarkAccentText(),
                job.getWatermarkImageUrl(),
                watermarkStoredFile == null ? null : watermarkStoredFile.path().toString(),
                job.getWatermarkStyle(),
                job.getWatermarkPosition(),
                job.getWatermarkOpacity(),
                job.getWatermarkScalePercent(),
                job.getCropMode(),
                job.getCropX(),
                job.getCropY(),
                job.getCropWidth(),
                job.getCropHeight(),
                outputFilename,
                outputPath.toString(),
                resultImageUrl
        );
    }

    public static ImageJobQueueMessage fromGenerationJob(ImageJob job) {
        return new ImageJobQueueMessage(
                job.getId(),
                job.getUser().getId(),
                null,
                job.getSourceImageUrl(),
                job.getPrompt(),
                job.getTargetWidth(),
                job.getTargetHeight(),
                job.getQuality(),
                job.getOutputFormat(),
                job.getAspectRatio(),
                job.getWatermarkText(),
                job.getWatermarkFontFamily(),
                job.getWatermarkAccentText(),
                job.getWatermarkImageUrl(),
                null,
                job.getWatermarkStyle(),
                job.getWatermarkPosition(),
                job.getWatermarkOpacity(),
                job.getWatermarkScalePercent(),
                job.getCropMode(),
                job.getCropX(),
                job.getCropY(),
                job.getCropWidth(),
                job.getCropHeight(),
                "optimized/" + job.getId() + "." + job.getOutputFormat(),
                null,
                null
        );
    }
}
