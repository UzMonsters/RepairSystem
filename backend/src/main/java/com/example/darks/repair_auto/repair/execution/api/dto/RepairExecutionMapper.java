package com.example.darks.repair_auto.repair.execution.api.dto;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.repair.execution.domain.RepairExecution;
import com.example.darks.repair_auto.repair.execution.domain.RepairRequestStatusHistory;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestUserSummary;

public final class RepairExecutionMapper {

    private RepairExecutionMapper() {
    }

    public static RepairExecutionSummary summary(RepairExecution execution) {
        if (execution == null) {
            return null;
        }
        return new RepairExecutionSummary(
                execution.getId(),
                execution.getStartedAt(),
                execution.hasDiagnosis(),
                execution.getWaitingSince(),
                execution.getCompletedAt(),
                execution.getCancelledAt());
    }

    public static RepairExecutionDetailResponse details(RepairExecution execution) {
        return new RepairExecutionDetailResponse(
                execution.getId(),
                execution.getRepairRequest().getId(),
                execution.getStartedAt(),
                user(execution.getStartedByUser()),
                execution.getDiagnosis(),
                execution.getDiagnosisUpdatedAt(),
                user(execution.getDiagnosisUpdatedByUser()),
                execution.getWaitingReason(),
                execution.getWaitingSince(),
                execution.getWorkPerformed(),
                execution.getCompletionNote(),
                execution.getCompletedAt(),
                user(execution.getCompletedByUser()),
                execution.getCancellationReason(),
                execution.getCancelledAt(),
                user(execution.getCancelledByUser()),
                execution.getCreatedAt(),
                execution.getUpdatedAt());
    }

    public static RepairRequestStatusHistoryResponse history(RepairRequestStatusHistory history) {
        return history(history, history.getReason());
    }

    public static RepairRequestStatusHistoryResponse history(
            RepairRequestStatusHistory history,
            String reason) {
        return new RepairRequestStatusHistoryResponse(
                history.getId(),
                history.getRepairRequest().getId(),
                history.getFromStatus(),
                history.getToStatus(),
                reason,
                user(history.getChangedByUser()),
                history.getChangedAt());
    }

    private static RepairRequestUserSummary user(User user) {
        if (user == null) {
            return null;
        }
        return new RepairRequestUserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
