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

    @Column(nullable = false, length = 1000)
    private String prompt;

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

    public String getPrompt() {
        return prompt;
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
