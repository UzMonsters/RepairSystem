package com.example.darks.repair_auto.realtime.event.dto;

import java.time.Instant;

public record ScheduleEventPayload(
        Long requestId,
        String requestNumber,
        Long assignmentId,
        Long technicianId,
        Long customerId,
        Instant scheduledStart,
        Instant scheduledEnd,
        String scheduleAction
) {}
