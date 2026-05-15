package com.imageflow.backend.queue;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageJobOutboxRepository extends JpaRepository<ImageJobOutboxMessageEntity, UUID> {

    List<ImageJobOutboxMessageEntity> findByStatusOrderByCreatedAtAsc(ImageJobOutboxStatus status, Pageable pageable);

    long countByStatus(ImageJobOutboxStatus status);
}
