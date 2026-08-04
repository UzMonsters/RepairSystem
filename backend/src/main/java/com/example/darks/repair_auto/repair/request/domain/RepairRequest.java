package com.example.darks.repair_auto.repair.request.domain;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.domain.User;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "repair_requests")
public class RepairRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_number", nullable = false, updatable = false, unique = true, length = 32)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private RepairCategory category;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 500)
    private String address;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RepairRequestPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RepairRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RepairRequestSource source;

    @Column(name = "customer_preferred_visit_at")
    private OffsetDateTime customerPreferredVisitAt;

    @Column(name = "internal_note", length = 2000)
    private String internalNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", updatable = false)
    private User createdByUser;

    @Column(name = "source_reference", unique = true, length = 120)
    private String sourceReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JsonIgnore
    @Version
    @Column(nullable = false)
    private long version;

    protected RepairRequest() {
    }

    public RepairRequest(
            String requestNumber,
            Customer customer,
            RepairCategory category,
            String description,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            RepairRequestPriority priority,
            OffsetDateTime customerPreferredVisitAt,
            String internalNote,
            User createdByUser,
            OffsetDateTime now) {
        this.requestNumber = requestNumber;
        this.customer = customer;
        this.category = category;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priority = priority == null ? RepairRequestPriority.NORMAL : priority;
        this.status = RepairRequestStatus.NEW;
        this.source = RepairRequestSource.ADMIN;
        this.customerPreferredVisitAt = customerPreferredVisitAt;
        this.internalNote = internalNote;
        this.createdByUser = createdByUser;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static RepairRequest telegram(
            String requestNumber,
            Customer customer,
            RepairCategory category,
            String description,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            RepairRequestPriority priority,
            OffsetDateTime customerPreferredVisitAt,
            String sourceReference,
            OffsetDateTime now) {
        RepairRequest repairRequest = new RepairRequest(
                requestNumber,
                customer,
                category,
                description,
                address,
                latitude,
                longitude,
                priority,
                customerPreferredVisitAt,
                null,
                null,
                now);
        repairRequest.source = RepairRequestSource.TELEGRAM;
        repairRequest.sourceReference = sourceReference;
        return repairRequest;
    }

    public Long getId() {
        return id;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public RepairCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public RepairRequestPriority getPriority() {
        return priority;
    }

    public RepairRequestStatus getStatus() {
        return status;
    }

    public RepairRequestSource getSource() {
        return source;
    }

    public OffsetDateTime getCustomerPreferredVisitAt() {
        return customerPreferredVisitAt;
    }

    public String getInternalNote() {
        return internalNote;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateIntake(
            Customer customer,
            RepairCategory category,
            String description,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            RepairRequestPriority priority,
            OffsetDateTime customerPreferredVisitAt,
            String internalNote,
            OffsetDateTime now) {
        this.customer = customer;
        this.category = category;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priority = priority;
        this.customerPreferredVisitAt = customerPreferredVisitAt;
        this.internalNote = internalNote;
        this.updatedAt = now;
    }

    public void markAssigned(OffsetDateTime now) {
        this.status = RepairRequestStatus.ASSIGNED;
        this.updatedAt = now;
    }

    public void markScheduled(OffsetDateTime now) {
        this.status = RepairRequestStatus.SCHEDULED;
        this.updatedAt = now;
    }

    public void returnToNew(OffsetDateTime now) {
        this.status = RepairRequestStatus.NEW;
        this.updatedAt = now;
    }

    public void markInProgress(OffsetDateTime now) {
        this.status = RepairRequestStatus.IN_PROGRESS;
        this.updatedAt = now;
    }

    public void markWaitingForParts(OffsetDateTime now) {
        this.status = RepairRequestStatus.WAITING_FOR_PARTS;
        this.updatedAt = now;
    }

    public void markCompleted(OffsetDateTime now) {
        this.status = RepairRequestStatus.COMPLETED;
        this.updatedAt = now;
    }

    public void markCancelled(OffsetDateTime now) {
        this.status = RepairRequestStatus.CANCELLED;
        this.updatedAt = now;
    }

    public void forceStatusForFixture(RepairRequestStatus status, OffsetDateTime now) {
        this.status = status;
        this.updatedAt = now;
    }
}
