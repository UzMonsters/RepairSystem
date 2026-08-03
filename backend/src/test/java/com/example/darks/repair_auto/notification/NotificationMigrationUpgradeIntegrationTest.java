package com.example.darks.repair_auto.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class NotificationMigrationUpgradeIntegrationTest {

    @Test
    void phase8DatabaseUpgradesToV14AndCreatesNotificationConstraintsAndIndexes() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("repair_auto_upgrade")
                .withUsername("repair_auto")
                .withPassword("repair_auto")) {
            postgres.start();
            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .target("13")
                    .load()
                    .migrate();
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword()));
            seedPhase8Data(jdbcTemplate);

            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            assertThat(count(jdbcTemplate, "users")).isEqualTo(1);
            assertThat(count(jdbcTemplate, "customers")).isEqualTo(1);
            assertThat(count(jdbcTemplate, "technicians")).isEqualTo(1);
            assertThat(count(jdbcTemplate, "repair_requests")).isEqualTo(1);
            assertThat(count(jdbcTemplate, "repair_assignments")).isEqualTo(1);
            assertThat(count(jdbcTemplate, "telegram_customer_sessions")).isEqualTo(1);
            assertThat(count(jdbcTemplate, "telegram_technician_sessions")).isEqualTo(1);
            assertThat(tableExists(jdbcTemplate, "notification_outbox")).isTrue();
            assertThat(tableExists(jdbcTemplate, "notification_delivery_attempts")).isTrue();
            assertThat(constraintExists(jdbcTemplate, "notification_outbox", "notification_outbox_event_key_unique"))
                    .isTrue();
            assertThat(constraintExists(jdbcTemplate, "notification_outbox", "notification_outbox_status_check"))
                    .isTrue();
            assertThat(constraintExists(jdbcTemplate, "notification_outbox", "notification_outbox_channel_check"))
                    .isTrue();
            assertThat(constraintExists(jdbcTemplate, "notification_outbox", "notification_outbox_recipient_type_check"))
                    .isTrue();
            assertThat(constraintExists(jdbcTemplate, "notification_outbox", "notification_outbox_payload_version_check"))
                    .isTrue();
            assertThat(constraintExists(jdbcTemplate, "notification_outbox", "notification_outbox_payload_json_check"))
                    .isTrue();
            assertThat(constraintExists(jdbcTemplate, "notification_outbox", "notification_outbox_repair_request_fk"))
                    .isTrue();
            assertThat(constraintExists(
                    jdbcTemplate,
                    "notification_delivery_attempts",
                    "notification_delivery_attempts_notification_fk")).isTrue();
            assertThat(constraintExists(
                    jdbcTemplate,
                    "notification_delivery_attempts",
                    "notification_delivery_attempts_attempt_unique")).isTrue();
            assertThat(columnExists(jdbcTemplate, "notification_outbox", "version")).isTrue();
            assertThat(indexExists(jdbcTemplate, "idx_notification_outbox_status_next_attempt_created")).isTrue();
            assertThat(indexExists(jdbcTemplate, "idx_notification_outbox_status_processing_lease")).isTrue();
            assertThat(indexExists(jdbcTemplate, "idx_notification_outbox_repair_request")).isTrue();
            assertThat(indexExists(jdbcTemplate, "idx_notification_outbox_recipient")).isTrue();
            assertThat(indexExists(jdbcTemplate, "idx_notification_outbox_type")).isTrue();
            assertThat(indexExists(jdbcTemplate, "notification_outbox_event_key_unique")).isTrue();
        }
    }

    private void seedPhase8Data(JdbcTemplate jdbcTemplate) {
        Long userId = jdbcTemplate.queryForObject("""
                insert into users (full_name, email, password_hash, role, active, password_changed_at)
                values ('Admin User', 'admin@example.com', 'hash', 'ADMIN', true, now())
                returning id
                """, Long.class);
        Long customerId = jdbcTemplate.queryForObject("""
                insert into customers (
                    full_name, phone, telegram_user_id, telegram_chat_id, preferred_language, registration_source
                ) values ('Ali Valiyev', '+998901112233', 70001, 80001, 'UZ', 'ADMIN')
                returning id
                """, Long.class);
        Long technicianId = jdbcTemplate.queryForObject("""
                insert into technicians (
                    full_name, phone, specialization, maximum_concurrent_requests, active,
                    telegram_user_id, telegram_chat_id, telegram_linked_at, preferred_language
                ) values ('Usta Karim', '+998902223344', 'Cooling', 5, true, 70002, 80002, now(), 'EN')
                returning id
                """, Long.class);
        Long categoryId = jdbcTemplate.queryForObject("""
                insert into repair_categories (
                    name_uz, name_ru, name_en,
                    name_uz_normalized, name_ru_normalized, name_en_normalized,
                    active, display_order
                ) values ('Konditsioner', 'Konditsioner RU', 'Air Conditioner',
                    'konditsioner', 'konditsioner-ru', 'air-conditioner', true, 10)
                returning id
                """, Long.class);
        Long requestId = jdbcTemplate.queryForObject("""
                insert into repair_requests (
                    request_number, customer_id, category_id, description, address, priority,
                    status, source, created_by_user_id, created_at, updated_at
                ) values ('REP-000001', ?, ?, 'Appliance does not cool properly.', 'Tashkent',
                    'NORMAL', 'ASSIGNED', 'ADMIN', ?, now(), now())
                returning id
                """, Long.class, customerId, categoryId, userId);
        jdbcTemplate.update("""
                insert into repair_assignments (
                    repair_request_id, technician_id, status, assigned_by_user_id,
                    assigned_at, created_at, updated_at
                ) values (?, ?, 'PENDING', ?, now(), now(), now())
                """, requestId, technicianId, userId);
        jdbcTemplate.update("""
                insert into telegram_customer_sessions (
                    telegram_user_id, telegram_chat_id, customer_id, language, state,
                    last_interaction_at, created_at, updated_at
                ) values (70001, 80001, ?, 'UZ', 'MAIN_MENU', now(), now(), now())
                """, customerId);
        jdbcTemplate.update("""
                insert into telegram_user_contexts (
                    telegram_user_id, telegram_chat_id, active_mode, created_at, updated_at
                ) values (70002, 80002, 'TECHNICIAN', now(), now())
                """);
        jdbcTemplate.update("""
                insert into telegram_technician_sessions (
                    telegram_user_id, telegram_chat_id, technician_id, language, state,
                    last_interaction_at, created_at, updated_at
                ) values (70002, 80002, ?, 'EN', 'MAIN_MENU', now(), now(), now())
                """, technicianId);
        jdbcTemplate.update("""
                insert into repair_attachments (
                    repair_request_id, attachment_type, status, storage_key, original_file_name,
                    content_type, size_bytes, sha256_checksum, uploaded_by_user_id,
                    uploaded_at, available_at, created_at, updated_at
                ) values (?, 'GENERAL_DOCUMENT', 'AVAILABLE', 'phase8/document.pdf', 'document.pdf',
                    'application/pdf', 12, ?, ?, now(), now(), now(), now())
                """, requestId, "a".repeat(64), userId);
    }

    private long count(JdbcTemplate jdbcTemplate, String tableName) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
        return count == null ? 0 : count;
    }

    private boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean constraintExists(JdbcTemplate jdbcTemplate, String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.table_constraints
                where table_schema = 'public' and table_name = ? and constraint_name = ?
                """, Integer.class, tableName, constraintName);
        return count != null && count > 0;
    }

    private boolean columnExists(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = 'public' and table_name = ? and column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(JdbcTemplate jdbcTemplate, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from pg_indexes
                where schemaname = 'public' and indexname = ?
                """, Integer.class, indexName);
        return count != null && count > 0;
    }
}
