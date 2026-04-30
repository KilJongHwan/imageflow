package com.imageflow.backend.domain.user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.imageflow.backend.common.entity.BaseTimeEntity;
import com.imageflow.backend.domain.image.ImageJob;
import com.imageflow.backend.domain.usage.UsageRecord;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    private String apiKey;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserPlan plan;

    @Column(name = "credit_balance", nullable = false)
    private int creditBalance;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", length = 20)
    private AuthProvider authProvider;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImageJob> imageJobs = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UsageRecord> usageRecords = new ArrayList<>();

    protected User() {
    }

    public User(String email, UserPlan plan, int creditBalance) {
        this.email = email;
        this.plan = plan;
        this.creditBalance = creditBalance;
    }

    public User(String email, String passwordHash, UserPlan plan, int creditBalance) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.plan = plan;
        this.creditBalance = creditBalance;
    }

    public User(String email, String passwordHash, UserPlan plan, int creditBalance, boolean emailVerified, UserRole role, AuthProvider authProvider) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.plan = plan;
        this.creditBalance = creditBalance;
        this.emailVerified = emailVerified;
        this.role = role;
        this.authProvider = authProvider;
    }

    @PrePersist
    protected void assignApiKey() {
        if (this.apiKey == null || this.apiKey.isBlank()) {
            this.apiKey = UUID.randomUUID().toString().replace("-", "");
        }
        if (this.plan == null) {
            this.plan = UserPlan.FREE;
        }
        if (this.role == null) {
            this.role = UserRole.USER;
        }
        if (this.authProvider == null) {
            this.authProvider = AuthProvider.LOCAL;
        }
        if (this.emailVerified == null) {
            this.emailVerified = false;
        }
    }

    public void chargeCredits(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (creditBalance < amount) {
            throw new IllegalStateException("not enough credits");
        }
        this.creditBalance -= amount;
    }

    public void addCredits(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.creditBalance += amount;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserPlan getPlan() {
        return plan;
    }

    public int getCreditBalance() {
        return creditBalance;
    }

    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }

    public UserRole getRole() {
        return role == null ? UserRole.USER : role;
    }

    public AuthProvider getAuthProvider() {
        return authProvider == null ? AuthProvider.LOCAL : authProvider;
    }

    public List<ImageJob> getImageJobs() {
        return imageJobs;
    }

    public List<UsageRecord> getUsageRecords() {
        return usageRecords;
    }

    public void addImageJob(ImageJob imageJob) {
        this.imageJobs.add(imageJob);
    }

    public void addUsageRecord(UsageRecord usageRecord) {
        this.usageRecords.add(usageRecord);
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }
}
