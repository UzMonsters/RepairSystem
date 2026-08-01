package com.example.darks.repair_auto.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class FlywayPostgreSqlIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void givenEmptyPostgreSqlDatabaseWhenContextStartsThenFlywayMigratesFromZero() {
        Integer markerCount = jdbcTemplate.queryForObject(
                "select count(*) from phase0_schema_marker where id = 1",
                Integer.class);
        Integer successfulMigrations = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true",
                Integer.class);

        assertThat(markerCount).isEqualTo(1);
        assertThat(successfulMigrations).isGreaterThanOrEqualTo(1);
    }
}
