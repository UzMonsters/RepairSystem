package com.example.darks.repair_auto.repair.assignment.domain;

import com.example.darks.repair_auto.identity.domain.User;
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

@Entity
@Table(name = "repair_assignments")
public class RepairAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repair_request_id", nullable = false)
    private RepairRequest repairRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AssignmentStatus status;

    @Column(name = "scheduled_visit_at")
    private OffsetDateTime scheduledVisitAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private User assignedByUser;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "closure_reason", length = 500)
    private String closureReason;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JsonIgnore
    @Version
    @Column(nullable = false)
    private long version;

    protected RepairAssignment() {
    }

    public RepairAssignment(
            RepairRequest repairRequest,
            Technician technician,
            OffsetDateTime scheduledVisitAt,
            User assignedByUser,
            OffsetDateTime now) {
        this.repairRequest = repairRequest;
        this.technician = technician;
        this.status = AssignmentStatus.PENDING;
        this.scheduledVisitAt = scheduledVisitAt;
        this.assignedByUser = assignedByUser;
        this.assignedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public RepairRequest getRepairRequest() {
        return repairRequest;
    }

    public Technician getTechnician() {
        return technician;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public OffsetDateTime getScheduledVisitAt() {
        return scheduledVisitAt;
    }

    public User getAssignedByUser() {
        return assignedByUser;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getClosureReason() {
        return closureReason;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isActive() {
        return status == AssignmentStatus.PENDING || status == AssignmentStatus.ACCEPTED;
    }

    public boolean isPending() {
        return status == AssignmentStatus.PENDING;
    }

    public void accept(OffsetDateTime now) {
        this.status = AssignmentStatus.ACCEPTED;
        this.respondedAt = now;
        this.updatedAt = now;
    }

    public void reject(String reason, OffsetDateTime now) {
        this.status = AssignmentStatus.REJECTED;
        this.rejectionReason = reason;
        this.respondedAt = now;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public void unassign(String reason, OffsetDateTime now) {
        this.status = AssignmentStatus.UNASSIGNED;
        this.closureReason = reason;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public void reassign(String reason, OffsetDateTime now) {
        this.status = AssignmentStatus.REASSIGNED;
        this.closureReason = reason;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public void complete(OffsetDateTime now) {
        this.status = AssignmentStatus.COMPLETED;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public void cancel(String reason, OffsetDateTime now) {
        this.status = AssignmentStatus.CANCELLED;
        this.closureReason = reason;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public void updateSchedule(OffsetDateTime scheduledVisitAt, OffsetDateTime now) {
        this.scheduledVisitAt = scheduledVisitAt;
        this.updatedAt = now;
    }
}
