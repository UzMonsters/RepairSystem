package com.example.darks.repair_auto.repair.execution.application;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.repair.execution.domain.RepairRequestStatusHistory;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairRequestStatusHistoryRepository;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.technician.domain.Technician;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class RepairStatusHistoryService {

    private final RepairRequestStatusHistoryRepository statusHistoryRepository;

    public RepairStatusHistoryService(RepairRequestStatusHistoryRepository statusHistoryRepository) {
        this.statusHistoryRepository = statusHistoryRepository;
    }

    public RepairRequestStatusHistory recordInitial(
            RepairRequest request,
            String reason,
            User user,
            OffsetDateTime changedAt) {
        return statusHistoryRepository.saveAndFlush(new RepairRequestStatusHistory(
                request,
                null,
                request.getStatus(),
                blankToNull(reason),
                user,
                changedAt));
    }

    public RepairRequestStatusHistory recordTransition(
            RepairRequest request,
            RepairRequestStatus fromStatus,
            String reason,
            User user,
            OffsetDateTime changedAt) {
        if (fromStatus == request.getStatus()) {
            return null;
        }
        return statusHistoryRepository.saveAndFlush(new RepairRequestStatusHistory(
                request,
                fromStatus,
                request.getStatus(),
                blankToNull(reason),
                user,
                changedAt));
    }

    public RepairRequestStatusHistory recordTransition(
            RepairRequest request,
            RepairRequestStatus fromStatus,
            String reason,
            Technician technician,
            OffsetDateTime changedAt) {
        if (fromStatus == request.getStatus()) {
            return null;
        }
        return statusHistoryRepository.saveAndFlush(new RepairRequestStatusHistory(
                request,
                fromStatus,
                request.getStatus(),
                blankToNull(reason),
                technician,
                changedAt));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
