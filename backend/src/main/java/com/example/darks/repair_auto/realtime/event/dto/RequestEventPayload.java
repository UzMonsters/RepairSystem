package com.example.darks.repair_auto.realtime.event.dto;

public record RequestEventPayload(
        Long requestId,
        String requestNumber,
        Long customerId,
        Long technicianId,
        String status,
        String oldStatus,
        String priority
) {
    public RequestEventPayload(
            Long requestId,
            String requestNumber,
            Long customerId,
            Long technicianId,
            String status,
            String priority) {
        this(requestId, requestNumber, customerId, technicianId, status, null, priority);
    }

    public RequestEventPayload(Long requestId, String requestNumber) {
        this(requestId, requestNumber, null, null, null, null, null);
    }
}
