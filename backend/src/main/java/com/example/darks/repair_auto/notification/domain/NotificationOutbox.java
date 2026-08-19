package com.example.darks.repair_auto.notification.domain;

import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

@Entity
@Table(name = "notification_outbox")
public class NotificationOutbox {

    public static final int PAYLOAD_VERSION = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, unique = true, length = 240)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 80)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    private NotificationRecipientType recipientType;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repair_request_id")
    private RepairRequest repairRequest;

    @Column(name = "template_key", nullable = false, length = 120)
    private String templateKey;

    @Column(name = "payload_json", nullable = false, length = 4000)
    private String payloadJson;

    @Column(name = "payload_version", nullable = false)
    private int payloadVersion;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "rendered_title", nullable = false, length = 240)
    private String renderedTitle;

    @Column(name = "rendered_message", nullable = false, length = 4096)
    private String renderedMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @Column(name = "processing_started_at")
    private OffsetDateTime processingStartedAt;

    @Column(name = "processing_lease_until")
    private OffsetDateTime processingLeaseUntil;

    @Column(name = "worker_id", length = 120)
    private String workerId;

    @Column(name = "provider_message_id", length = 120)
    private String providerMessageId;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "skipped_at")
    private OffsetDateTime skippedAt;

    @Column(name = "dead_at")
    private OffsetDateTime deadAt;

    @Column(name = "last_failure_category", length = 80)
    private String lastFailureCategory;

    @Column(name = "last_failure_at")
    private OffsetDateTime lastFailureAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JsonIgnore
    @Version
    @Column(nullable = false)
    private long version;

    protected NotificationOutbox() {
    }

    public NotificationOutbox(
            String eventKey,
            NotificationType notificationType,
            NotificationRecipientType recipientType,
            Long recipientId,
            RepairRequest repairRequest,
            String templateKey,
            String payloadJson,
            String language,
            String renderedTitle,
            String renderedMessage,
            OffsetDateTime now) {
        this(
                eventKey,
                notificationType,
                NotificationChannel.TELEGRAM,
                recipientType,
                recipientId,
                repairRequest,
                templateKey,
                payloadJson,
                language,
                renderedTitle,
                renderedMessage,
                now);
    }

    public NotificationOutbox(
            String eventKey,
            NotificationType notificationType,
            NotificationChannel channel,
            NotificationRecipientType recipientType,
            Long recipientId,
            RepairRequest repairRequest,
            String templateKey,
            String payloadJson,
            String language,
            String renderedTitle,
            String renderedMessage,
            OffsetDateTime now) {
        this.eventKey = eventKey;
        this.notificationType = notificationType;
        this.channel = channel != null ? channel : NotificationChannel.TELEGRAM;
        this.recipientType = recipientType;
        this.recipientId = recipientId;
        this.repairRequest = repairRequest;
        this.templateKey = templateKey;
        this.payloadJson = payloadJson;
        this.payloadVersion = PAYLOAD_VERSION;
        this.language = language;
        this.renderedTitle = renderedTitle;
        this.renderedMessage = renderedMessage;
        this.status = NotificationStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getEventKey() {
        return eventKey;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationRecipientType getRecipientType() {
        return recipientType;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public RepairRequest getRepairRequest() {
        return repairRequest;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public int getPayloadVersion() {
        return payloadVersion;
    }

    public String getLanguage() {
        return language;
    }

    public String getRenderedTitle() {
        return renderedTitle;
    }

    public String getRenderedMessage() {
        return renderedMessage;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public OffsetDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public OffsetDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    public OffsetDateTime getProcessingLeaseUntil() {
        return processingLeaseUntil;
    }

    public String getWorkerId() {
        return workerId;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public OffsetDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public OffsetDateTime getSkippedAt() {
        return skippedAt;
    }

    public OffsetDateTime getDeadAt() {
        return deadAt;
    }

    public String getLastFailureCategory() {
        return lastFailureCategory;
    }

    public OffsetDateTime getLastFailureAt() {
        return lastFailureAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void claim(String workerId, OffsetDateTime now, OffsetDateTime leaseUntil) {
        this.status = NotificationStatus.PROCESSING;
        this.processingStartedAt = now;
        this.processingLeaseUntil = leaseUntil;
        this.workerId = workerId;
        this.updatedAt = now;
    }

    public void recoverLease(OffsetDateTime now) {
        this.lastFailureCategory = NotificationFailureCategory.PROCESSING_LEASE_EXPIRED;
        this.lastFailureAt = now;
        this.status = NotificationStatus.RETRY_SCHEDULED;
        this.nextAttemptAt = now;
        clearProcessing();
        this.updatedAt = now;
    }

    public void markDelivered(String providerMessageId, OffsetDateTime now) {
        this.status = NotificationStatus.DELIVERED;
        this.providerMessageId = providerMessageId;
        this.deliveredAt = now;
        clearFailure();
        clearProcessing();
        this.updatedAt = now;
    }

    public void markSkipped(String failureCategory, OffsetDateTime now) {
        this.status = NotificationStatus.SKIPPED;
        this.skippedAt = now;
        this.lastFailureCategory = failureCategory;
        this.lastFailureAt = now;
        clearProcessing();
        this.updatedAt = now;
    }

    public void markDead(String failureCategory, OffsetDateTime now) {
        this.status = NotificationStatus.DEAD;
        this.deadAt = now;
        this.lastFailureCategory = failureCategory;
        this.lastFailureAt = now;
        clearProcessing();
        this.updatedAt = now;
    }

    public void scheduleRetry(String failureCategory, OffsetDateTime nextAttemptAt, OffsetDateTime now) {
        this.status = NotificationStatus.RETRY_SCHEDULED;
        this.nextAttemptAt = nextAttemptAt;
        this.lastFailureCategory = failureCategory;
        this.lastFailureAt = now;
        clearProcessing();
        this.updatedAt = now;
    }

    public void incrementAttemptCount() {
        this.attemptCount++;
    }

    public void manualRetry(String reason, OffsetDateTime now) {
        if (status == NotificationStatus.DELIVERED) {
            throw new IllegalStateException("Delivered notification cannot be retried.");
        }
        this.status = NotificationStatus.PENDING;
        this.nextAttemptAt = now;
        this.deliveredAt = null;
        this.skippedAt = null;
        this.deadAt = null;
        this.lastFailureCategory = NotificationFailureCategory.MANUAL_RETRY;
        this.lastFailureAt = now;
        clearProcessing();
        this.updatedAt = now;
    }

    private void clearFailure() {
        this.lastFailureCategory = null;
        this.lastFailureAt = null;
    }

    private void clearProcessing() {
        this.processingStartedAt = null;
        this.processingLeaseUntil = null;
        this.workerId = null;
    }
}
