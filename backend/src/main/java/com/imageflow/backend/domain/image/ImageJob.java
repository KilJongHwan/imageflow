package com.imageflow.backend.domain.image;

import java.util.UUID;

import com.imageflow.backend.common.entity.BaseTimeEntity;
import com.imageflow.backend.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "image_jobs")
public class ImageJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "source_image_url", length = 500)
    private String sourceImageUrl;

    @Column(name = "result_image_url", length = 500)
    private String resultImageUrl;

    @Column(name = "output_object_key", length = 255)
    private String outputObjectKey;

    @Column(nullable = false, length = 1000)
    private String prompt;

    @Column(name = "target_width")
    private Integer targetWidth;

    @Column(name = "target_height")
    private Integer targetHeight;

    @Column(nullable = false)
    private int quality;

    @Column(name = "output_format", nullable = false, length = 20)
    private String outputFormat;

    @Column(name = "aspect_ratio", length = 20)
    private String aspectRatio;

    @Column(name = "watermark_text", length = 120)
    private String watermarkText;

    @Column(name = "crop_mode", length = 20)
    private String cropMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImageJobStatus status;

    @Column(name = "credits_used", nullable = false)
    private int creditsUsed;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    protected ImageJob() {
    }

    public ImageJob(User user, String prompt, int creditsUsed) {
        this.user = user;
        this.prompt = prompt;
        this.creditsUsed = creditsUsed;
        this.status = ImageJobStatus.QUEUED;
        this.quality = 80;
        this.outputFormat = "webp";
    }

    public void startProcessing() {
        this.status = ImageJobStatus.PROCESSING;
        this.failureReason = null;
    }

    public void markSuccess(String resultImageUrl) {
        this.status = ImageJobStatus.SUCCEEDED;
        this.resultImageUrl = resultImageUrl;
        this.failureReason = null;
    }

    public void markSuccess(String resultImageUrl, String outputObjectKey) {
        this.status = ImageJobStatus.SUCCEEDED;
        this.resultImageUrl = resultImageUrl;
        this.outputObjectKey = outputObjectKey;
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = ImageJobStatus.FAILED;
        this.failureReason = failureReason;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getSourceImageUrl() {
        return sourceImageUrl;
    }

    public void setSourceImageUrl(String sourceImageUrl) {
        this.sourceImageUrl = sourceImageUrl;
    }

    public String getResultImageUrl() {
        return resultImageUrl;
    }

    public String getOutputObjectKey() {
        return outputObjectKey;
    }

    public String getPrompt() {
        return prompt;
    }

    public Integer getTargetWidth() {
        return targetWidth;
    }

    public void setTargetWidth(Integer targetWidth) {
        this.targetWidth = targetWidth;
    }

    public Integer getTargetHeight() {
        return targetHeight;
    }

    public void setTargetHeight(Integer targetHeight) {
        this.targetHeight = targetHeight;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public String getWatermarkText() {
        return watermarkText;
    }

    public void setWatermarkText(String watermarkText) {
        this.watermarkText = watermarkText;
    }

    public String getCropMode() {
        return cropMode;
    }

    public void setCropMode(String cropMode) {
        this.cropMode = cropMode;
    }

    public ImageJobStatus getStatus() {
        return status;
    }

    public int getCreditsUsed() {
        return creditsUsed;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
