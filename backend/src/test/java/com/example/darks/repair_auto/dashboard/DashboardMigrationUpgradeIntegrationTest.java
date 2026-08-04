package com.example.darks.repair_auto.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class DashboardMigrationUpgradeIntegrationTest {

    @Test
    void phase10DatabaseUpgradesToV16AndAddsDashboardIndexesWithoutChangingData() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("repair_auto_dashboard_upgrade")
                .withUsername("repair_auto")
                .withPassword("repair_auto")) {
            postgres.start();
            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .target("15")
                    .load()
                    .migrate();
            JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword()));
            seedPhase10Data(jdbcTemplate);

            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            assertThat(count(jdbcTemplate, "repair_requests")).isEqualTo(2);
            assertThat(count(jdbcTemplate, "repair_executions")).isEqualTo(2);
            assertThat(count(jdbcTemplate, "repair_reviews")).isEqualTo(1);
            assertThat(indexExists(jdbcTemplate, "idx_repair_executions_completed_at")).isTrue();
            assertThat(indexExists(jdbcTemplate, "idx_repair_executions_cancelled_at")).isTrue();
            assertThat(indexExists(jdbcTemplate, "idx_repair_requests_created_category")).isTrue();
        }
    }

    private void seedPhase10Data(JdbcTemplate jdbcTemplate) {
        Long userId = jdbcTemplate.queryForObject("""
                insert into users (full_name, email, password_hash, role, active, password_changed_at)
                values ('Admin User', 'dashboard-upgrade-admin@example.com', 'hash', 'ADMIN', true, now())
                returning id
                """, Long.class);
        Long customerId = jdbcTemplate.queryForObject("""
                insert into customers (
                    full_name, phone, telegram_user_id, telegram_chat_id,
                    preferred_language, registration_source
                ) values ('Ali Valiyev', '+998901212121', 72001, 82001, 'UZ', 'TELEGRAM')
                returning id
                """, Long.class);
        Long technicianId = jdbcTemplate.queryForObject("""
                insert into technicians (
                    full_name, phone, specialization, maximum_concurrent_requests, active,
                    preferred_language, created_at, updated_at
                ) values ('Usta Dashboard', '+998902323232', 'Cooling', 5, true, 'UZ', now(), now())
                returning id
                """, Long.class);
        Long categoryId = jdbcTemplate.queryForObject("""
                insert into repair_categories (
                    name_uz, name_ru, name_en,
                    name_uz_normalized, name_ru_normalized, name_en_normalized,
                    active, display_order
                ) values ('Konditsioner', 'Кондиционер', 'Air Conditioner',
                    'dashboard-konditsioner', 'dashboard-conditioner-ru', 'dashboard-conditioner', true, 10)
                returning id
                """, Long.class);
        Long completedRequest = request(jdbcTemplate, userId, customerId, categoryId, "REP-DASH-UP-001", "COMPLETED");
        Long cancelledRequest = request(jdbcTemplate, userId, customerId, categoryId, "REP-DASH-UP-002", "CANCELLED");
        jdbcTemplate.update("""
                insert into repair_assignments (
                    repair_request_id, technician_id, status, assigned_by_user_id,
                    assigned_at, responded_at, closed_at, created_at, updated_at
                ) values (?, ?, 'COMPLETED', ?, now(), now(), now(), now(), now())
                """, completedRequest, technicianId, userId);
        jdbcTemplate.update("""
                insert into repair_executions (
                    repair_request_id, started_at, started_by_user_id, diagnosis,
                    diagnosis_updated_at, diagnosis_updated_by_user_id, work_performed,
                    completed_at, completed_by_user_id, created_at, updated_at
                ) values (?, now(), ?, 'Upgrade diagnosis.', now(), ?, 'Upgrade repair.', now(), ?, now(), now())
                """, completedRequest, userId, userId, userId);
        jdbcTemplate.update("""
                insert into repair_executions (
                    repair_request_id, cancellation_reason, cancelled_at, cancelled_by_user_id, created_at, updated_at
                ) values (?, 'Cancelled during upgrade seed.', now(), ?, now(), now())
                """, cancelledRequest, userId);
        jdbcTemplate.update("""
                insert into repair_reviews (
                    repair_request_id, customer_id, technician_id, rating, comment,
                    source, submitted_language, submitted_at, created_at
                ) values (?, ?, ?, 5, 'Great.', 'TELEGRAM', 'EN', now(), now())
                """, completedRequest, customerId, technicianId);
    }

    private Long request(
            JdbcTemplate jdbcTemplate,
            Long userId,
            Long customerId,
            Long categoryId,
            String requestNumber,
            String status) {
        return jdbcTemplate.queryForObject("""
                insert into repair_requests (
                    request_number, customer_id, category_id, description, address, priority,
                    status, source, created_by_user_id, created_at, updated_at
                ) values (?, ?, ?, 'Dashboard upgrade seeded request.', 'Tashkent',
                    'NORMAL', ?, 'ADMIN', ?, now(), now())
                returning id
                """, Long.class, requestNumber, customerId, categoryId, status, userId);
    }

    private long count(JdbcTemplate jdbcTemplate, String tableName) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
        return count == null ? 0 : count;
    }

    private boolean indexExists(JdbcTemplate jdbcTemplate, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from pg_indexes
                where schemaname = 'public' and indexname = ?
                """, Integer.class, indexName);
        return count != null && count > 0;
    }
}
