package com.example.darks.repair_auto.realtime.event.application;

public record RequestUnassignedDomainEvent(
        Long requestId,
        String requestNumber,
        Long customerId,
        Long technicianId,
        Long assignmentId
) {}
