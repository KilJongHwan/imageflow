package com.imageflow.backend.queue;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ImageJobOutboxRelay {

    private final ImageJobOutboxRepository imageJobOutboxRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final boolean queueEnabled;
    private final String queueKey;
    private final int batchSize;
    private final int retryAttempts;
    private final long retryDelayMillis;

    public ImageJobOutboxRelay(
            ImageJobOutboxRepository imageJobOutboxRepository,
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider,
            @Value("${app.queue.enabled:true}") boolean queueEnabled,
            @Value("${app.queue.image-jobs-key:imageflow:image-jobs}") String queueKey,
            @Value("${app.queue.outbox.batch-size:20}") int batchSize,
            @Value("${app.queue.retry-attempts:3}") int retryAttempts,
            @Value("${app.queue.retry-delay-millis:200}") long retryDelayMillis
    ) {
        this.imageJobOutboxRepository = imageJobOutboxRepository;
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
        this.queueEnabled = queueEnabled;
        this.queueKey = queueKey;
        this.batchSize = batchSize;
        this.retryAttempts = retryAttempts;
        this.retryDelayMillis = retryDelayMillis;
    }

    @Scheduled(fixedDelayString = "${app.queue.outbox.poll-interval-millis:1000}")
    public void relayPendingMessages() {
        if (!queueEnabled || stringRedisTemplate == null) {
            return;
        }

        List<ImageJobOutboxMessageEntity> pendingMessages = imageJobOutboxRepository.findByStatusOrderByCreatedAtAsc(
                ImageJobOutboxStatus.PENDING,
                PageRequest.of(0, batchSize)
        );

        for (ImageJobOutboxMessageEntity pendingMessage : pendingMessages) {
            relaySingleMessage(pendingMessage.getId());
        }
    }

    public void relaySingleMessage(java.util.UUID outboxMessageId) {
        ImageJobOutboxMessageEntity outboxMessage = imageJobOutboxRepository.findById(outboxMessageId)
                .orElseThrow(() -> new IllegalStateException("outbox message not found: " + outboxMessageId));

        if (outboxMessage.getStatus() != ImageJobOutboxStatus.PENDING) {
            return;
        }

        outboxMessage.markAttempt();
        try {
            publishWithRetry(outboxMessage.getPayload());
            outboxMessage.markSent();
        } catch (RuntimeException exception) {
            outboxMessage.markRetryableFailure(exception.getMessage());
        }
        imageJobOutboxRepository.save(outboxMessage);
    }

    public long pendingCount() {
        return imageJobOutboxRepository.countByStatus(ImageJobOutboxStatus.PENDING);
    }

    private void publishWithRetry(String payload) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                stringRedisTemplate.opsForList().rightPush(queueKey, payload);
                return;
            } catch (RuntimeException exception) {
                lastFailure = exception;
                if (attempt == retryAttempts) {
                    break;
                }
                try {
                    Thread.sleep(retryDelayMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("outbox relay retry interrupted", interruptedException);
                }
            }
        }

        throw new IllegalStateException("failed to relay outbox message after retries", lastFailure);
    }
}
