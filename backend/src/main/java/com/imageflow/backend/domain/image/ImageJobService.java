package com.imageflow.backend.domain.image;

import java.util.UUID;
import java.nio.file.Path;

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
    private final UserService userService;
    private final StorageService storageService;
    private final String publicBaseUrl;

    public ImageJobService(
            UserRepository userRepository,
            ImageJobRepository imageJobRepository,
            UsageRecordRepository usageRecordRepository,
            ImageJobQueuePublisher imageJobQueuePublisher,
            UserService userService,
            StorageService storageService,
            @org.springframework.beans.factory.annotation.Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.userRepository = userRepository;
        this.imageJobRepository = imageJobRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.imageJobQueuePublisher = imageJobQueuePublisher;
        this.userService = userService;
        this.storageService = storageService;
        this.publicBaseUrl = publicBaseUrl;
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
        imageJob.setQuality(quality);
        imageJob.setOutputFormat(resolveOutputFormat(request.outputFormat()));
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
                savedJob.getQuality(),
                savedJob.getOutputFormat(),
                "optimized/" + savedJob.getId() + "." + savedJob.getOutputFormat(),
                null,
                null
        ));

        return ImageJobResponse.from(savedJob);
    }

    public ImageJobResponse createGuestUploadJob(
            MultipartFile file,
            Integer targetWidth,
            Integer quality,
            String outputFormat
    ) {
        User guestUser = userService.getOrCreateGuestUser();
        StoredFile storedFile = storageService.storeInput(file);
        String normalizedFormat = resolveOutputFormat(outputFormat);
        String outputFilename = UUID.randomUUID() + "." + normalizedFormat;
        Path outputPath = storageService.createOutputPath(outputFilename);
        ImageJob imageJob = new ImageJob(guestUser, "Simple optimize upload", 1);
        imageJob.setSourceImageUrl(publicUrl("input", storedFile.filename()));
        imageJob.setTargetWidth(targetWidth);
        imageJob.setQuality(quality == null ? 80 : quality);
        imageJob.setOutputFormat(normalizedFormat);
        guestUser.addImageJob(imageJob);
        ImageJob savedJob = imageJobRepository.save(imageJob);
        imageJobQueuePublisher.publish(new ImageJobQueueMessage(
                savedJob.getId(),
                guestUser.getId(),
                storedFile.path().toString(),
                savedJob.getSourceImageUrl(),
                savedJob.getPrompt(),
                savedJob.getTargetWidth(),
                savedJob.getQuality(),
                savedJob.getOutputFormat(),
                outputFilename,
                outputPath.toString(),
                publicUrl("output", outputFilename)
        ));

        System.out.println("get response :: " + savedJob.toString());

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

    private String publicUrl(String bucket, String filename) {
        return publicBaseUrl.replaceAll("/$", "") + "/api/files/" + bucket + "/" + filename;
    }
}
