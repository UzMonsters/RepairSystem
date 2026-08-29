package com.example.darks.repair_auto.realtime.event.dto;

public record AssignmentEventPayload(
        Long requestId,
        String requestNumber,
        Long assignmentId,
        Long technicianId,
        Long previousTechnicianId,
        Long customerId,
        String action,
        String status
) {
    public AssignmentEventPayload(
            Long requestId,
            String requestNumber,
            Long assignmentId,
            Long technicianId,
            Long customerId,
            String action,
            String status) {
        this(requestId, requestNumber, assignmentId, technicianId, null, customerId, action, status);
    }
}
