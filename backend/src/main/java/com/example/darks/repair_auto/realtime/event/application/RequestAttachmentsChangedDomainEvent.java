package com.example.darks.repair_auto.realtime.event.application;

public record RequestAttachmentsChangedDomainEvent(
        Long requestId,
        String requestNumber,
        Long attachmentId,
        String changeType,
        Long customerId,
        Long technicianId
) {}
