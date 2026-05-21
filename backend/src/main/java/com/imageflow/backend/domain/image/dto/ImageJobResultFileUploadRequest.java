package com.imageflow.backend.domain.image.dto;

import org.springframework.web.multipart.MultipartFile;

public record ImageJobResultFileUploadRequest(
        MultipartFile file,
        String outputObjectKey,
        String resultImageUrl,
        Long sourceFileSizeBytes,
        Long resultFileSizeBytes
) {
}
