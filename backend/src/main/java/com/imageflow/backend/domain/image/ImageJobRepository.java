package com.imageflow.backend.domain.image;

import java.util.List;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.imageflow.backend.domain.user.User;

public interface ImageJobRepository extends JpaRepository<ImageJob, UUID> {

    java.util.Optional<ImageJob> findByIdAndUser(UUID id, User user);

    List<ImageJob> findTop20ByUserOrderByCreatedAtDesc(User user);

    List<ImageJob> findAllByIdInAndUser(Collection<UUID> ids, User user);
}
