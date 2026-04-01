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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserPlan plan;

    @Column(name = "credit_balance", nullable = false)
    private int creditBalance;

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

    @PrePersist
    protected void assignApiKey() {
        if (this.apiKey == null || this.apiKey.isBlank()) {
            this.apiKey = UUID.randomUUID().toString().replace("-", "");
        }
        if (this.plan == null) {
            this.plan = UserPlan.FREE;
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

    public UserPlan getPlan() {
        return plan;
    }

    public int getCreditBalance() {
        return creditBalance;
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
}
