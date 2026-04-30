package com.imageflow.backend.domain.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findTopByEmailOrderByCreatedAtDesc(String email);

    List<EmailVerificationToken> findByEmailAndConsumedAtIsNull(String email);
}
