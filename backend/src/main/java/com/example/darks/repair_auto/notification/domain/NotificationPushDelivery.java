package com.example.darks.repair_auto.notification.domain;

import com.example.darks.repair_auto.notification.push.domain.PushEndpoint;
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
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "notification_push_deliveries")
public class NotificationPushDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_outbox_id", nullable = false)
    private NotificationOutbox notificationOutbox;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "push_endpoint_id", nullable = false)
    private PushEndpoint pushEndpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "firebase_message_id", length = 255)
    private String firebaseMessageId;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "dead_at")
    private OffsetDateTime deadAt;

    @Column(name = "last_error_code", length = 80)
    private String lastErrorCode;

    @Column(name = "last_error_category", length = 80)
    private String lastErrorCategory;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected NotificationPushDelivery() {
    }

    public NotificationPushDelivery(
            NotificationOutbox notificationOutbox,
            PushEndpoint pushEndpoint,
            OffsetDateTime now) {
        this.notificationOutbox = Objects.requireNonNull(notificationOutbox, "notificationOutbox must not be null");
        this.pushEndpoint = Objects.requireNonNull(pushEndpoint, "pushEndpoint must not be null");
        this.status = NotificationStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void markDelivered(OffsetDateTime now, String firebaseMessageId) {
        this.status = NotificationStatus.DELIVERED;
        this.attemptCount++;
        this.firebaseMessageId = firebaseMessageId;
        this.deliveredAt = now;
        this.updatedAt = now;
    }

    public void markRetry(OffsetDateTime now, OffsetDateTime nextAttemptAt, String errorCode, String errorCategory) {
        this.status = NotificationStatus.RETRY_SCHEDULED;
        this.attemptCount++;
        this.nextAttemptAt = nextAttemptAt;
        this.lastErrorCode = errorCode;
        this.lastErrorCategory = errorCategory;
        this.updatedAt = now;
    }

    public void markDead(OffsetDateTime now, String errorCode, String errorCategory) {
        this.status = NotificationStatus.DEAD;
        this.attemptCount++;
        this.deadAt = now;
        this.lastErrorCode = errorCode;
        this.lastErrorCategory = errorCategory;
        this.updatedAt = now;
    }

    public void markSkipped(OffsetDateTime now, String reason) {
        this.status = NotificationStatus.SKIPPED;
        this.lastErrorCategory = reason;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public NotificationOutbox getNotificationOutbox() {
        return notificationOutbox;
    }

    public PushEndpoint getPushEndpoint() {
        return pushEndpoint;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getFirebaseMessageId() {
        return firebaseMessageId;
    }

    public OffsetDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public OffsetDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public OffsetDateTime getDeadAt() {
        return deadAt;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public String getLastErrorCategory() {
        return lastErrorCategory;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
