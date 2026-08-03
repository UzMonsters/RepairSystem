package com.example.darks.repair_auto.notification.domain;

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
import java.time.OffsetDateTime;

@Entity
@Table(name = "notification_delivery_attempts")
public class NotificationDeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private NotificationOutbox notification;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "worker_id", nullable = false, length = 120)
    private String workerId;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private OffsetDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationAttemptOutcome outcome;

    @Column(name = "failure_category", length = 80)
    private String failureCategory;

    @Column(name = "provider_message_id", length = 120)
    private String providerMessageId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected NotificationDeliveryAttempt() {
    }

    public NotificationDeliveryAttempt(
            NotificationOutbox notification,
            int attemptNumber,
            String workerId,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            NotificationAttemptOutcome outcome,
            String failureCategory,
            String providerMessageId) {
        this.notification = notification;
        this.attemptNumber = attemptNumber;
        this.workerId = workerId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.outcome = outcome;
        this.failureCategory = failureCategory;
        this.providerMessageId = providerMessageId;
        this.createdAt = finishedAt;
    }

    public Long getId() {
        return id;
    }

    public NotificationOutbox getNotification() {
        return notification;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public String getWorkerId() {
        return workerId;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public NotificationAttemptOutcome getOutcome() {
        return outcome;
    }

    public String getFailureCategory() {
        return failureCategory;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }
}
