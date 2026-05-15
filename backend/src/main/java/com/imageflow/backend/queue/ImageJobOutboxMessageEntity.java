package com.imageflow.backend.queue;

import java.time.LocalDateTime;
import java.util.UUID;

import com.imageflow.backend.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "image_job_outbox_messages")
public class ImageJobOutboxMessageEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "payload", nullable = false, length = 10000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImageJobOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    protected ImageJobOutboxMessageEntity() {
    }

    public ImageJobOutboxMessageEntity(String aggregateId, String eventType, String payload) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = ImageJobOutboxStatus.PENDING;
        this.attemptCount = 0;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public ImageJobOutboxStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void markAttempt() {
        this.attemptCount += 1;
        this.lastAttemptAt = LocalDateTime.now();
    }

    public void markSent() {
        this.status = ImageJobOutboxStatus.SENT;
        this.publishedAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void markRetryableFailure(String errorMessage) {
        this.status = ImageJobOutboxStatus.PENDING;
        this.lastError = errorMessage == null ? "unknown outbox relay error" : errorMessage;
    }
}
