package com.example.darks.repair_auto.repair.execution.domain;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.technician.domain.Technician;
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
@Table(name = "repair_request_status_history")
public class RepairRequestStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repair_request_id", nullable = false)
    private RepairRequest repairRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private RepairRequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private RepairRequestStatus toStatus;

    @Column(length = 1000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_technician_id")
    private Technician changedByTechnician;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    protected RepairRequestStatusHistory() {
    }

    public RepairRequestStatusHistory(
            RepairRequest repairRequest,
            RepairRequestStatus fromStatus,
            RepairRequestStatus toStatus,
            String reason,
            User changedByUser,
            OffsetDateTime changedAt) {
        this.repairRequest = repairRequest;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedByUser = changedByUser;
        this.changedAt = changedAt;
    }

    public RepairRequestStatusHistory(
            RepairRequest repairRequest,
            RepairRequestStatus fromStatus,
            RepairRequestStatus toStatus,
            String reason,
            Technician changedByTechnician,
            OffsetDateTime changedAt) {
        this.repairRequest = repairRequest;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedByTechnician = changedByTechnician;
        this.changedAt = changedAt;
    }

    public Long getId() {
        return id;
    }

    public RepairRequest getRepairRequest() {
        return repairRequest;
    }

    public RepairRequestStatus getFromStatus() {
        return fromStatus;
    }

    public RepairRequestStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public User getChangedByUser() {
        return changedByUser;
    }

    public Technician getChangedByTechnician() {
        return changedByTechnician;
    }

    public OffsetDateTime getChangedAt() {
        return changedAt;
    }
}
