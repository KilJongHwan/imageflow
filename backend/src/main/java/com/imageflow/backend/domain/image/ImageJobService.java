package com.imageflow.backend.domain.image;

import java.util.UUID;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.imageflow.backend.common.storage.StoredFile;
import com.imageflow.backend.common.storage.StorageService;
import com.imageflow.backend.common.exception.BadRequestException;
import com.imageflow.backend.common.exception.NotFoundException;
import com.imageflow.backend.domain.image.dto.CreateImageJobRequest;
import com.imageflow.backend.domain.image.dto.ImageJobResponse;
import com.imageflow.backend.domain.image.dto.ImageJobResultUpdateRequest;
import com.imageflow.backend.domain.usage.UsageRecord;
import com.imageflow.backend.domain.usage.UsageRecordRepository;
import com.imageflow.backend.domain.usage.UsageType;
import com.imageflow.backend.domain.user.User;
import com.imageflow.backend.domain.user.UserRepository;
import com.imageflow.backend.domain.user.UserService;
import com.imageflow.backend.queue.ImageJobQueueMessage;
import com.imageflow.backend.queue.ImageJobQueuePublisher;

@Service
@Transactional
public class ImageJobService {

    private final UserRepository userRepository;
    private final ImageJobRepository imageJobRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final ImageJobQueuePublisher imageJobQueuePublisher;
    private final ImageProcessingService imageProcessingService;
    private final UserService userService;
    private final StorageService storageService;
    private final String publicBaseUrl;
    private final String processingMode;

    public ImageJobService(
            UserRepository userRepository,
            ImageJobRepository imageJobRepository,
            UsageRecordRepository usageRecordRepository,
            ImageJobQueuePublisher imageJobQueuePublisher,
            ImageProcessingService imageProcessingService,
            UserService userService,
            StorageService storageService,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${app.processing.mode:sync}") String processingMode
    ) {
        this.userRepository = userRepository;
        this.imageJobRepository = imageJobRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.imageJobQueuePublisher = imageJobQueuePublisher;
        this.imageProcessingService = imageProcessingService;
        this.userService = userService;
        this.storageService = storageService;
        this.publicBaseUrl = publicBaseUrl;
        this.processingMode = processingMode;
    }

    public ImageJobResponse create(CreateImageJobRequest request) {
        if (request.userId() == null) {
            throw new BadRequestException("userId is required");
        }
        if (request.prompt() == null || request.prompt().isBlank()) {
            throw new BadRequestException("prompt is required");
        }

        int creditsToUse = request.creditsToUse() == null ? 1 : request.creditsToUse();
        if (creditsToUse <= 0) {
            throw new BadRequestException("creditsToUse must be greater than zero");
        }
        int quality = request.quality() == null ? 80 : request.quality();
        if (quality < 1 || quality > 100) {
            throw new BadRequestException("quality must be between 1 and 100");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("user not found: " + request.userId()));

        user.chargeCredits(creditsToUse);

        ImageJob imageJob = new ImageJob(user, request.prompt().trim(), creditsToUse);
        if (request.sourceImageUrl() != null && !request.sourceImageUrl().isBlank()) {
            imageJob.setSourceImageUrl(request.sourceImageUrl().trim());
        }
        imageJob.setTargetWidth(request.targetWidth());
        imageJob.setTargetHeight(null);
        imageJob.setQuality(quality);
        imageJob.setOutputFormat(resolveOutputFormat(request.outputFormat()));
        imageJob.setAspectRatio("original");
        imageJob.setCropMode("fit");
        user.addImageJob(imageJob);
        ImageJob savedJob = imageJobRepository.save(imageJob);

        UsageRecord usageRecord = new UsageRecord(
                user,
                UsageType.IMAGE_GENERATION,
                creditsToUse,
                savedJob.getId().toString(),
                "Credits used for image job"
        );
        user.addUsageRecord(usageRecord);
        usageRecordRepository.save(usageRecord);
        imageJobQueuePublisher.publish(new ImageJobQueueMessage(
                savedJob.getId(),
                user.getId(),
                null,
                savedJob.getSourceImageUrl(),
                savedJob.getPrompt(),
                savedJob.getTargetWidth(),
                savedJob.getTargetHeight(),
                savedJob.getQuality(),
                savedJob.getOutputFormat(),
                savedJob.getAspectRatio(),
                savedJob.getWatermarkText(),
                savedJob.getCropMode(),
                "optimized/" + savedJob.getId() + "." + savedJob.getOutputFormat(),
                null,
                null
        ));

        return ImageJobResponse.from(savedJob);
    }

    public ImageJobResponse createGuestUploadJob(
            MultipartFile file,
            Integer width,
            Integer height,
            Integer quality,
            String aspectRatio,
            String watermarkText,
            String cropMode
    ) {
        User guestUser = userService.getOrCreateGuestUser();
        StoredFile storedFile = storageService.storeInput(file);
        String normalizedFormat = "jpg";
        String outputFilename = UUID.randomUUID() + "." + normalizedFormat;
        Path outputPath = storageService.createOutputPath(outputFilename);
        ImageJob imageJob = new ImageJob(guestUser, "Simple optimize upload", 1);
        imageJob.setSourceImageUrl(publicUrl("input", storedFile.filename()));
        imageJob.setTargetWidth(width);
        imageJob.setTargetHeight(height);
        imageJob.setQuality(resolveQuality(quality));
        imageJob.setOutputFormat(normalizedFormat);
        imageJob.setAspectRatio(normalizeAspectRatio(aspectRatio));
        imageJob.setWatermarkText(normalizeOptionalText(watermarkText));
        imageJob.setCropMode(normalizeCropMode(cropMode));
        guestUser.addImageJob(imageJob);
        ImageJob savedJob = imageJobRepository.save(imageJob);
        ImageJobQueueMessage queueMessage = new ImageJobQueueMessage(
                savedJob.getId(),
                guestUser.getId(),
                storedFile.path().toString(),
                savedJob.getSourceImageUrl(),
                savedJob.getPrompt(),
                savedJob.getTargetWidth(),
                savedJob.getTargetHeight(),
                savedJob.getQuality(),
                savedJob.getOutputFormat(),
                savedJob.getAspectRatio(),
                savedJob.getWatermarkText(),
                savedJob.getCropMode(),
                outputFilename,
                outputPath.toString(),
                publicUrl("output", outputFilename)
        );

        if ("sync".equalsIgnoreCase(processingMode)) {
            savedJob.startProcessing();
            ImageProcessingService.ProcessedImageResult result = imageProcessingService.process(queueMessage);
            savedJob.markSuccess(result.resultImageUrl(), result.outputObjectKey());
        } else {
            imageJobQueuePublisher.publish(queueMessage);
        }

        return ImageJobResponse.from(savedJob);
    }

    @Transactional(readOnly = true)
    public ImageJobResponse get(UUID imageJobId) {
        ImageJob imageJob = imageJobRepository.findById(imageJobId)
                .orElseThrow(() -> new NotFoundException("image job not found: " + imageJobId));
        return ImageJobResponse.from(imageJob);
    }

    @Transactional(readOnly = true)
    public java.util.List<ImageJobResponse> listRecent() {
        return imageJobRepository.findAll().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .limit(20)
                .map(ImageJobResponse::from)
                .toList();
    }

    public ImageJobResponse updateResult(UUID imageJobId, ImageJobResultUpdateRequest request) {
        ImageJob imageJob = imageJobRepository.findById(imageJobId)
                .orElseThrow(() -> new NotFoundException("image job not found: " + imageJobId));

        String status = request.status() == null ? "" : request.status().trim().toUpperCase();
        switch (status) {
            case "PROCESSING" -> imageJob.startProcessing();
            case "SUCCEEDED" -> imageJob.markSuccess(request.resultImageUrl(), request.outputObjectKey());
            case "FAILED" -> imageJob.markFailed(request.failureReason() == null ? "unknown failure" : request.failureReason());
            default -> throw new BadRequestException("status must be one of PROCESSING, SUCCEEDED, FAILED");
        }

        return ImageJobResponse.from(imageJob);
    }

    private String resolveOutputFormat(String outputFormat) {
        if (outputFormat == null || outputFormat.isBlank()) {
            return "webp";
        }

        String normalized = outputFormat.trim().toLowerCase();
        if (!normalized.equals("webp") && !normalized.equals("jpg") && !normalized.equals("jpeg") && !normalized.equals("png")) {
            throw new BadRequestException("outputFormat must be one of webp, jpg, jpeg, png");
        }
        return normalized;
    }

    private int resolveQuality(Integer quality) {
        int resolved = quality == null ? 80 : quality;
        if (resolved < 1 || resolved > 100) {
            throw new BadRequestException("quality must be between 1 and 100");
        }
        return resolved;
    }

    private String normalizeAspectRatio(String aspectRatio) {
        if (aspectRatio == null || aspectRatio.isBlank()) {
            return "original";
        }
        String normalized = aspectRatio.trim().toLowerCase();
        return switch (normalized) {
            case "original", "1:1", "4:5", "3:4", "16:9", "9:16" -> normalized;
            default -> throw new BadRequestException("aspectRatio must be one of original, 1:1, 4:5, 3:4, 16:9, 9:16");
        };
    }

    private String normalizeCropMode(String cropMode) {
        if (cropMode == null || cropMode.isBlank()) {
            return "fit";
        }
        String normalized = cropMode.trim().toLowerCase();
        return switch (normalized) {
            case "fit", "center-crop" -> normalized;
            default -> throw new BadRequestException("cropMode must be one of fit, center-crop");
        };
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String publicUrl(String bucket, String filename) {
        return publicBaseUrl.replaceAll("/$", "") + "/api/files/" + bucket + "/" + filename;
    }
}
