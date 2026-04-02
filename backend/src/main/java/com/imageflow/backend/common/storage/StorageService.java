package com.imageflow.backend.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.imageflow.backend.common.config.StorageProperties;
import com.imageflow.backend.common.exception.BadRequestException;

import jakarta.annotation.PostConstruct;

@Service
public class StorageService {

    private final Path rootPath;

    public StorageService(StorageProperties storageProperties) {
        this.rootPath = Path.of(storageProperties.getRoot()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void initDirectories() {
        try {
            Files.createDirectories(getInputRoot());
            Files.createDirectories(getOutputRoot());
        } catch (IOException exception) {
            throw new IllegalStateException("failed to initialize storage directories", exception);
        }
    }

    public StoredFile storeInput(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("image file is required");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;
        Path targetPath = getInputRoot().resolve(filename).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(filename, targetPath);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to store uploaded file", exception);
        }
    }

    public Path createOutputPath(String filename) {
        return getOutputRoot().resolve(filename).normalize();
    }

    public Path resolveInputFile(String filename) {
        return getInputRoot().resolve(filename).normalize();
    }

    public Path resolveOutputFile(String filename) {
        return getOutputRoot().resolve(filename).normalize();
    }

    public Path getInputRoot() {
        return rootPath.resolve("input");
    }

    public Path getOutputRoot() {
        return rootPath.resolve("output");
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".bin";
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        return extension.length() > 10 ? ".bin" : extension;
    }
}
