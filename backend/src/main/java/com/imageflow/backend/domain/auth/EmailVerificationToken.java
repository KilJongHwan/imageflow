package com.imageflow.backend.domain.auth;

import java.time.LocalDateTime;
import java.util.UUID;

import com.imageflow.backend.common.entity.BaseTimeEntity;
import com.imageflow.backend.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 16)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    protected EmailVerificationToken() {
    }

    public EmailVerificationToken(User user, String email, String code, LocalDateTime expiresAt) {
        this.user = user;
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public void consume(LocalDateTime now) {
        this.consumedAt = now;
    }
}
