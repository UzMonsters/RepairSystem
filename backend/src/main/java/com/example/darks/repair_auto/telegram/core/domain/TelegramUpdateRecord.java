package com.example.darks.repair_auto.telegram.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "telegram_updates")
public class TelegramUpdateRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_update_id", nullable = false, unique = true)
    private Long telegramUpdateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TelegramUpdateStatus status;

    @Column(name = "update_type", nullable = false, length = 40)
    private String updateType;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "failure_category", length = 80)
    private String failureCategory;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TelegramUpdateRecord() {
    }

    public TelegramUpdateRecord(Long telegramUpdateId, String updateType, OffsetDateTime now) {
        this.telegramUpdateId = telegramUpdateId;
        this.status = TelegramUpdateStatus.RECEIVED;
        this.updateType = updateType;
        this.receivedAt = now;
        this.attemptCount = 1;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getTelegramUpdateId() {
        return telegramUpdateId;
    }

    public TelegramUpdateStatus getStatus() {
        return status;
    }

    public boolean isProcessed() {
        return status == TelegramUpdateStatus.PROCESSED;
    }

    public void retry(OffsetDateTime now) {
        this.status = TelegramUpdateStatus.RECEIVED;
        this.failureCategory = null;
        this.attemptCount++;
        this.updatedAt = now;
    }

    public void processed(OffsetDateTime now) {
        this.status = TelegramUpdateStatus.PROCESSED;
        this.processedAt = now;
        this.failureCategory = null;
        this.updatedAt = now;
    }

    public void failed(String failureCategory, OffsetDateTime now) {
        this.status = TelegramUpdateStatus.FAILED;
        this.failureCategory = safe(failureCategory);
        this.updatedAt = now;
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 80);
    }
}
