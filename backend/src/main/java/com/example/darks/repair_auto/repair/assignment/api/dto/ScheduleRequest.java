package com.example.darks.repair_auto.repair.assignment.api.dto;

import java.time.OffsetDateTime;

public record ScheduleRequest(
        OffsetDateTime scheduledVisitAt,
        Boolean clearSchedule) {
}
