package com.example.darks.repair_auto.dashboard.application;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DashboardClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
