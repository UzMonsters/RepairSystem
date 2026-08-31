package com.example.darks.repair_auto;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@ActiveProfiles("test")
public abstract class PostgreSqlIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;
    static final boolean USE_CONTAINER;

    static {
        PostgreSQLContainer<?> container = null;
        boolean started = false;
        try {
            container = new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("repair_auto_test")
                    .withUsername("repair_auto")
                    .withPassword("repair_auto");
            container.start();
            started = true;
        } catch (Throwable ex) {
            container = null;
            started = false;
        }
        POSTGRES = container;
        USE_CONTAINER = started;
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        if (USE_CONTAINER && POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5432/repair_auto_test");
            registry.add("spring.datasource.username", () -> "repair_auto");
            registry.add("spring.datasource.password", () -> "repair_auto");
        }
    }

    @BeforeEach
    void cleanWorkflowChildren() {
        jdbcTemplate.update("delete from telegram_technician_sessions");
        jdbcTemplate.update("delete from telegram_customer_sessions");
        jdbcTemplate.update("delete from telegram_user_contexts");
        jdbcTemplate.update("delete from telegram_technician_link_tokens");
        jdbcTemplate.update("delete from telegram_updates");
        jdbcTemplate.update("delete from notification_delivery_attempts");
        jdbcTemplate.update("delete from notification_outbox");
        jdbcTemplate.update("delete from refresh_sessions");
        jdbcTemplate.update("delete from repair_reviews");
        jdbcTemplate.update("delete from chat_messages");
        jdbcTemplate.update("delete from conversation_participants");
        jdbcTemplate.update("delete from conversations");
        jdbcTemplate.update("update users set avatar_attachment_id = null");
        jdbcTemplate.update("update customers set avatar_attachment_id = null");
        jdbcTemplate.update("update technicians set avatar_attachment_id = null");
        jdbcTemplate.update("delete from repair_attachments");
        jdbcTemplate.update("delete from repair_request_status_history");
        jdbcTemplate.update("delete from repair_executions");
        jdbcTemplate.update("delete from repair_assignments");
        jdbcTemplate.update("delete from auth_throttle_entries");
    }
}
