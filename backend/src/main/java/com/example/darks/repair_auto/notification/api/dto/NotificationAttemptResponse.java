package com.example.darks.repair_auto.notification.api.dto;

import com.example.darks.repair_auto.notification.domain.NotificationAttemptOutcome;
import java.time.OffsetDateTime;

public record NotificationAttemptResponse(
        int attemptNumber,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        NotificationAttemptOutcome outcome,
        String failureCategory) {
}
