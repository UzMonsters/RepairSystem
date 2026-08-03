package com.example.darks.repair_auto.repair.assignment.api.dto;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.technician.domain.Technician;

public final class AssignmentMapper {

    private AssignmentMapper() {
    }

    public static CurrentAssignmentSummary current(RepairAssignment assignment) {
        if (assignment == null) {
            return null;
        }
        return new CurrentAssignmentSummary(
                assignment.getId(),
                assignment.getRepairRequest().getId(),
                technician(assignment.getTechnician()),
                assignment.getStatus(),
                assignment.getScheduledVisitAt(),
                assignment.getAssignedAt(),
                assignment.getRespondedAt());
    }

    public static AssignmentDetailResponse details(RepairAssignment assignment) {
        return new AssignmentDetailResponse(
                assignment.getId(),
                assignment.getRepairRequest().getId(),
                technician(assignment.getTechnician()),
                assignment.getStatus(),
                assignment.getScheduledVisitAt(),
                user(assignment.getAssignedByUser()),
                assignment.getAssignedAt(),
                assignment.getRespondedAt(),
                assignment.getRejectionReason(),
                assignment.getClosureReason(),
                assignment.getClosedAt(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }

    private static AssignmentTechnicianSummary technician(Technician technician) {
        return new AssignmentTechnicianSummary(
                technician.getId(),
                technician.getFullName(),
                technician.getPhone(),
                technician.isActive());
    }

    private static AssignmentUserSummary user(User user) {
        return new AssignmentUserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}
