package com.example.darks.repair_auto.dashboard.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DashboardTimeRange(
        LocalDate fromDate,
        LocalDate toDate,
        OffsetDateTime fromInclusive,
        OffsetDateTime toExclusive) {
}
