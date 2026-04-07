package com.imageflow.backend.domain.image;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.imageflow.backend.common.exception.BadRequestException;
import com.imageflow.backend.queue.ImageJobQueueMessage;

@Service
public class ImageProcessingService {

    public record ProcessedImageResult(
            String resultImageUrl,
            String outputObjectKey,
            Long sourceFileSizeBytes,
            Long resultFileSizeBytes
    ) {
    }

    public ProcessedImageResult process(ImageJobQueueMessage message) {
        try {
            BufferedImage image = loadSource(message);
            image = applyCrop(image, message);
            image = resize(image, message);
            image = applyWatermark(image, message.watermarkText());

            String format = normalizeFormat(message.outputFormat());
            Path outputPath = resolveOutputPath(message.outputFilePath());
            Files.createDirectories(outputPath.getParent());
            ImageIO.write(convertIfNeeded(image, format), format, outputPath.toFile());

            Long sourceFileSizeBytes = message.sourceFilePath() == null ? null : Files.size(Path.of(message.sourceFilePath()));
            Long resultFileSizeBytes = Files.size(outputPath);
            return new ProcessedImageResult(message.resultImageUrl(), message.outputObjectKey(), sourceFileSizeBytes, resultFileSizeBytes);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to process image", exception);
        }
    }

    private BufferedImage loadSource(ImageJobQueueMessage message) throws IOException {
        if (message.sourceFilePath() != null && !message.sourceFilePath().isBlank()) {
            Path sourcePath = Path.of(message.sourceFilePath());
            if (Files.exists(sourcePath)) {
                return ImageIO.read(sourcePath.toFile());
            }
        }

        if (message.sourceImageUrl() != null && !message.sourceImageUrl().isBlank()) {
            return ImageIO.read(new URL(message.sourceImageUrl()));
        }

        throw new BadRequestException("source image path or url is required");
    }

    private BufferedImage resize(BufferedImage image, ImageJobQueueMessage message) {
        Integer width = message.targetWidth();
        Integer height = message.targetHeight();

        if (width == null && height == null) {
            return image;
        }

        int nextWidth;
        int nextHeight;

        if (width != null && height != null) {
            nextWidth = width;
            nextHeight = height;
        } else if (width != null) {
            nextWidth = width;
            nextHeight = Math.max(1, (int) Math.round((double) image.getHeight() * width / image.getWidth()));
        } else {
            nextHeight = height;
            nextWidth = Math.max(1, (int) Math.round((double) image.getWidth() * height / image.getHeight()));
        }

        BufferedImage resized = new BufferedImage(nextWidth, nextHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(image, 0, 0, nextWidth, nextHeight, null);
        graphics.dispose();
        return resized;
    }

    private BufferedImage applyCrop(BufferedImage image, ImageJobQueueMessage message) {
        if ("manual".equals(message.cropMode())) {
            return applyManualCrop(image, message);
        }

        if (!"center-crop".equals(message.cropMode()) || message.aspectRatio() == null || "original".equals(message.aspectRatio())) {
            return image;
        }

        String[] parts = message.aspectRatio().split(":");
        if (parts.length != 2) {
            throw new BadRequestException("invalid aspect ratio");
        }

        double ratioWidth = Double.parseDouble(parts[0]);
        double ratioHeight = Double.parseDouble(parts[1]);
        double targetRatio = ratioWidth / ratioHeight;
        double currentRatio = (double) image.getWidth() / image.getHeight();

        if (currentRatio > targetRatio) {
            int newWidth = (int) Math.round(image.getHeight() * targetRatio);
            int x = (image.getWidth() - newWidth) / 2;
            return image.getSubimage(x, 0, newWidth, image.getHeight());
        }

        int newHeight = (int) Math.round(image.getWidth() / targetRatio);
        int y = (image.getHeight() - newHeight) / 2;
        return image.getSubimage(0, y, image.getWidth(), newHeight);
    }

    private BufferedImage applyManualCrop(BufferedImage image, ImageJobQueueMessage message) {
        Integer cropX = message.cropX();
        Integer cropY = message.cropY();
        Integer cropWidth = message.cropWidth();
        Integer cropHeight = message.cropHeight();

        if (cropX == null || cropY == null || cropWidth == null || cropHeight == null) {
            throw new BadRequestException("manual crop requires cropX, cropY, cropWidth and cropHeight");
        }
        if (cropX < 0 || cropY < 0 || cropWidth <= 0 || cropHeight <= 0) {
            throw new BadRequestException("manual crop values are invalid");
        }
        if (cropX + cropWidth > image.getWidth() || cropY + cropHeight > image.getHeight()) {
            throw new BadRequestException("manual crop must stay within the source image bounds");
        }

        return image.getSubimage(cropX, cropY, cropWidth, cropHeight);
    }

    private BufferedImage applyWatermark(BufferedImage image, String watermarkText) {
        if (watermarkText == null || watermarkText.isBlank()) {
            return image;
        }

        BufferedImage withWatermark = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = withWatermark.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setFont(new Font("SansSerif", Font.BOLD, Math.max(16, image.getWidth() / 26)));

        int padding = Math.max(18, image.getWidth() / 40);
        int boxHeight = Math.max(42, image.getHeight() / 12);
        int boxWidth = Math.min(image.getWidth() - padding * 2, Math.max(160, watermarkText.length() * 12));
        int boxX = image.getWidth() - boxWidth - padding;
        int boxY = image.getHeight() - boxHeight - padding;

        graphics.setComposite(AlphaComposite.SrcOver.derive(0.52f));
        graphics.setColor(new Color(17, 24, 39));
        graphics.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 22, 22);

        graphics.setComposite(AlphaComposite.SrcOver);
        graphics.setColor(Color.WHITE);
        graphics.drawString(watermarkText, boxX + 18, boxY + (boxHeight / 2) + 6);
        graphics.dispose();
        return withWatermark;
    }

    private BufferedImage convertIfNeeded(BufferedImage image, String format) {
        if (format.equals("jpg") || format.equals("jpeg")) {
            BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = rgb.createGraphics();
            graphics.drawImage(image, 0, 0, Color.WHITE, null);
            graphics.dispose();
            return rgb;
        }
        return image;
    }

    private String normalizeFormat(String outputFormat) {
        if (outputFormat == null || outputFormat.isBlank()) {
            return "jpg";
        }
        String normalized = outputFormat.trim().toLowerCase();
        if (normalized.equals("jpeg")) {
            return "jpg";
        }
        if (!normalized.equals("jpg") && !normalized.equals("png")) {
            throw new BadRequestException("sync demo mode supports jpg and png output only");
        }
        return normalized;
    }

    private Path resolveOutputPath(String outputFilePath) {
        if (outputFilePath == null || outputFilePath.isBlank()) {
            throw new BadRequestException("output file path is required");
        }
        return Path.of(outputFilePath);
    }
}
