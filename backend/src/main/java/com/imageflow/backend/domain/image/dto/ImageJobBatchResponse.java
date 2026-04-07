package com.imageflow.backend.domain.image.dto;

import java.util.List;

public record ImageJobBatchResponse(
        List<ImageJobResponse> jobs,
        int totalCount,
        int succeededCount
) {
    public static ImageJobBatchResponse from(List<ImageJobResponse> jobs) {
        int succeededCount = (int) jobs.stream()
                .filter(job -> "SUCCEEDED".equals(job.status()))
                .count();

        return new ImageJobBatchResponse(jobs, jobs.size(), succeededCount);
    }
}
