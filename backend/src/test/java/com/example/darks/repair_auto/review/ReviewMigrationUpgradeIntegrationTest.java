package com.example.darks.repair_auto.review;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class ReviewMigrationUpgradeIntegrationTest {

    @Test
    void phase9DatabaseUpgradesToV15AndPreservesExistingTelegramData() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("repair_auto_review_upgrade")
                .withUsername("repair_auto")
                .withPassword("repair_auto")) {
            postgres.start();
            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .target("14")
                    .load()
                    .migrate();
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword()));
            seedPhase9Data(jdbcTemplate);

            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            assertThat(count(jdbcTemplate, "customers")).isEqualTo(1);
            assertThat(count(jdbcTemplate, "technicians")).isEqualTo(1);
            assertThat(count(jdbcTemplate, "repair_requests")).isEqualTo(1);
            assertThat(count(jdbcTemplate, "telegram_customer_sessions")).isEqualTo(1);
            assertThat(tableExists(jdbcTemplate, "repair_reviews")).isTrue();
            assertThat(columnExists(jdbcTemplate, "telegram_customer_sessions", "review_request_id")).isTrue();
            assertThat(columnExists(jdbcTemplate, "telegram_customer_sessions", "draft_review_rating")).isTrue();
            assertThat(columnExists(jdbcTemplate, "telegram_customer_sessions", "draft_review_comment")).isTrue();
            assertThat(constraintExists(jdbcTemplate, "repair_reviews", "repair_reviews_request_unique")).isTrue();
            assertThat(constraintExists(jdbcTemplate, "repair_reviews", "repair_reviews_rating_check")).isTrue();
            assertThat(indexExists(jdbcTemplate, "idx_repair_reviews_customer_id")).isTrue();
        }
    }

    private void seedPhase9Data(JdbcTemplate jdbcTemplate) {
        Long userId = jdbcTemplate.queryForObject("""
                insert into users (full_name, email, password_hash, role, active, password_changed_at)
                values ('Admin User', 'review-upgrade-admin@example.com', 'hash', 'ADMIN', true, now())
                returning id
                """, Long.class);
        Long customerId = jdbcTemplate.queryForObject("""
                insert into customers (
                    full_name, phone, telegram_user_id, telegram_chat_id,
                    preferred_language, registration_source
                ) values ('Ali Valiyev', '+998901112233', 71001, 81001, 'UZ', 'TELEGRAM')
                returning id
                """, Long.class);
        Long technicianId = jdbcTemplate.queryForObject("""
                insert into technicians (
                    full_name, phone, specialization, maximum_concurrent_requests, active,
                    preferred_language, created_at, updated_at
                ) values ('Usta Karim', '+998902223344', 'Cooling', 5, true, 'UZ', now(), now())
                returning id
                """, Long.class);
        Long categoryId = jdbcTemplate.queryForObject("""
                insert into repair_categories (
                    name_uz, name_ru, name_en,
                    name_uz_normalized, name_ru_normalized, name_en_normalized,
                    active
                ) values ('Konditsioner', 'Konditsioner RU', 'Air Conditioner',
                    'konditsioner', 'konditsioner-ru', 'air-conditioner', true)
                returning id
                """, Long.class);
        Long requestId = jdbcTemplate.queryForObject("""
                insert into repair_requests (
                    request_number, customer_id, category_id, description, address, priority,
                    status, source, created_by_user_id, source_reference, created_at, updated_at
                ) values ('REP-UPGRADE-001', ?, ?, 'Completed request before review phase.', 'Tashkent',
                    'NORMAL', 'COMPLETED', 'TELEGRAM', null, 'upgrade-review-source', now(), now())
                returning id
                """, Long.class, customerId, categoryId);
        jdbcTemplate.update("""
                insert into repair_assignments (
                    repair_request_id, technician_id, status, assigned_by_user_id,
                    assigned_at, responded_at, closed_at, created_at, updated_at
                ) values (?, ?, 'COMPLETED', ?, now(), now(), now(), now(), now())
                """, requestId, technicianId, userId);
        jdbcTemplate.update("""
                insert into telegram_customer_sessions (
                    telegram_user_id, telegram_chat_id, customer_id, language, state,
                    last_interaction_at, created_at, updated_at
                ) values (71001, 81001, ?, 'UZ', 'MAIN_MENU', now(), now(), now())
                """, customerId);
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

    private boolean constraintExists(JdbcTemplate jdbcTemplate, String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.table_constraints
                where table_schema = 'public' and table_name = ? and constraint_name = ?
                """, Integer.class, tableName, constraintName);
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
