package com.imageflow.backend.api;

import java.util.UUID;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import com.imageflow.backend.domain.image.dto.ImageJobResponse;
import com.imageflow.backend.domain.image.dto.ImageJobResultUpdateRequest;
import com.imageflow.backend.domain.auth.AuthService;
import com.imageflow.backend.domain.user.User;

@RestController
@RequestMapping("/api/image-jobs")
public class ImageJobController {

    private final ImageJobService imageJobService;
    private final AuthService authService;

    public ImageJobController(ImageJobService imageJobService, AuthService authService) {
        this.imageJobService = imageJobService;
        this.authService = authService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ImageJobResponse create(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody CreateImageJobRequest request
    ) {
        return imageJobService.create(currentUser(authorizationHeader), request);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
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
        return imageJobService.createUploadJob(
                currentUser(authorizationHeader),
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
        return imageJobService.createUploadJobs(
                currentUser(authorizationHeader),
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

    @GetMapping("/{imageJobId}")
    public ImageJobResponse get(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable UUID imageJobId
    ) {
        return imageJobService.get(currentUser(authorizationHeader), imageJobId);
    }

    @GetMapping
    public java.util.List<ImageJobResponse> listRecent(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        return imageJobService.listRecent(currentUser(authorizationHeader));
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam("jobIds") List<UUID> jobIds
    ) {
        return imageJobService.downloadJobs(currentUser(authorizationHeader), jobIds);
    }

    @PatchMapping("/{imageJobId}/result")
    public ImageJobResponse updateResult(
            @PathVariable UUID imageJobId,
            @RequestBody ImageJobResultUpdateRequest request
    ) {
        return imageJobService.updateResult(imageJobId, request);
    }

    private User currentUser(String authorizationHeader) {
        return authService.requireAuthenticatedUser(authorizationHeader);
    }
}
