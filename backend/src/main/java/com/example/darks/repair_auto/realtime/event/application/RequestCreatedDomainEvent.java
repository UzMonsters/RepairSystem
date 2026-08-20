package com.example.darks.repair_auto.realtime.event.application;

public record RequestCreatedDomainEvent(
        Long requestId,
        String requestNumber,
        Long customerId
) {}
