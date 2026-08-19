package com.example.darks.repair_auto.notification.inbox.domain;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.notification.domain.NotificationRecipientType;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.technician.domain.Technician;
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
import java.util.Objects;

@Entity
@Table(name = "user_notifications")
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 30)
    private NotificationRecipientType recipientType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repair_request_id")
    private RepairRequest repairRequest;

    @Column(name = "request_number", length = 64)
    private String requestNumber;

    @Column(name = "target", nullable = false, length = 64)
    private String target;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private String payloadJson;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JsonIgnore
    @Version
    @Column(nullable = false)
    private long version;

    protected UserNotification() {
    }

    public UserNotification(
            String eventKey,
            NotificationType notificationType,
            NotificationRecipientType recipientType,
            Customer customer,
            Technician technician,
            RepairRequest repairRequest,
            String requestNumber,
            String target,
            Long targetId,
            String payloadJson,
            OffsetDateTime createdAt) {
        this.eventKey = Objects.requireNonNull(eventKey, "eventKey must not be null");
        this.notificationType = Objects.requireNonNull(notificationType, "notificationType must not be null");
        this.recipientType = Objects.requireNonNull(recipientType, "recipientType must not be null");
        this.customer = customer;
        this.technician = technician;
        this.repairRequest = repairRequest;
        this.requestNumber = requestNumber;
        this.target = Objects.requireNonNull(target, "target must not be null");
        this.targetId = targetId;
        this.payloadJson = Objects.requireNonNull(payloadJson, "payloadJson must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = createdAt;
    }

    public static UserNotification forCustomer(
            String eventKey,
            NotificationType notificationType,
            Customer customer,
            RepairRequest repairRequest,
            String requestNumber,
            String target,
            Long targetId,
            String payloadJson,
            OffsetDateTime createdAt) {
        return new UserNotification(
                eventKey,
                notificationType,
                NotificationRecipientType.CUSTOMER,
                Objects.requireNonNull(customer, "customer must not be null"),
                null,
                repairRequest,
                requestNumber,
                target,
                targetId,
                payloadJson,
                createdAt);
    }

    public static UserNotification forTechnician(
            String eventKey,
            NotificationType notificationType,
            Technician technician,
            RepairRequest repairRequest,
            String requestNumber,
            String target,
            Long targetId,
            String payloadJson,
            OffsetDateTime createdAt) {
        return new UserNotification(
                eventKey,
                notificationType,
                NotificationRecipientType.TECHNICIAN,
                null,
                Objects.requireNonNull(technician, "technician must not be null"),
                repairRequest,
                requestNumber,
                target,
                targetId,
                payloadJson,
                createdAt);
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

    public NotificationRecipientType getRecipientType() {
        return recipientType;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Technician getTechnician() {
        return technician;
    }

    public RepairRequest getRepairRequest() {
        return repairRequest;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public String getTarget() {
        return target;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead(OffsetDateTime now) {
        if (this.readAt == null) {
            this.readAt = now;
            this.updatedAt = now;
        }
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
