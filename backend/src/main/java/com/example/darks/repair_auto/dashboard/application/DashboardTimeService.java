package com.example.darks.repair_auto.dashboard.application;

import com.example.darks.repair_auto.dashboard.domain.DashboardPeriod;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class DashboardTimeService {

    private final ZoneId businessZone;

    public DashboardTimeService(DashboardProperties properties) {
        String configuredZone = properties.businessTimeZone() == null
                ? "Asia/Tashkent"
                : properties.businessTimeZone();
        try {
            this.businessZone = ZoneId.of(configuredZone);
        } catch (DateTimeException exception) {
            throw new IllegalStateException("DASHBOARD_TIME_ZONE_INVALID: app.dashboard.business-time-zone is invalid.", exception);
        }
    }

    public ZoneId businessZone() {
        return businessZone;
    }

    public LocalDate businessDate(OffsetDateTime generatedAt) {
        return generatedAt.toInstant().atZone(businessZone).toLocalDate();
    }

    public DashboardTimeRange todayRange(OffsetDateTime generatedAt) {
        LocalDate date = businessDate(generatedAt);
        return range(date, date);
    }

    public DashboardTimeRange periodRange(DashboardPeriod period, OffsetDateTime generatedAt) {
        LocalDate toDate = businessDate(generatedAt);
        LocalDate fromDate = toDate.minusDays(period.days() - 1L);
        return range(fromDate, toDate);
    }

    public OffsetDateTime toUtcStart(LocalDate date) {
        return OffsetDateTime.ofInstant(date.atStartOfDay(businessZone).toInstant(), ZoneOffset.UTC);
    }

    private DashboardTimeRange range(LocalDate fromDate, LocalDate toDate) {
        return new DashboardTimeRange(
                fromDate,
                toDate,
                toUtcStart(fromDate),
                toUtcStart(toDate.plusDays(1)));
    }
}
