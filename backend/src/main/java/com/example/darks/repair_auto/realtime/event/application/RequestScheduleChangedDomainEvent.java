package com.example.darks.repair_auto.realtime.event.application;

import java.time.OffsetDateTime;

public record RequestScheduleChangedDomainEvent(
        Long requestId,
        String requestNumber,
        Long assignmentId,
        Long technicianId,
        Long customerId,
        OffsetDateTime scheduledStart,
        OffsetDateTime scheduledEnd,
        String scheduleAction
) {}
