package com.imageflow.backend.domain.image;

import java.util.UUID;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.imageflow.backend.common.storage.StoredFile;
import com.imageflow.backend.common.storage.StorageService;
import com.imageflow.backend.common.storage.UploadBinary;
import com.imageflow.backend.common.exception.BadRequestException;
import com.imageflow.backend.common.exception.NotFoundException;
import com.imageflow.backend.domain.image.dto.CreateImageJobRequest;
import com.imageflow.backend.domain.image.dto.ImageJobBatchResponse;
import com.imageflow.backend.domain.image.dto.ImageJobResponse;
import com.imageflow.backend.domain.image.dto.ImageJobResultUpdateRequest;
import com.imageflow.backend.domain.usage.UsageRecord;
import com.imageflow.backend.domain.usage.UsageRecordRepository;
import com.imageflow.backend.domain.usage.UsageType;
import com.imageflow.backend.domain.user.User;
import com.imageflow.backend.queue.ImageJobQueueMessage;
import com.imageflow.backend.queue.ImageJobQueuePublisher;

@Service
@Transactional
public class ImageJobService {

    private final ImageJobRepository imageJobRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final ImageJobQueuePublisher imageJobQueuePublisher;
    private final ImageProcessingService imageProcessingService;
    private final StorageService storageService;
    private final String publicBaseUrl;
    private final String processingMode;
    private final boolean queueEnabled;
    private final int maxBatchSize;
    private final int maxZipEntryCount;
    private final long maxZipEntryBytes;
    private final long maxZipTotalBytes;

    public ImageJobService(
            ImageJobRepository imageJobRepository,
            UsageRecordRepository usageRecordRepository,
            ImageJobQueuePublisher imageJobQueuePublisher,
            ImageProcessingService imageProcessingService,
            StorageService storageService,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${app.processing.mode:sync}") String processingMode,
            @Value("${app.queue.enabled:true}") boolean queueEnabled,
            @Value("${app.upload.max-batch-size:10}") int maxBatchSize,
            @Value("${app.upload.max-zip-entry-count:32}") int maxZipEntryCount,
            @Value("${app.upload.max-zip-entry-bytes:20971520}") long maxZipEntryBytes,
            @Value("${app.upload.max-zip-total-bytes:52428800}") long maxZipTotalBytes
    ) {
        this.imageJobRepository = imageJobRepository;
        this.usageRecordRepository = usageRecordRepository;
        this.imageJobQueuePublisher = imageJobQueuePublisher;
        this.imageProcessingService = imageProcessingService;
        this.storageService = storageService;
        this.publicBaseUrl = publicBaseUrl;
        this.processingMode = processingMode;
        this.queueEnabled = queueEnabled;
        this.maxBatchSize = maxBatchSize;
        this.maxZipEntryCount = maxZipEntryCount;
        this.maxZipEntryBytes = maxZipEntryBytes;
        this.maxZipTotalBytes = maxZipTotalBytes;
    }

    public ImageJobResponse create(User user, CreateImageJobRequest request) {
        validatePromptGenerationMode();
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
                savedJob.getWatermarkAccentText(),
                savedJob.getWatermarkStyle(),
                savedJob.getWatermarkPosition(),
                savedJob.getWatermarkOpacity(),
                savedJob.getWatermarkScalePercent(),
                savedJob.getCropMode(),
                savedJob.getCropX(),
                savedJob.getCropY(),
                savedJob.getCropWidth(),
                savedJob.getCropHeight(),
                "optimized/" + savedJob.getId() + "." + savedJob.getOutputFormat(),
                null,
                null
        ));

        return ImageJobResponse.from(savedJob);
    }

    public ImageJobResponse createUploadJob(
            User user,
            MultipartFile file,
            Integer width,
            Integer height,
            Integer quality,
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
            Integer cropHeight
    ) {
        UploadBinary uploadBinary = readImageUpload(file);
        return createUploadJob(
                user,
                uploadBinary,
                width,
                height,
                quality,
                aspectRatio,
                watermarkText,
                watermarkAccentText,
                watermarkStyle,
                watermarkPosition,
                watermarkOpacity,
                watermarkScalePercent,
                cropMode,
                cropX,
                cropY,
                cropWidth,
                cropHeight
        );
    }

    private ImageJobResponse createUploadJob(
            User user,
            UploadBinary uploadBinary,
            Integer width,
            Integer height,
            Integer quality,
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
            Integer cropHeight
    ) {
        StoredFile storedFile = storageService.storeInput(uploadBinary);
        String normalizedFormat = "jpg";
        String outputFilename = UUID.randomUUID() + "." + normalizedFormat;
        Path outputPath = storageService.createOutputPath(outputFilename);
        ImageJob imageJob = new ImageJob(user, "Simple optimize upload", 1);
        imageJob.setSourceImageUrl(publicUrl("input", storedFile.filename()));
        imageJob.setTargetWidth(width);
        imageJob.setTargetHeight(height);
        imageJob.setQuality(resolveQuality(quality));
        imageJob.setOutputFormat(normalizedFormat);
        imageJob.setAspectRatio(normalizeAspectRatio(aspectRatio));
        imageJob.setWatermarkText(normalizeOptionalText(watermarkText));
        imageJob.setWatermarkAccentText(normalizeOptionalText(watermarkAccentText));
        imageJob.setWatermarkStyle(normalizeWatermarkStyle(watermarkStyle));
        imageJob.setWatermarkPosition(normalizeWatermarkPosition(watermarkPosition));
        imageJob.setWatermarkOpacity(resolveWatermarkOpacity(watermarkOpacity));
        imageJob.setWatermarkScalePercent(resolveWatermarkScalePercent(watermarkScalePercent));
        imageJob.setCropMode(normalizeCropMode(cropMode));
        imageJob.setCropX(cropX);
        imageJob.setCropY(cropY);
        imageJob.setCropWidth(cropWidth);
        imageJob.setCropHeight(cropHeight);
        imageJob.setSourceFileSizeBytes(uploadBinary.size());
        validateManualCrop(imageJob.getCropMode(), cropX, cropY, cropWidth, cropHeight);
        user.chargeCredits(1);
        user.addImageJob(imageJob);
        ImageJob savedJob = imageJobRepository.save(imageJob);
        usageRecordRepository.save(new UsageRecord(
                user,
                UsageType.IMAGE_OPTIMIZATION,
                1,
                savedJob.getId().toString(),
                "Credits used for image optimization job"
        ));
        ImageJobQueueMessage queueMessage = new ImageJobQueueMessage(
                savedJob.getId(),
                user.getId(),
                storedFile.path().toString(),
                savedJob.getSourceImageUrl(),
                savedJob.getPrompt(),
                savedJob.getTargetWidth(),
                savedJob.getTargetHeight(),
                savedJob.getQuality(),
                savedJob.getOutputFormat(),
                savedJob.getAspectRatio(),
                savedJob.getWatermarkText(),
                savedJob.getWatermarkAccentText(),
                savedJob.getWatermarkStyle(),
                savedJob.getWatermarkPosition(),
                savedJob.getWatermarkOpacity(),
                savedJob.getWatermarkScalePercent(),
                savedJob.getCropMode(),
                savedJob.getCropX(),
                savedJob.getCropY(),
                savedJob.getCropWidth(),
                savedJob.getCropHeight(),
                outputFilename,
                outputPath.toString(),
                publicUrl("output", outputFilename)
        );

        if ("sync".equalsIgnoreCase(processingMode)) {
            savedJob.startProcessing();
            ImageProcessingService.ProcessedImageResult result = imageProcessingService.process(queueMessage);
            savedJob.markSuccess(result.resultImageUrl(), result.outputObjectKey(), result.sourceFileSizeBytes(), result.resultFileSizeBytes());
        } else {
            imageJobQueuePublisher.publish(queueMessage);
        }

        return ImageJobResponse.from(savedJob);
    }

    public ImageJobBatchResponse createUploadJobs(
            User user,
            List<MultipartFile> files,
            Integer width,
            Integer height,
            Integer quality,
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
            Integer cropHeight
    ) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("at least one image file is required");
        }
        List<UploadBinary> uploadBinaries = expandUploadFiles(files);
        if (uploadBinaries.size() > maxBatchSize) {
            throw new BadRequestException("batch upload supports up to " + maxBatchSize + " images");
        }
        if ("manual".equals(normalizeCropMode(cropMode)) && uploadBinaries.size() > 1) {
            throw new BadRequestException("manual crop is available only when uploading a single image");
        }

        List<ImageJobResponse> jobs = new ArrayList<>();
        for (UploadBinary uploadBinary : uploadBinaries) {
            jobs.add(createUploadJob(
                    user,
                    uploadBinary,
                    width,
                    height,
                    quality,
                    aspectRatio,
                    watermarkText,
                    watermarkAccentText,
                    watermarkStyle,
                    watermarkPosition,
                    watermarkOpacity,
                    watermarkScalePercent,
                    cropMode,
                    cropX,
                    cropY,
                    cropWidth,
                    cropHeight
            ));
        }
        return ImageJobBatchResponse.from(jobs);
    }

    @Transactional(readOnly = true)
    public ImageJobResponse get(User user, UUID imageJobId) {
        ImageJob imageJob = imageJobRepository.findByIdAndUser(imageJobId, user)
                .orElseThrow(() -> new NotFoundException("image job not found: " + imageJobId));
        return ImageJobResponse.from(imageJob);
    }

    @Transactional(readOnly = true)
    public java.util.List<ImageJobResponse> listRecent(User user) {
        return imageJobRepository.findTop20ByUserOrderByCreatedAtDesc(user).stream()
                .map(ImageJobResponse::from)
                .toList();
    }

    public ImageJobResponse updateResult(UUID imageJobId, ImageJobResultUpdateRequest request) {
        ImageJob imageJob = imageJobRepository.findById(imageJobId)
                .orElseThrow(() -> new NotFoundException("image job not found: " + imageJobId));

        String status = request.status() == null ? "" : request.status().trim().toUpperCase();
        switch (status) {
            case "PROCESSING" -> imageJob.startProcessing();
            case "SUCCEEDED" -> imageJob.markSuccess(
                    request.resultImageUrl(),
                    request.outputObjectKey(),
                    request.sourceFileSizeBytes(),
                    request.resultFileSizeBytes()
            );
            case "FAILED" -> imageJob.markFailed(request.failureReason() == null ? "unknown failure" : request.failureReason());
            default -> throw new BadRequestException("status must be one of PROCESSING, SUCCEEDED, FAILED");
        }

        return ImageJobResponse.from(imageJob);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> downloadJobs(User user, List<UUID> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            throw new BadRequestException("jobIds is required");
        }
        if (jobIds.size() > maxBatchSize) {
            throw new BadRequestException("download supports up to " + maxBatchSize + " files at once");
        }

        List<ImageJob> jobs = imageJobRepository.findAllByIdInAndUser(jobIds, user);
        if (jobs.size() != jobIds.size()) {
            throw new NotFoundException("one or more image jobs were not found");
        }

        Set<String> usedNames = new HashSet<>();

        List<DownloadableResultFile> downloadableFiles = new ArrayList<>();
        for (ImageJob job : jobs) {
            if (job.getResultImageUrl() == null || job.getResultImageUrl().isBlank()) {
                throw new BadRequestException("all selected jobs must have a downloadable result");
            }

            Path outputFile = storageService.resolveOutputFile(resolveOutputFilename(job));
            if (!Files.exists(outputFile)) {
                throw new NotFoundException("optimized file not found for job: " + job.getId());
            }

            downloadableFiles.add(new DownloadableResultFile(uniqueEntryName(job, usedNames), outputFile));
        }

        StreamingResponseBody responseBody = outputStream -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                for (DownloadableResultFile downloadableFile : downloadableFiles) {
                    zipOutputStream.putNextEntry(new ZipEntry(downloadableFile.entryName()));
                    Files.copy(downloadableFile.path(), zipOutputStream);
                    zipOutputStream.closeEntry();
                }
                zipOutputStream.finish();
            } catch (IOException exception) {
                throw new IllegalStateException("failed to stream batch download", exception);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"imageflow-batch.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(responseBody);
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
            case "fit", "center-crop", "manual" -> normalized;
            default -> throw new BadRequestException("cropMode must be one of fit, center-crop, manual");
        };
    }

    private String normalizeWatermarkStyle(String watermarkStyle) {
        if (watermarkStyle == null || watermarkStyle.isBlank()) {
            return null;
        }
        String normalized = watermarkStyle.trim().toLowerCase();
        return switch (normalized) {
            case "signature", "outline", "badge", "plaque", "monogram", "ribbon", "pop", "sticker", "label" -> normalized;
            default -> throw new BadRequestException("watermarkStyle is not supported");
        };
    }

    private String normalizeWatermarkPosition(String watermarkPosition) {
        if (watermarkPosition == null || watermarkPosition.isBlank()) {
            return "bottom-right";
        }
        String normalized = watermarkPosition.trim().toLowerCase();
        return switch (normalized) {
            case "bottom-right", "bottom-left", "top-right", "center" -> normalized;
            default -> throw new BadRequestException("watermarkPosition must be one of bottom-right, bottom-left, top-right, center");
        };
    }

    private Integer resolveWatermarkOpacity(Integer watermarkOpacity) {
        if (watermarkOpacity == null) {
            return 56;
        }
        if (watermarkOpacity < 20 || watermarkOpacity > 90) {
            throw new BadRequestException("watermarkOpacity must be between 20 and 90");
        }
        return watermarkOpacity;
    }

    private Integer resolveWatermarkScalePercent(Integer watermarkScalePercent) {
        if (watermarkScalePercent == null) {
            return 18;
        }
        if (watermarkScalePercent < 10 || watermarkScalePercent > 40) {
            throw new BadRequestException("watermarkScalePercent must be between 10 and 40");
        }
        return watermarkScalePercent;
    }

    private void validateManualCrop(String cropMode, Integer cropX, Integer cropY, Integer cropWidth, Integer cropHeight) {
        if (!"manual".equals(cropMode)) {
            return;
        }
        if (cropX == null || cropY == null || cropWidth == null || cropHeight == null) {
            throw new BadRequestException("manual crop requires cropX, cropY, cropWidth and cropHeight");
        }
        if (cropX < 0 || cropY < 0) {
            throw new BadRequestException("cropX and cropY must be zero or greater");
        }
        if (cropWidth <= 0 || cropHeight <= 0) {
            throw new BadRequestException("cropWidth and cropHeight must be greater than zero");
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void validatePromptGenerationMode() {
        if ("sync".equalsIgnoreCase(processingMode) || !queueEnabled) {
            throw new BadRequestException("prompt-based image generation is unavailable in sync mode");
        }
    }

    private String publicUrl(String bucket, String filename) {
        return publicBaseUrl.replaceAll("/$", "") + "/api/files/" + bucket + "/" + filename;
    }

    private UploadBinary readImageUpload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (!storageService.isSupportedImageFilename(originalFilename)) {
            throw new BadRequestException("only jpg, jpeg, png and webp images are allowed");
        }

        try {
            byte[] bytes = file.getBytes();
            storageService.validateImageBytes(originalFilename, bytes);
            return new UploadBinary(originalFilename, bytes);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read uploaded image", exception);
        }
    }

    private List<UploadBinary> expandUploadFiles(List<MultipartFile> files) {
        List<UploadBinary> uploadBinaries = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            if (storageService.isZipFilename(originalFilename)) {
                uploadBinaries.addAll(extractZipImages(file));
                continue;
            }
            uploadBinaries.add(readImageUpload(file));
        }

        if (uploadBinaries.isEmpty()) {
            throw new BadRequestException("no supported image files were found");
        }

        return uploadBinaries;
    }

    private List<UploadBinary> extractZipImages(MultipartFile archive) {
        List<UploadBinary> uploadBinaries = new ArrayList<>();
        long totalUncompressedBytes = 0;
        int processedEntries = 0;

        try {
            byte[] archiveBytes = archive.getBytes();
            storageService.validateZipBytes(archive.getOriginalFilename(), archiveBytes);

            try (ZipInputStream zipInputStream = new ZipInputStream(new java.io.ByteArrayInputStream(archiveBytes))) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    processedEntries++;
                    if (processedEntries > maxZipEntryCount) {
                        throw new BadRequestException("zip archive contains too many entries");
                    }

                    if (entry.isDirectory()) {
                        continue;
                    }

                    String entryName = extractEntryFilename(entry.getName());
                    if (entryName.isBlank() || entryName.startsWith(".")) {
                        continue;
                    }
                    if (!storageService.isSupportedImageFilename(entryName)) {
                        if (entryName.startsWith("__MACOSX")) {
                            continue;
                        }
                        throw new BadRequestException("zip archive contains unsupported file: " + entryName);
                    }

                    byte[] bytes = readZipEntryBytes(zipInputStream, entryName);
                    totalUncompressedBytes += bytes.length;
                    if (totalUncompressedBytes > maxZipTotalBytes) {
                        throw new BadRequestException("zip archive is too large after extraction");
                    }
                    if (bytes.length == 0) {
                        continue;
                    }
                    storageService.validateImageBytes(entryName, bytes);
                    uploadBinaries.add(new UploadBinary(entryName, bytes));
                    if (uploadBinaries.size() > maxBatchSize) {
                        throw new BadRequestException("batch upload supports up to " + maxBatchSize + " images");
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read zip archive", exception);
        }

        if (uploadBinaries.isEmpty()) {
            throw new BadRequestException("zip archive does not contain supported image files");
        }

        return uploadBinaries;
    }

    private byte[] readZipEntryBytes(ZipInputStream zipInputStream, String entryName) throws IOException {
        byte[] buffer = new byte[8192];
        long totalBytes = 0;

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            int read;
            while ((read = zipInputStream.read(buffer)) != -1) {
                totalBytes += read;
                if (totalBytes > maxZipEntryBytes) {
                    throw new BadRequestException("zip entry is too large: " + entryName);
                }
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    private String extractEntryFilename(String entryName) {
        if (entryName == null || entryName.isBlank()) {
            return "";
        }
        String normalized = entryName.replace("\\", "/");
        int slashIndex = normalized.lastIndexOf('/');
        return slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
    }

    private String resolveOutputFilename(ImageJob job) {
        if (job.getOutputObjectKey() == null || job.getOutputObjectKey().isBlank()) {
            throw new BadRequestException("output object key is missing for job: " + job.getId());
        }

        String outputObjectKey = job.getOutputObjectKey().replace("\\", "/");
        int slashIndex = outputObjectKey.lastIndexOf('/');
        return slashIndex >= 0 ? outputObjectKey.substring(slashIndex + 1) : outputObjectKey;
    }

    private String uniqueEntryName(ImageJob job, Set<String> usedNames) {
        String filename = resolveOutputFilename(job);
        if (usedNames.add(filename)) {
            return filename;
        }

        int extensionIndex = filename.lastIndexOf('.');
        String base = extensionIndex >= 0 ? filename.substring(0, extensionIndex) : filename;
        String extension = extensionIndex >= 0 ? filename.substring(extensionIndex) : "";
        String uniqueName = base + "-" + job.getId() + extension;
        usedNames.add(uniqueName);
        return uniqueName;
    }

    private record DownloadableResultFile(String entryName, Path path) {
    }
}
