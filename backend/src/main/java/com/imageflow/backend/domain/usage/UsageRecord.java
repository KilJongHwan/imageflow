package com.imageflow.backend.domain.usage;

import java.util.UUID;

import com.imageflow.backend.common.entity.BaseTimeEntity;
import com.imageflow.backend.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usage_records")
public class UsageRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UsageType usageType;

    @Column(nullable = false)
    private int amount;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(length = 255)
    private String description;

    protected UsageRecord() {
    }

    public UsageRecord(User user, UsageType usageType, int amount, String referenceId, String description) {
        this.user = user;
        this.usageType = usageType;
        this.amount = amount;
        this.referenceId = referenceId;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public int getAmount() {
        return amount;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getDescription() {
        return description;
    }
}
