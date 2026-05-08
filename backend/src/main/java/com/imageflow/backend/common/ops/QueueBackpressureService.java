package com.imageflow.backend.common.ops;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.imageflow.backend.common.exception.BadRequestException;

@Service
public class QueueBackpressureService {

    private final boolean queueEnabled;
    private final int maxBacklogDepth;
    private final String queueKey;
    private final StringRedisTemplate stringRedisTemplate;

    public QueueBackpressureService(
            @Value("${app.queue.enabled:true}") boolean queueEnabled,
            @Value("${app.queue.max-backlog-depth:100}") int maxBacklogDepth,
            @Value("${app.queue.image-jobs-key:imageflow:image-jobs}") String queueKey,
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider
    ) {
        this.queueEnabled = queueEnabled;
        this.maxBacklogDepth = maxBacklogDepth;
        this.queueKey = queueKey;
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
    }

    public void checkWritable() {
        if (!queueEnabled || maxBacklogDepth <= 0 || stringRedisTemplate == null) {
            return;
        }

        long currentDepth = currentDepth();
        if (currentDepth >= maxBacklogDepth) {
            throw new BadRequestException(
                    "queue backlog limit reached, please retry later (depth=%d, limit=%d)"
                            .formatted(currentDepth, maxBacklogDepth)
            );
        }
    }

    public long currentDepth() {
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

    public int maxBacklogDepth() {
        return maxBacklogDepth;
    }
}
