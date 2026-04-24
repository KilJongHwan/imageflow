package com.imageflow.backend.api;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final String processingMode;
    private final boolean queueEnabled;
    private final int maxBatchSize;
    private final String storageProvider;
    private final int uploadRequestsPerMinute;
    private final StringRedisTemplate stringRedisTemplate;
    private final String queueKey;

    public HealthController(
            @Value("${app.processing.mode:worker}") String processingMode,
            @Value("${app.queue.enabled:true}") boolean queueEnabled,
            @Value("${app.upload.max-batch-size:10}") int maxBatchSize,
            @Value("${app.storage.provider:local}") String storageProvider,
            @Value("${app.rate-limit.upload-requests-per-minute:30}") int uploadRequestsPerMinute,
            @Value("${app.queue.image-jobs-key:imageflow:image-jobs}") String queueKey,
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider
    ) {
        this.processingMode = processingMode;
        this.queueEnabled = queueEnabled;
        this.maxBatchSize = maxBatchSize;
        this.storageProvider = storageProvider;
        this.uploadRequestsPerMinute = uploadRequestsPerMinute;
        this.queueKey = queueKey;
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "imageflow-backend",
                "timestamp", Instant.now().toString(),
                "processingMode", processingMode,
                "queueEnabled", queueEnabled,
                "maxBatchSize", maxBatchSize,
                "storageProvider", storageProvider,
                "uploadRequestsPerMinute", uploadRequestsPerMinute,
                "queueDepth", resolveQueueDepth()
        );
    }

    private long resolveQueueDepth() {
        if (!queueEnabled || stringRedisTemplate == null) {
            return 0;
        }
        try {
            Long size = stringRedisTemplate.opsForList().size(queueKey);
            return size == null ? 0 : size;
        } catch (RuntimeException exception) {
            return -1;
        }
    }
}
