package com.example.darks.repair_auto.realtime.event.application;

import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;

public record RequestStatusChangedDomainEvent(
        Long requestId,
        String requestNumber,
        Long customerId,
        Long technicianId,
        RepairRequestStatus fromStatus,
        RepairRequestStatus toStatus
) {}
