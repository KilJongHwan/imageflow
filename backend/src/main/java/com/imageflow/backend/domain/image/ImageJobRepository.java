package com.imageflow.backend.domain.image;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageJobRepository extends JpaRepository<ImageJob, UUID> {
}
