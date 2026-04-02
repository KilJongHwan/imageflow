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

    public ImageJobQueuePublisher(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.queue.enabled:true}") boolean enabled,
            @Value("${app.queue.image-jobs-key:imageflow:image-jobs}") String queueKey
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.queueKey = queueKey;
    }

    public void publish(ImageJobQueueMessage message) {
        if (!enabled) {
            return;
        }
        try {
            stringRedisTemplate.opsForList().rightPush(queueKey, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize image job message", exception);
        }
    }
}
