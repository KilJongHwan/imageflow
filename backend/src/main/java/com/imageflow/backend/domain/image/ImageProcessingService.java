package com.imageflow.backend.domain.image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
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
            image = applyWatermark(
                    image,
                    message.watermarkText(),
                    message.watermarkAccentText(),
                    message.watermarkStyle(),
                    message.watermarkPosition(),
                    message.watermarkOpacity(),
                    message.watermarkScalePercent()
            );

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

    private BufferedImage applyWatermark(
            BufferedImage image,
            String watermarkText,
            String watermarkAccentText,
            String watermarkStyle,
            String watermarkPosition,
            Integer watermarkOpacity,
            Integer watermarkScalePercent
    ) {
        if (watermarkText == null || watermarkText.isBlank()) {
            return image;
        }

        BufferedImage withWatermark = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = withWatermark.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        WatermarkLayout layout = resolveWatermarkLayout(image, watermarkPosition, watermarkScalePercent, watermarkText, watermarkAccentText);
        String style = watermarkStyle == null || watermarkStyle.isBlank() ? "signature" : watermarkStyle;
        float alpha = Math.max(0.2f, Math.min(0.9f, (watermarkOpacity == null ? 56 : watermarkOpacity) / 100f));

        drawWatermarkBackground(graphics, layout, style, alpha);
        drawWatermarkText(graphics, layout, style, watermarkText, watermarkAccentText);
        graphics.dispose();
        return withWatermark;
    }

    private WatermarkLayout resolveWatermarkLayout(
            BufferedImage image,
            String watermarkPosition,
            Integer watermarkScalePercent,
            String watermarkText,
            String watermarkAccentText
    ) {
        int padding = Math.max(20, image.getWidth() / 34);
        int scalePercent = watermarkScalePercent == null ? 18 : watermarkScalePercent;
        int boxWidth = Math.max(180, Math.min(image.getWidth() - padding * 2, image.getWidth() * scalePercent / 100));
        int boxHeight = watermarkAccentText == null || watermarkAccentText.isBlank()
                ? Math.max(54, image.getHeight() / 10)
                : Math.max(74, image.getHeight() / 8);

        int x;
        int y;
        String normalizedPosition = watermarkPosition == null || watermarkPosition.isBlank() ? "bottom-right" : watermarkPosition;

        switch (normalizedPosition) {
            case "bottom-left" -> {
                x = padding;
                y = image.getHeight() - boxHeight - padding;
            }
            case "top-right" -> {
                x = image.getWidth() - boxWidth - padding;
                y = padding;
            }
            case "center" -> {
                x = (image.getWidth() - boxWidth) / 2;
                y = (image.getHeight() - boxHeight) / 2;
            }
            default -> {
                x = image.getWidth() - boxWidth - padding;
                y = image.getHeight() - boxHeight - padding;
            }
        }

        int titleFontSize = Math.max(18, boxHeight / (watermarkAccentText == null || watermarkAccentText.isBlank() ? 2 : 3));
        int accentFontSize = Math.max(12, titleFontSize - 6);
        return new WatermarkLayout(x, y, boxWidth, boxHeight, padding, titleFontSize, accentFontSize);
    }

    private void drawWatermarkBackground(Graphics2D graphics, WatermarkLayout layout, String style, float alpha) {
        Color backgroundColor = switch (style) {
            case "outline" -> new Color(15, 23, 42, Math.round(alpha * 80));
            case "badge" -> new Color(17, 24, 39, Math.round(alpha * 170));
            case "plaque" -> new Color(17, 24, 39, Math.round(alpha * 190));
            case "monogram" -> new Color(17, 24, 39, Math.round(alpha * 120));
            case "ribbon" -> new Color(31, 41, 55, Math.round(alpha * 150));
            case "pop" -> new Color(190, 24, 93, Math.round(alpha * 180));
            case "sticker" -> new Color(124, 45, 18, Math.round(alpha * 160));
            case "label" -> new Color(29, 78, 216, Math.round(alpha * 170));
            default -> new Color(15, 23, 42, Math.round(alpha * 145));
        };

        if ("monogram".equals(style)) {
            graphics.setColor(backgroundColor);
            graphics.fillOval(layout.x(), layout.y(), layout.width(), layout.height());
            return;
        }

        if ("ribbon".equals(style)) {
            graphics.setColor(backgroundColor);
            graphics.fill(new RoundRectangle2D.Float(layout.x(), layout.y(), layout.width(), layout.height(), 30, 30));
            graphics.fillRect(layout.x() + 18, layout.y() + layout.height() - 18, Math.max(40, layout.width() / 4), 14);
            return;
        }

        graphics.setColor(backgroundColor);
        graphics.fill(new RoundRectangle2D.Float(layout.x(), layout.y(), layout.width(), layout.height(), 24, 24));

        if ("outline".equals(style)) {
            graphics.setColor(new Color(191, 219, 254, 170));
            graphics.draw(new RoundRectangle2D.Float(layout.x(), layout.y(), layout.width(), layout.height(), 24, 24));
        }
    }

    private void drawWatermarkText(Graphics2D graphics, WatermarkLayout layout, String style, String watermarkText, String watermarkAccentText) {
        graphics.setFont(new Font("SansSerif", Font.BOLD, layout.titleFontSize()));
        Color titleColor = switch (style) {
            case "sticker" -> new Color(255, 247, 237);
            case "label" -> new Color(255, 255, 255);
            case "pop" -> new Color(255, 255, 255);
            default -> Color.WHITE;
        };
        graphics.setColor(titleColor);
        int titleX = layout.x() + 18;
        int titleY = layout.y() + Math.max(28, layout.titleFontSize() + 8);
        graphics.drawString(watermarkText, titleX, titleY);

        if (watermarkAccentText != null && !watermarkAccentText.isBlank()) {
            graphics.setFont(new Font("SansSerif", Font.PLAIN, layout.accentFontSize()));
            Color accentColor = switch (style) {
                case "plaque", "monogram", "ribbon" -> new Color(245, 213, 139);
                case "pop" -> new Color(253, 230, 138);
                case "label" -> new Color(191, 219, 254);
                default -> new Color(219, 234, 254);
            };
            graphics.setColor(accentColor);
            graphics.drawString(watermarkAccentText, titleX, titleY + layout.accentFontSize() + 12);
        }
    }

    private record WatermarkLayout(int x, int y, int width, int height, int padding, int titleFontSize, int accentFontSize) {
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
