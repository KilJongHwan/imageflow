package com.imageflow.backend.domain.usage;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {
}
