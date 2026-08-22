package com.example.darks.repair_auto.shared.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRepairMigrationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayRepairMigrationConfiguration.class);

    @Bean
    @ConditionalOnProperty(name = "app.flyway.repair-on-startup", havingValue = "true")
    FlywayMigrationStrategy repairBeforeMigrateStrategy() {
        return (Flyway flyway) -> {
            LOGGER.warn("Running Flyway repair before migrate because app.flyway.repair-on-startup=true.");
            flyway.repair();
            flyway.migrate();
        };
    }
}
