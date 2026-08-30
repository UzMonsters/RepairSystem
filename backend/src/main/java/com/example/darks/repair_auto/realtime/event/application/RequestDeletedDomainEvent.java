package com.example.darks.repair_auto.realtime.event.application;

public record RequestDeletedDomainEvent(
        Long requestId,
        String requestNumber,
        Long customerId,
        Long technicianId
) {}
