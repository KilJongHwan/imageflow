package com.imageflow.backend.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import com.imageflow.backend.domain.image.ImageJobService;
import com.imageflow.backend.domain.image.dto.CreateImageJobRequest;
import com.imageflow.backend.domain.image.dto.ImageJobResponse;
import com.imageflow.backend.domain.image.dto.ImageJobResultUpdateRequest;

@RestController
@RequestMapping("/api/image-jobs")
public class ImageJobController {

    private final ImageJobService imageJobService;

    public ImageJobController(ImageJobService imageJobService) {
        this.imageJobService = imageJobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ImageJobResponse create(@RequestBody CreateImageJobRequest request) {
        return imageJobService.create(request);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageJobResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Integer targetWidth,
            @RequestParam(required = false) Integer quality,
            @RequestParam(required = false) String outputFormat
    ) {
        return imageJobService.createGuestUploadJob(file, targetWidth, quality, outputFormat);
    }

    @GetMapping("/{imageJobId}")
    public ImageJobResponse get(@PathVariable UUID imageJobId) {
        return imageJobService.get(imageJobId);
    }

    @GetMapping
    public java.util.List<ImageJobResponse> listRecent() {
        return imageJobService.listRecent();
    }

    @PatchMapping("/{imageJobId}/result")
    public ImageJobResponse updateResult(
            @PathVariable UUID imageJobId,
            @RequestBody ImageJobResultUpdateRequest request
    ) {
        return imageJobService.updateResult(imageJobId, request);
    }
}
