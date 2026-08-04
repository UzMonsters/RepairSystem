package com.example.darks.repair_auto.shared.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {

    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ZoneId businessZone(
            @Value("${app.business-time-zone:${app.dashboard.business-time-zone:Asia/Tashkent}}") String zone) {
        return ZoneId.of(zone);
    }
}
