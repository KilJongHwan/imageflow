package com.imageflow.backend.api;

import java.util.UUID;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.imageflow.backend.domain.image.ImageJobService;
import com.imageflow.backend.domain.image.dto.CreateImageJobRequest;
import com.imageflow.backend.domain.image.dto.ImageJobBatchResponse;
import com.imageflow.backend.domain.image.dto.ImageJobResultFileUploadRequest;
import com.imageflow.backend.domain.image.dto.ImageJobResponse;
import com.imageflow.backend.domain.image.dto.ImageJobResultUpdateRequest;
import com.imageflow.backend.common.ops.QueueBackpressureService;
import com.imageflow.backend.common.ops.RateLimitService;
import com.imageflow.backend.domain.auth.AuthService;
import com.imageflow.backend.domain.user.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/image-jobs")
@Tag(name = "Image Jobs", description = "Upload, queue, poll, and download image optimization jobs")
public class ImageJobController {

    private final ImageJobService imageJobService;
    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final QueueBackpressureService queueBackpressureService;
    private final int uploadRequestsPerMinute;

    public ImageJobController(
            ImageJobService imageJobService,
            AuthService authService,
            RateLimitService rateLimitService,
            QueueBackpressureService queueBackpressureService,
            @Value("${app.rate-limit.upload-requests-per-minute:30}") int uploadRequestsPerMinute
    ) {
        this.imageJobService = imageJobService;
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.queueBackpressureService = queueBackpressureService;
        this.uploadRequestsPerMinute = uploadRequestsPerMinute;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create prompt-based image job", description = "Creates a queue-backed image generation or optimization job from JSON input.")
    public ImageJobResponse create(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody CreateImageJobRequest request
    ) {
        User user = currentUser(authorizationHeader);
        rateLimitService.checkLimit("image-job-create", user.getId().toString(), uploadRequestsPerMinute);
        queueBackpressureService.checkWritable();
        return imageJobService.create(user, request);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a single image", description = "Uploads one image and creates a queue-backed optimization job.")
    public ImageJobResponse upload(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) MultipartFile watermarkImage,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height,
            @RequestParam(required = false) Integer quality,
            @RequestParam(required = false) String aspectRatio,
            @RequestParam(required = false) String watermarkText,
            @RequestParam(required = false) String watermarkFontFamily,
            @RequestParam(required = false) String watermarkAccentText,
            @RequestParam(required = false) String watermarkStyle,
            @RequestParam(required = false) String watermarkPosition,
            @RequestParam(required = false) Integer watermarkOpacity,
            @RequestParam(required = false) Integer watermarkScalePercent,
            @RequestParam(required = false) String cropMode,
            @RequestParam(required = false) Integer cropX,
            @RequestParam(required = false) Integer cropY,
            @RequestParam(required = false) Integer cropWidth,
            @RequestParam(required = false) Integer cropHeight
    ) {
        User user = currentUser(authorizationHeader);
        rateLimitService.checkLimit("image-job-upload", user.getId().toString(), uploadRequestsPerMinute);
        queueBackpressureService.checkWritable();
        return imageJobService.createUploadJob(
                user,
                file,
                watermarkImage,
                width,
                height,
                quality,
                aspectRatio,
                watermarkText,
                watermarkFontFamily,
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

    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload multiple images or ZIP", description = "Uploads multiple image files or one ZIP archive and creates a batch of optimization jobs.")
    public ImageJobBatchResponse uploadMany(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) MultipartFile watermarkImage,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height,
            @RequestParam(required = false) Integer quality,
            @RequestParam(required = false) String aspectRatio,
            @RequestParam(required = false) String watermarkText,
            @RequestParam(required = false) String watermarkFontFamily,
            @RequestParam(required = false) String watermarkAccentText,
            @RequestParam(required = false) String watermarkStyle,
            @RequestParam(required = false) String watermarkPosition,
            @RequestParam(required = false) Integer watermarkOpacity,
            @RequestParam(required = false) Integer watermarkScalePercent,
            @RequestParam(required = false) String cropMode,
            @RequestParam(required = false) Integer cropX,
            @RequestParam(required = false) Integer cropY,
            @RequestParam(required = false) Integer cropWidth,
            @RequestParam(required = false) Integer cropHeight
    ) {
        User user = currentUser(authorizationHeader);
        rateLimitService.checkLimit("image-job-uploads", user.getId().toString(), uploadRequestsPerMinute);
        queueBackpressureService.checkWritable();
        return imageJobService.createUploadJobs(
                user,
                files,
                watermarkImage,
                width,
                height,
                quality,
                aspectRatio,
                watermarkText,
                watermarkFontFamily,
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

    @GetMapping("/{imageJobId:[0-9a-fA-F\\-]{36}}")
    @Operation(summary = "Get image job", description = "Returns the current state and output metadata of a single job.")
    public ImageJobResponse get(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable UUID imageJobId
    ) {
        return imageJobService.get(currentUser(authorizationHeader), imageJobId);
    }

    @GetMapping
    @Operation(summary = "List recent jobs", description = "Returns the recent jobs of the authenticated user.")
    public java.util.List<ImageJobResponse> listRecent(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return imageJobService.listRecent(currentUser(authorizationHeader));
    }

    @GetMapping("/download")
    @Operation(summary = "Download batch ZIP", description = "Downloads multiple completed job outputs as a streamed ZIP archive.")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam("jobIds") List<UUID> jobIds
    ) {
        return imageJobService.downloadJobs(currentUser(authorizationHeader), jobIds);
    }

    @PatchMapping("/{imageJobId:[0-9a-fA-F\\-]{36}}/result")
    @Operation(summary = "Update image job result", description = "Worker callback endpoint that updates PROCESSING, SUCCEEDED, or FAILED status.")
    public ImageJobResponse updateResult(
            @PathVariable UUID imageJobId,
            @RequestBody ImageJobResultUpdateRequest request
    ) {
        return imageJobService.updateResult(imageJobId, request);
    }

    @PatchMapping(value = "/{imageJobId:[0-9a-fA-F\\-]{36}}/result-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload optimized result file", description = "Worker callback endpoint that uploads the optimized file back to the backend when storage is not shared.")
    public ImageJobResponse uploadResultFile(
            @PathVariable UUID imageJobId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String outputObjectKey,
            @RequestParam(required = false) String resultImageUrl,
            @RequestParam(required = false) Long sourceFileSizeBytes,
            @RequestParam(required = false) Long resultFileSizeBytes
    ) {
        return imageJobService.uploadResultFile(
                imageJobId,
                new ImageJobResultFileUploadRequest(
                        file,
                        outputObjectKey,
                        resultImageUrl,
                        sourceFileSizeBytes,
                        resultFileSizeBytes
                )
        );
    }

    private User currentUser(String authorizationHeader) {
        return authService.requireAuthenticatedUser(authorizationHeader);
    }
}
