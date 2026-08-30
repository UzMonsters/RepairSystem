package com.example.darks.repair_auto.realtime.event.dto;

public record RequestDeletedPayload(
        Long requestId,
        String requestNumber,
        Long customerId,
        Long technicianId
) {}
