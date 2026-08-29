package com.example.darks.repair_auto.realtime.event.application;

public record RequestAssignmentCreatedDomainEvent(
        Long requestId,
        String requestNumber,
        Long technicianId,
        Long assignmentId,
        Long customerId
) {
    public RequestAssignmentCreatedDomainEvent(
            Long requestId,
            String requestNumber,
            Long technicianId,
            Long assignmentId) {
        this(requestId, requestNumber, technicianId, assignmentId, null);
    }
}
