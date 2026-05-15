package com.imageflow.backend.queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ImageJobQueuePublisher {

    private final ObjectMapper objectMapper;
    private final ImageJobOutboxRepository imageJobOutboxRepository;
    private final boolean enabled;

    public ImageJobQueuePublisher(
            ObjectMapper objectMapper,
            ImageJobOutboxRepository imageJobOutboxRepository,
            @Value("${app.queue.enabled:true}") boolean enabled
    ) {
        this.objectMapper = objectMapper;
        this.imageJobOutboxRepository = imageJobOutboxRepository;
        this.enabled = enabled;
    }

    public void publish(ImageJobQueueMessage message) {
        if (!enabled) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(message);
            imageJobOutboxRepository.save(new ImageJobOutboxMessageEntity(
                    message.jobId().toString(),
                    "IMAGE_JOB_QUEUED",
                    payload
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize image job message", exception);
        }
    }
}
