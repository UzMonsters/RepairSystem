package com.example.darks.repair_auto.dashboard.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dashboard")
public record DashboardProperties(
        String businessTimeZone
) {
}
