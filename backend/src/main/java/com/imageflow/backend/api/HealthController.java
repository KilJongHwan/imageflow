package com.imageflow.backend.api;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final String processingMode;
    private final boolean queueEnabled;
    private final int maxBatchSize;

    public HealthController(
            @Value("${app.processing.mode:worker}") String processingMode,
            @Value("${app.queue.enabled:true}") boolean queueEnabled,
            @Value("${app.upload.max-batch-size:10}") int maxBatchSize
    ) {
        this.processingMode = processingMode;
        this.queueEnabled = queueEnabled;
        this.maxBatchSize = maxBatchSize;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "imageflow-backend",
                "timestamp", Instant.now().toString(),
                "processingMode", processingMode,
                "queueEnabled", queueEnabled,
                "maxBatchSize", maxBatchSize
        );
    }
}
