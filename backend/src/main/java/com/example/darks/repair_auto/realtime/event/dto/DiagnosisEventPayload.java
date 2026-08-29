package com.example.darks.repair_auto.realtime.event.dto;

public record DiagnosisEventPayload(
        Long requestId,
        String requestNumber,
        Long executionId,
        Long technicianId,
        Long customerId
) {}
