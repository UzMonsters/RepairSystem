package com.example.darks.repair_auto.dashboard.domain;

import com.example.darks.repair_auto.shared.error.BusinessRuleException;

public enum DashboardPeriod {
    LAST_7_DAYS("7d", 7),
    LAST_30_DAYS("30d", 30);

    private final String apiValue;
    private final int days;

    DashboardPeriod(String apiValue, int days) {
        this.apiValue = apiValue;
        this.days = days;
    }

    public String apiValue() {
        return apiValue;
    }

    public int days() {
        return days;
    }

    public static DashboardPeriod parse(String value) {
        if (value == null || value.isBlank()) {
            return LAST_30_DAYS;
        }
        for (DashboardPeriod period : values()) {
            if (period.apiValue.equalsIgnoreCase(value.trim())) {
                return period;
            }
        }
        throw new BusinessRuleException(
                "DASHBOARD_PERIOD_INVALID",
                "Dashboard period must be 7d or 30d.",
                400);
    }
}
