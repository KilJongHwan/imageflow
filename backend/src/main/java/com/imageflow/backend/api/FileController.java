package com.imageflow.backend.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.imageflow.backend.common.exception.NotFoundException;
import com.imageflow.backend.common.storage.StorageService;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/{bucket}/{filename:.+}")
    public ResponseEntity<Resource> getFile(
            @PathVariable String bucket,
            @PathVariable String filename
    ) {
        Path path = switch (bucket) {
            case "input" -> storageService.resolveInputFile(filename);
            case "output" -> storageService.resolveOutputFile(filename);
            default -> throw new NotFoundException("file bucket not found: " + bucket);
        };

        if (!Files.exists(path)) {
            throw new NotFoundException("file not found: " + filename);
        }

        try {
            String contentType = Files.probeContentType(path);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType)
                    .body(new FileSystemResource(path));
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read file metadata", exception);
        }
    }
}
