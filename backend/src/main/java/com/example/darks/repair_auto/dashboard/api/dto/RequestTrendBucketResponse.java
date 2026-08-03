package com.example.darks.repair_auto.dashboard.api.dto;

import java.time.LocalDate;

public record RequestTrendBucketResponse(
        LocalDate date,
        long created,
        long completed,
        long cancelled) {
}
