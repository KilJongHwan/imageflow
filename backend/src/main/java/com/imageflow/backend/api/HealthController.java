package com.imageflow.backend.api;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.imageflow.backend.common.ops.QueueBackpressureService;

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
    private final QueueBackpressureService queueBackpressureService;

    public HealthController(
            @Value("${app.processing.mode:worker}") String processingMode,
            @Value("${app.queue.enabled:true}") boolean queueEnabled,
            @Value("${app.upload.max-batch-size:10}") int maxBatchSize,
            @Value("${app.storage.provider:local}") String storageProvider,
            @Value("${app.rate-limit.upload-requests-per-minute:30}") int uploadRequestsPerMinute,
            @Value("${app.queue.image-jobs-key:imageflow:image-jobs}") String queueKey,
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider,
            QueueBackpressureService queueBackpressureService
    ) {
        this.processingMode = processingMode;
        this.queueEnabled = queueEnabled;
        this.maxBatchSize = maxBatchSize;
        this.storageProvider = storageProvider;
        this.uploadRequestsPerMinute = uploadRequestsPerMinute;
        this.queueKey = queueKey;
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
        this.queueBackpressureService = queueBackpressureService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.ofEntries(
                Map.entry("status", "ok"),
                Map.entry("service", "imageflow-backend"),
                Map.entry("timestamp", Instant.now().toString()),
                Map.entry("processingMode", processingMode),
                Map.entry("queueEnabled", queueEnabled),
                Map.entry("maxBatchSize", maxBatchSize),
                Map.entry("storageProvider", storageProvider),
                Map.entry("uploadRequestsPerMinute", uploadRequestsPerMinute),
                Map.entry("queueDepth", resolveQueueDepth()),
                Map.entry("maxQueueBacklogDepth", queueBackpressureService.maxBacklogDepth()),
                Map.entry("queueWritable", isQueueWritable())
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

    private boolean isQueueWritable() {
        long depth = resolveQueueDepth();
        int limit = queueBackpressureService.maxBacklogDepth();
        if (limit <= 0 || depth < 0) {
            return true;
        }
        return depth < limit;
    }
}
