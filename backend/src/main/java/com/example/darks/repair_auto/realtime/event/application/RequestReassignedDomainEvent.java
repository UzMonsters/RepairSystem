package com.example.darks.repair_auto.realtime.event.application;

public record RequestReassignedDomainEvent(
        Long requestId,
        String requestNumber,
        Long oldTechnicianId,
        Long newTechnicianId,
        Long assignmentId,
        Long customerId
) {}
