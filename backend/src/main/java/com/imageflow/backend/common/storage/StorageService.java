package com.imageflow.backend.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.Set;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.imageflow.backend.common.config.StorageProperties;
import com.imageflow.backend.common.exception.BadRequestException;

import jakarta.annotation.PostConstruct;

@Service
public class StorageService {

    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
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

        try {
            return storeInput(file.getOriginalFilename(), file.getBytes());
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read uploaded file", exception);
        }
    }

    public StoredFile storeInput(UploadBinary uploadBinary) {
        return storeInput(uploadBinary.originalFilename(), uploadBinary.bytes());
    }

    public StoredFile storeInput(String originalFilename, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("image file is required");
        }

        if (!isSupportedImageFilename(originalFilename)) {
            throw new BadRequestException("only jpg, jpeg, png and webp images are allowed");
        }
        validateImageBytes(originalFilename, bytes);

        String extension = resolveExtension(originalFilename);
        String filename = UUID.randomUUID() + extension;
        Path targetPath = getInputRoot().resolve(filename).normalize();

        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
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

    public boolean isSupportedImageFilename(String originalFilename) {
        return SUPPORTED_IMAGE_EXTENSIONS.contains(resolveExtension(originalFilename));
    }

    public boolean isZipFilename(String originalFilename) {
        return ".zip".equals(resolveExtension(originalFilename));
    }

    public void validateImageBytes(String originalFilename, byte[] bytes) {
        String extension = resolveExtension(originalFilename);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                if (".webp".equals(extension) && looksLikeWebp(bytes)) {
                    return;
                }
                throw new BadRequestException("uploaded file is not a valid image: " + originalFilename);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to validate uploaded image", exception);
        }
    }

    public void validateZipBytes(String originalFilename, byte[] bytes) {
        if (!isZipFilename(originalFilename)) {
            throw new BadRequestException("zip file is required");
        }
        if (!looksLikeZip(bytes)) {
            throw new BadRequestException("uploaded zip file is invalid: " + originalFilename);
        }
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".bin";
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        return extension.length() > 10 ? ".bin" : extension;
    }

    private boolean looksLikeZip(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == 0x50
                && bytes[1] == 0x4B
                && bytes[2] == 0x03
                && bytes[3] == 0x04;
    }

    private boolean looksLikeWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';
    }
}
