package com.example.darks.repair_auto.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.shared.cleanup.TechnicalDataCleanupService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "app.cleanup.batch-size=50",
        "app.cleanup.stale-upload-threshold=PT1H",
        "app.cleanup.deleted-object-retention=PT1H",
        "app.cleanup.refresh-session-retention=PT1H",
        "app.cleanup.telegram-update-retention=PT1H",
        "app.cleanup.notification-attempt-retention=PT1H",
        "app.auth-throttle.retention=PT1H"
})
class TechnicalDataCleanupServiceIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private TechnicalDataCleanupService cleanupService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetRows() {
        jdbcTemplate.update("delete from refresh_sessions");
        jdbcTemplate.update("delete from repair_attachments");
        jdbcTemplate.update("delete from repair_requests");
        jdbcTemplate.update("delete from repair_categories");
        jdbcTemplate.update("delete from customers");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void givenExpiredTechnicalDataWhenCleanupRunsThenOnlyEligibleRowsAreRemovedOrMarked() {
        Long userId = user();
        Long requestId = request(userId, customer(), category());
        Long terminalNotificationId = terminalNotification(requestId);
        Long pendingNotificationId = pendingNotification(requestId);
        OffsetDateTime old = OffsetDateTime.now().minusHours(3);

        attachment(requestId, userId, "UPLOADING", "cleanup/stale", old);
        attachment(requestId, userId, "DELETED", "cleanup/deleted", old);
        attachment(requestId, userId, "AVAILABLE", "cleanup/available", old);
        usedRefreshSession(userId, old);
        telegramUpdate(910001L, "PROCESSED", old);
        telegramUpdate(910002L, "RECEIVED", old);
        deliveryAttempt(terminalNotificationId, old);
        deliveryAttempt(pendingNotificationId, old);
        jdbcTemplate.update("""
                insert into auth_throttle_entries (
                    throttle_key, failed_attempts, window_started_at, updated_at
                ) values ('login:cleanup-old', 1, ?, ?)
                """, old, old);

        TechnicalDataCleanupService.CleanupResult result = cleanupService.runOnce();

        assertThat(result.staleUploadsFailed()).isEqualTo(1);
        assertThat(result.deletedObjectsPurged()).isEqualTo(1);
        assertThat(result.technicalRowsDeleted()).isEqualTo(4);
        assertThat(attachmentStatus("cleanup/stale")).isEqualTo("FAILED");
        assertThat(objectPurgedAt("cleanup/deleted")).isNotNull();
        assertThat(count("refresh_sessions")).isZero();
        assertThat(countWhere("telegram_updates", "status = 'PROCESSED'")).isZero();
        assertThat(countWhere("telegram_updates", "status = 'RECEIVED'")).isEqualTo(1);
        assertThat(countWhere("notification_delivery_attempts", "notification_id = " + terminalNotificationId))
                .isZero();
        assertThat(countWhere("notification_delivery_attempts", "notification_id = " + pendingNotificationId))
                .isEqualTo(1);
        assertThat(count("auth_throttle_entries")).isZero();
    }

    private Long user() {
        return jdbcTemplate.queryForObject("""
                insert into users (full_name, email, password_hash, role, active, password_changed_at)
                values ('Cleanup Admin', 'cleanup-admin@example.com', 'hash', 'ADMIN', true, now())
                returning id
                """, Long.class);
    }

    private Long customer() {
        return jdbcTemplate.queryForObject("""
                insert into customers (full_name, phone, preferred_language, registration_source)
                values ('Cleanup Customer', '+998901111111', 'UZ', 'ADMIN')
                returning id
                """, Long.class);
    }

    private Long category() {
        return jdbcTemplate.queryForObject("""
                insert into repair_categories (
                    name_uz, name_ru, name_en,
                    name_uz_normalized, name_ru_normalized, name_en_normalized,
                    active, display_order
                ) values (
                    'Tozalash', 'Ochistka', 'Cleanup',
                    'cleanup-uz', 'cleanup-ru', 'cleanup-en',
                    true, 1
                )
                returning id
                """, Long.class);
    }

    private Long request(Long userId, Long customerId, Long categoryId) {
        return jdbcTemplate.queryForObject("""
                insert into repair_requests (
                    request_number, customer_id, category_id, description, address, priority,
                    status, source, created_by_user_id, created_at, updated_at
                ) values (
                    'REP-CLEAN-001', ?, ?, 'Cleanup integration seeded request.',
                    'Tashkent', 'NORMAL', 'NEW', 'ADMIN', ?, now(), now()
                )
                returning id
                """, Long.class, customerId, categoryId, userId);
    }

    private void attachment(Long requestId, Long userId, String status, String storageKey, OffsetDateTime old) {
        jdbcTemplate.update("""
                insert into repair_attachments (
                    repair_request_id, attachment_type, status, storage_key, original_file_name,
                    content_type, size_bytes, sha256_checksum, uploaded_by_user_id, uploaded_at,
                    available_at, deleted_at, failure_reason, created_at, updated_at
                ) values (
                    ?, 'GENERAL_DOCUMENT', ?, ?, 'clean.pdf',
                    case when ? = 'AVAILABLE' then 'application/pdf' else null end,
                    case when ? = 'AVAILABLE' then 12 else null end,
                    case when ? = 'AVAILABLE'
                        then 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'
                        else null end,
                    ?, ?, case when ? = 'AVAILABLE' then ? else null end,
                    case when ? = 'DELETED' then ? else null end,
                    case when ? = 'FAILED' then 'SEED_FAILURE' else null end,
                    ?, ?
                )
                """, requestId, status, storageKey, status, status, status, userId, old, status, old, status, old,
                status, old, old);
    }

    private void usedRefreshSession(Long userId, OffsetDateTime old) {
        jdbcTemplate.update("""
                insert into refresh_sessions (
                    user_id, token_hash, token_family_id, issued_at, expires_at, used_at, created_at
                ) values (
                    ?, 'cleanup-token-hash',
                    '00000000-0000-0000-0000-000000000001'::uuid,
                    ?, ?, ?, ?
                )
                """, userId, old.minusHours(1), old, old, old);
    }

    private void telegramUpdate(Long updateId, String status, OffsetDateTime old) {
        jdbcTemplate.update("""
                insert into telegram_updates (
                    telegram_update_id, status, update_type, received_at, processed_at,
                    attempt_count, created_at, updated_at
                ) values (?, ?, 'MESSAGE', ?, case when ? = 'PROCESSED' then ? else null end, 1, ?, ?)
                """, updateId, status, old, status, old, old, old);
    }

    private Long terminalNotification(Long requestId) {
        return notification(requestId, "DELIVERED");
    }

    private Long pendingNotification(Long requestId) {
        return notification(requestId, "PENDING");
    }

    private Long notification(Long requestId, String status) {
        return jdbcTemplate.queryForObject("""
                insert into notification_outbox (
                    event_key, notification_type, channel, recipient_type, recipient_id,
                    repair_request_id, template_key, payload_json, payload_version, status,
                    next_attempt_at, delivered_at, created_at, updated_at
                ) values (
                    ?, 'CUSTOMER_REQUEST_CREATED', 'TELEGRAM', 'CUSTOMER', 1,
                    ?, 'customer.request.created', '{}', 1, ?,
                    now(), case when ? = 'DELIVERED' then now() else null end, now(), now()
                )
                returning id
                """, Long.class, "cleanup-" + status.toLowerCase(), requestId, status, status);
    }

    private void deliveryAttempt(Long notificationId, OffsetDateTime old) {
        jdbcTemplate.update("""
                insert into notification_delivery_attempts (
                    notification_id, attempt_number, worker_id, started_at, finished_at, outcome, created_at
                ) values (?, 1, 'cleanup-worker', ?, ?, 'DELIVERED', ?)
                """, notificationId, old, old, old);
    }

    private String attachmentStatus(String storageKey) {
        return jdbcTemplate.queryForObject(
                "select status from repair_attachments where storage_key = ?",
                String.class,
                storageKey);
    }

    private OffsetDateTime objectPurgedAt(String storageKey) {
        return jdbcTemplate.queryForObject(
                "select object_purged_at from repair_attachments where storage_key = ?",
                OffsetDateTime.class,
                storageKey);
    }

    private long count(String tableName) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
        return count == null ? 0 : count;
    }

    private long countWhere(String tableName, String predicate) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + tableName + " where " + predicate,
                Long.class);
        return count == null ? 0 : count;
    }
}
