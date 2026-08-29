package com.example.darks.repair_auto.realtime.event.dto;

public record AttachmentEventPayload(
        Long requestId,
        String requestNumber,
        Long attachmentId,
        String changeType,
        Long customerId,
        Long technicianId
) {}
