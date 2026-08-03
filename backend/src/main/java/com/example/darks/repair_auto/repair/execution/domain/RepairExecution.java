package com.example.darks.repair_auto.repair.execution.domain;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "repair_executions")
public class RepairExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repair_request_id", nullable = false)
    private RepairRequest repairRequest;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by_user_id")
    private User startedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by_technician_id")
    private Technician startedByTechnician;

    @Column(length = 4000)
    private String diagnosis;

    @Column(name = "diagnosis_updated_at")
    private OffsetDateTime diagnosisUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_updated_by_user_id")
    private User diagnosisUpdatedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnosis_updated_by_technician_id")
    private Technician diagnosisUpdatedByTechnician;

    @Column(name = "waiting_reason", length = 1000)
    private String waitingReason;

    @Column(name = "waiting_since")
    private OffsetDateTime waitingSince;

    @Column(name = "work_performed", length = 4000)
    private String workPerformed;

    @Column(name = "completion_note", length = 2000)
    private String completionNote;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_user_id")
    private User completedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_technician_id")
    private Technician completedByTechnician;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id")
    private User cancelledByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_technician_id")
    private Technician cancelledByTechnician;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JsonIgnore
    @Version
    @Column(nullable = false)
    private long version;

    protected RepairExecution() {
    }

    public RepairExecution(RepairRequest repairRequest, OffsetDateTime now) {
        this.repairRequest = repairRequest;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public RepairRequest getRepairRequest() {
        return repairRequest;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public User getStartedByUser() {
        return startedByUser;
    }

    public Technician getStartedByTechnician() {
        return startedByTechnician;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public OffsetDateTime getDiagnosisUpdatedAt() {
        return diagnosisUpdatedAt;
    }

    public User getDiagnosisUpdatedByUser() {
        return diagnosisUpdatedByUser;
    }

    public Technician getDiagnosisUpdatedByTechnician() {
        return diagnosisUpdatedByTechnician;
    }

    public String getWaitingReason() {
        return waitingReason;
    }

    public OffsetDateTime getWaitingSince() {
        return waitingSince;
    }

    public String getWorkPerformed() {
        return workPerformed;
    }

    public String getCompletionNote() {
        return completionNote;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public User getCompletedByUser() {
        return completedByUser;
    }

    public Technician getCompletedByTechnician() {
        return completedByTechnician;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public User getCancelledByUser() {
        return cancelledByUser;
    }

    public Technician getCancelledByTechnician() {
        return cancelledByTechnician;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean hasStarted() {
        return startedAt != null;
    }

    public boolean hasDiagnosis() {
        return diagnosis != null && !diagnosis.isBlank();
    }

    public void start(User user, OffsetDateTime now) {
        this.startedAt = now;
        this.startedByUser = user;
        this.startedByTechnician = null;
        this.updatedAt = now;
    }

    public void startByTechnician(Technician technician, OffsetDateTime now) {
        this.startedAt = now;
        this.startedByUser = null;
        this.startedByTechnician = technician;
        this.updatedAt = now;
    }

    public void updateDiagnosis(String diagnosis, User user, OffsetDateTime now) {
        this.diagnosis = diagnosis;
        this.diagnosisUpdatedAt = now;
        this.diagnosisUpdatedByUser = user;
        this.diagnosisUpdatedByTechnician = null;
        this.updatedAt = now;
    }

    public void updateDiagnosisByTechnician(String diagnosis, Technician technician, OffsetDateTime now) {
        this.diagnosis = diagnosis;
        this.diagnosisUpdatedAt = now;
        this.diagnosisUpdatedByUser = null;
        this.diagnosisUpdatedByTechnician = technician;
        this.updatedAt = now;
    }

    public void waitForParts(String reason, OffsetDateTime now) {
        this.waitingReason = reason;
        this.waitingSince = now;
        this.updatedAt = now;
    }

    public String clearWaiting(OffsetDateTime now) {
        String previousReason = waitingReason;
        this.waitingReason = null;
        this.waitingSince = null;
        this.updatedAt = now;
        return previousReason;
    }

    public void complete(String workPerformed, String completionNote, User user, OffsetDateTime now) {
        this.workPerformed = workPerformed;
        this.completionNote = completionNote;
        this.completedAt = now;
        this.completedByUser = user;
        this.completedByTechnician = null;
        this.waitingReason = null;
        this.waitingSince = null;
        this.updatedAt = now;
    }

    public void completeByTechnician(
            String workPerformed,
            String completionNote,
            Technician technician,
            OffsetDateTime now) {
        this.workPerformed = workPerformed;
        this.completionNote = completionNote;
        this.completedAt = now;
        this.completedByUser = null;
        this.completedByTechnician = technician;
        this.waitingReason = null;
        this.waitingSince = null;
        this.updatedAt = now;
    }

    public void cancel(String reason, User user, OffsetDateTime now) {
        this.cancellationReason = reason;
        this.cancelledAt = now;
        this.cancelledByUser = user;
        this.cancelledByTechnician = null;
        this.waitingReason = null;
        this.waitingSince = null;
        this.updatedAt = now;
    }

    public void cancelByTechnician(String reason, Technician technician, OffsetDateTime now) {
        this.cancellationReason = reason;
        this.cancelledAt = now;
        this.cancelledByUser = null;
        this.cancelledByTechnician = technician;
        this.waitingReason = null;
        this.waitingSince = null;
        this.updatedAt = now;
    }
}
