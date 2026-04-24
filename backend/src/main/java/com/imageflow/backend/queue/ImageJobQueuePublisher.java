package com.imageflow.backend.queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ImageJobQueuePublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String queueKey;
    private final int retryAttempts;
    private final long retryDelayMillis;

    public ImageJobQueuePublisher(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.queue.enabled:true}") boolean enabled,
            @Value("${app.queue.image-jobs-key:imageflow:image-jobs}") String queueKey,
            @Value("${app.queue.retry-attempts:3}") int retryAttempts,
            @Value("${app.queue.retry-delay-millis:200}") long retryDelayMillis
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.queueKey = queueKey;
        this.retryAttempts = retryAttempts;
        this.retryDelayMillis = retryDelayMillis;
    }

    public void publish(ImageJobQueueMessage message) {
        if (!enabled) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(message);
            publishWithRetry(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize image job message", exception);
        }
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
                    throw new IllegalStateException("image job queue publish retry interrupted", interruptedException);
                }
            }
        }

        throw new IllegalStateException("failed to publish image job message after retries", lastFailure);
    }
}
