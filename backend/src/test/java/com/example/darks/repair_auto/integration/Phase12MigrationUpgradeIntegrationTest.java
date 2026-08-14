package com.example.darks.repair_auto.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class Phase12MigrationUpgradeIntegrationTest {

    @Test
    void phase11DatabaseUpgradesToV17AndAddsHardeningObjectsWithoutChangingData() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("repair_auto_phase12_upgrade")
                .withUsername("repair_auto")
                .withPassword("repair_auto")) {
            postgres.start();
            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .target("16")
                    .load()
                    .migrate();
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword()));
            seedPhase11Data(jdbcTemplate);

            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            assertThat(count(jdbcTemplate, "repair_requests")).isEqualTo(1);
            assertThat(count(jdbcTemplate, "repair_attachments")).isEqualTo(1);
            assertThat(tableExists(jdbcTemplate, "auth_throttle_entries")).isTrue();
            assertThat(columnExists(jdbcTemplate, "repair_attachments", "object_purged_at")).isTrue();
            assertThat(indexExists(jdbcTemplate, "idx_repair_attachments_stale_uploading")).isTrue();
            assertThat(indexExists(jdbcTemplate, "idx_repair_attachments_deleted_cleanup")).isTrue();
            assertThat(indexExists(jdbcTemplate, "idx_telegram_updates_status_received_at")).isTrue();
            assertThat(indexExists(jdbcTemplate, "idx_notification_delivery_attempts_created_at")).isTrue();
        }
    }

    private void seedPhase11Data(JdbcTemplate jdbcTemplate) {
        Long userId = jdbcTemplate.queryForObject("""
                insert into users (full_name, email, password_hash, role, active, password_changed_at)
                values ('Upgrade Admin', 'phase12-upgrade-admin@example.com', 'hash', 'ADMIN', true, now())
                returning id
                """, Long.class);
        Long customerId = jdbcTemplate.queryForObject("""
                insert into customers (full_name, phone, preferred_language, registration_source)
                values ('Upgrade Customer', '+998902222222', 'UZ', 'ADMIN')
                returning id
                """, Long.class);
        Long categoryId = jdbcTemplate.queryForObject("""
                insert into repair_categories (
                    name_uz, name_ru, name_en,
                    name_uz_normalized, name_ru_normalized, name_en_normalized,
                    active
                ) values (
                    'Upgrade', 'Upgrade RU', 'Upgrade EN',
                    'phase12-upgrade-uz', 'phase12-upgrade-ru', 'phase12-upgrade-en',
                    true
                )
                returning id
                """, Long.class);
        Long requestId = jdbcTemplate.queryForObject("""
                insert into repair_requests (
                    request_number, customer_id, category_id, description, address, priority,
                    status, source, created_by_user_id, created_at, updated_at
                ) values (
                    'REP-P12-UP-001', ?, ?, 'Phase twelve upgrade seeded request.',
                    'Tashkent', 'NORMAL', 'NEW', 'ADMIN', ?, now(), now()
                )
                returning id
                """, Long.class, customerId, categoryId, userId);
        jdbcTemplate.update("""
                insert into repair_attachments (
                    repair_request_id, attachment_type, status, storage_key, original_file_name,
                    uploaded_by_user_id, uploaded_at, created_at, updated_at
                ) values (?, 'GENERAL_DOCUMENT', 'UPLOADING', 'phase12/upgrade', 'upgrade.pdf', ?, now(), now(), now())
                """, requestId, userId);
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
