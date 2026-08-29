package com.example.darks.repair_auto.realtime.event.application;

public record RequestDiagnosisUpdatedDomainEvent(
        Long requestId,
        String requestNumber,
        Long executionId,
        Long technicianId,
        Long customerId
) {}
