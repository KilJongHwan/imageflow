package com.imageflow.backend.common.ops;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.imageflow.backend.common.exception.BadRequestException;

@Service
public class RateLimitService {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public void checkLimit(String scope, String subject, int maxRequestsPerMinute) {
        if (maxRequestsPerMinute <= 0) {
            return;
        }

        long minuteBucket = Instant.now().getEpochSecond() / 60;
        String key = scope + ":" + subject + ":" + minuteBucket;

        counters.entrySet().removeIf(entry -> entry.getValue().bucket() < minuteBucket - 1);
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(minuteBucket, new AtomicInteger()));
        int current = counter.requestCount().incrementAndGet();
        if (current > maxRequestsPerMinute) {
            throw new BadRequestException("rate limit exceeded for " + scope + ", try again in a minute");
        }
    }

    private record WindowCounter(long bucket, AtomicInteger requestCount) {
    }
}
