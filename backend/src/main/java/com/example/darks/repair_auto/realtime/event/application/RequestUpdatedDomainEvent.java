package com.example.darks.repair_auto.realtime.event.application;

public record RequestUpdatedDomainEvent(
        Long requestId,
        String requestNumber,
        Long customerId,
        Long technicianId
) {
    public RequestUpdatedDomainEvent(Long requestId, String requestNumber, Long customerId) {
        this(requestId, requestNumber, customerId, null);
    }
}
