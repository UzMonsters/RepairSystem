package com.example.darks.repair_auto.repair.execution.api.dto;

import java.time.OffsetDateTime;

public record RepairExecutionSummary(
        Long id,
        OffsetDateTime startedAt,
        boolean diagnosisPresent,
        OffsetDateTime waitingSince,
        OffsetDateTime completedAt,
        OffsetDateTime cancelledAt) {
}
