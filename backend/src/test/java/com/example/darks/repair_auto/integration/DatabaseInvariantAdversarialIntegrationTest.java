package com.example.darks.repair_auto.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DatabaseInvariantAdversarialIntegrationTest extends PostgreSqlIntegrationTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 8, 22, 10, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;
    private Long customerId;
    private Long technicianId;
    private Long categoryId;
    private Long requestId;

    @BeforeEach
    void setUpInvariantFixtures() {
        cleanupOwnFixtures();
        userId = insertUser();
        customerId = insertCustomer();
        technicianId = insertTechnician();
        categoryId = insertCategory();
        requestId = insertRepairRequest("REP-ADV-001", "ADMIN", userId, null, "Tashkent, Chilonzor", null, null, "MANUAL");
    }

    @AfterEach
    void cleanupOwnFixtures() {
        jdbcTemplate.update("delete from chat_messages");
        jdbcTemplate.update("delete from conversation_participants");
        jdbcTemplate.update("delete from conversations");
        jdbcTemplate.update("delete from user_notifications");
        jdbcTemplate.update("delete from notification_push_deliveries");
        jdbcTemplate.update("delete from push_endpoints");
        jdbcTemplate.update("delete from mobile_refresh_sessions");
        jdbcTemplate.update("delete from telegram_updates");
        jdbcTemplate.update("delete from repair_attachments");
        jdbcTemplate.update("delete from repair_request_status_history");
        jdbcTemplate.update("delete from repair_executions");
        jdbcTemplate.update("delete from repair_assignments");
        jdbcTemplate.update("delete from repair_reviews");
        jdbcTemplate.update("delete from repair_requests");
        jdbcTemplate.update("delete from repair_categories");
        jdbcTemplate.update("delete from technicians");
        jdbcTemplate.update("delete from customers");
        jdbcTemplate.update("delete from user_settings");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void givenCorruptRepairRequestLocationOrSourceAttributionWhenInsertedThenDatabaseRejectsIt() {
        assertThatThrownBy(() -> insertRepairRequest(
                "REP-ADV-BAD-SOURCE",
                "MOBILE",
                userId,
                "mobile:customer:" + customerId,
                "Tashkent",
                null,
                null,
                "MANUAL"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> insertRepairRequest(
                "REP-ADV-BAD-LOCATION",
                "ADMIN",
                userId,
                null,
                null,
                "41.311081",
                null,
                "DEVICE_GPS"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> insertRepairRequest(
                "REP-ADV-BAD-LOCATION-SOURCE",
                "ADMIN",
                userId,
                null,
                "Tashkent",
                null,
                null,
                "SATELLITE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenSecondActiveAssignmentForSameRequestWhenInsertedThenDatabaseRejectsIt() {
        insertAssignment("PENDING", null, null, null);

        assertThatThrownBy(() -> insertAssignment("ACCEPTED", NOW, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenCorruptExecutionActorPairsWhenInsertedThenDatabaseRejectsThem() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into repair_executions (
                    repair_request_id, started_at, started_by_user_id, started_by_technician_id,
                    created_at, updated_at
                )
                values (?, ?, ?, ?, ?, ?)
                """, requestId, NOW, userId, technicianId, NOW, NOW))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into repair_executions (
                    repair_request_id, completed_at, completed_by_user_id, work_performed, created_at, updated_at
                )
                values (?, ?, ?, ?, ?, ?)
                """, requestId, NOW, userId, null, NOW, NOW))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenCorruptAttachmentUploaderOrStatusWhenInsertedThenDatabaseRejectsIt() {
        assertThatThrownBy(() -> insertAttachment(
                "adv/bad-uploader.jpg",
                "CUSTOMER_PROBLEM_PHOTO",
                "UPLOADING",
                userId,
                customerId,
                null,
                null,
                null,
                null,
                null))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> insertAttachment(
                "adv/bad-available.jpg",
                "CUSTOMER_PROBLEM_PHOTO",
                "AVAILABLE",
                userId,
                null,
                null,
                "image/jpeg",
                128L,
                null,
                NOW))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenCorruptMobileRefreshOrPushEndpointOwnershipWhenInsertedThenDatabaseRejectsIt() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into mobile_refresh_sessions (
                    actor_type, customer_id, technician_id, token_hash, token_family_id, issued_at, expires_at
                )
                values ('CUSTOMER', null, ?, 'hash-1', ?, ?, ?)
                """, technicianId, UUID.randomUUID(), NOW, NOW.plusDays(30)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into push_endpoints (
                    owner_type, staff_user_id, customer_id, technician_id, client_type, platform,
                    firebase_app_key, fcm_registration_token, last_seen_at
                )
                values ('CUSTOMER', null, ?, ?, 'CUSTOMER_MOBILE', 'ANDROID', 'CUSTOMER_ANDROID', 'fid-1', ?)
                """, customerId, technicianId, NOW))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenDuplicateTelegramUpdateOrUserNotificationWhenInsertedThenDatabaseRejectsIt() {
        insertTelegramUpdate(900001L);

        assertThatThrownBy(() -> insertTelegramUpdate(900001L))
                .isInstanceOf(DataIntegrityViolationException.class);

        insertUserNotification("event-1", customerId);

        assertThatThrownBy(() -> insertUserNotification("event-1", customerId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenCorruptNotificationOutboxLifecycleWhenInsertedThenDatabaseRejectsIt() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into notification_outbox (
                    event_key, notification_type, channel, recipient_type, recipient_id, repair_request_id,
                    template_key, payload_json, payload_version, status, attempt_count, next_attempt_at,
                    language, rendered_title, rendered_message, created_at, updated_at
                )
                values (
                    'bad-terminal', 'REQUEST_CREATED', 'TELEGRAM', 'CUSTOMER', ?, ?,
                    'notification.request.created', '{}', 1, 'DELIVERED', 1, ?,
                    'UZ', 'title', 'message', ?, ?
                )
                """, customerId, requestId, NOW, NOW, NOW))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into notification_outbox (
                    event_key, notification_type, channel, recipient_type, recipient_id, repair_request_id,
                    template_key, payload_json, payload_version, status, attempt_count, next_attempt_at,
                    language, rendered_title, rendered_message, created_at, updated_at
                )
                values (
                    'bad-processing', 'REQUEST_CREATED', 'TELEGRAM', 'CUSTOMER', ?, ?,
                    'notification.request.created', '{}', 1, 'PROCESSING', 1, ?,
                    'UZ', 'title', 'message', ?, ?
                )
                """, customerId, requestId, NOW, NOW, NOW))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenDuplicateConversationOrMessageIdempotencyKeyWhenInsertedThenDatabaseRejectsIt() {
        insertConversation("CUSTOMER_TECHNICIAN");

        assertThatThrownBy(() -> insertConversation("CUSTOMER_TECHNICIAN"))
                .isInstanceOf(DataIntegrityViolationException.class);

        Long conversationId = insertConversation("TECHNICIAN_MANAGER");
        insertChatMessage(conversationId, "client-msg-1");

        assertThatThrownBy(() -> insertChatMessage(conversationId, "client-msg-1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long insertUser() {
        return jdbcTemplate.queryForObject("""
                insert into users (full_name, email, password_hash, role, active, password_changed_at)
                values ('Adversarial Admin', 'adversarial-admin@example.com', 'hash', 'ADMIN', true, ?)
                returning id
                """, Long.class, NOW);
    }

    private Long insertCustomer() {
        return jdbcTemplate.queryForObject("""
                insert into customers (full_name, phone, preferred_language, registration_source)
                values ('Adversarial Customer', '+998901000001', 'UZ', 'ADMIN')
                returning id
                """, Long.class);
    }

    private Long insertTechnician() {
        return jdbcTemplate.queryForObject("""
                insert into technicians (full_name, phone, specialization, maximum_concurrent_requests, preferred_language)
                values ('Adversarial Technician', '+998901000002', 'HVAC', 5, 'UZ')
                returning id
                """, Long.class);
    }

    private Long insertCategory() {
        return jdbcTemplate.queryForObject("""
                insert into repair_categories (
                    name_uz, name_ru, name_uz_normalized, name_ru_normalized, active,
                    name_en, name_en_normalized
                )
                values (
                    'Adversarial Category', 'Adversarial Category RU', 'adversarial-category',
                    'adversarial-category-ru', true, 'Adversarial Category EN', 'adversarial-category-en'
                )
                returning id
                """, Long.class);
    }

    private Long insertRepairRequest(
            String number,
            String source,
            Long createdByUserId,
            String sourceReference,
            String locationAddress,
            String latitude,
            String longitude,
            String locationSource) {
        return jdbcTemplate.queryForObject("""
                insert into repair_requests (
                    request_number, customer_id, category_id, description, location_address,
                    location_latitude, location_longitude, location_source, priority, status, source,
                    created_by_user_id, source_reference, created_at, updated_at
                )
                values (?, ?, ?, ?, ?, ?::numeric, ?::numeric, ?, 'NORMAL', 'NEW', ?, ?, ?, ?, ?)
                returning id
                """, Long.class, number, customerId, categoryId, "Detailed adversarial repair description",
                locationAddress, latitude, longitude, locationSource, source, createdByUserId, sourceReference, NOW, NOW);
    }

    private void insertAssignment(String status, OffsetDateTime respondedAt, String rejectionReason, String closureReason) {
        OffsetDateTime closedAt = switch (status) {
            case "REJECTED", "UNASSIGNED", "REASSIGNED", "COMPLETED", "CANCELLED" -> NOW;
            default -> null;
        };
        jdbcTemplate.update("""
                insert into repair_assignments (
                    repair_request_id, technician_id, status, assigned_by_user_id, assigned_at,
                    responded_at, rejection_reason, closure_reason, closed_at, created_at, updated_at
                )
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, requestId, technicianId, status, userId, NOW, respondedAt, rejectionReason, closureReason,
                closedAt, NOW, NOW);
    }

    private void insertAttachment(
            String storageKey,
            String type,
            String status,
            Long uploaderUserId,
            Long uploaderCustomerId,
            Long uploaderTechnicianId,
            String contentType,
            Long sizeBytes,
            String checksum,
            OffsetDateTime availableAt) {
        jdbcTemplate.update("""
                insert into repair_attachments (
                    repair_request_id, attachment_type, status, storage_key, original_file_name,
                    content_type, size_bytes, sha256_checksum, uploaded_by_user_id,
                    uploaded_by_customer_id, uploaded_by_technician_id, uploaded_at, available_at,
                    created_at, updated_at
                )
                values (?, ?, ?, ?, 'photo.jpg', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, requestId, type, status, storageKey, contentType, sizeBytes, checksum, uploaderUserId,
                uploaderCustomerId, uploaderTechnicianId, NOW, availableAt, NOW, NOW);
    }

    private void insertTelegramUpdate(Long updateId) {
        jdbcTemplate.update("""
                insert into telegram_updates (
                    telegram_update_id, status, update_type, received_at, processed_at,
                    attempt_count, created_at, updated_at
                )
                values (?, 'PROCESSED', 'MESSAGE', ?, ?, 1, ?, ?)
                """, updateId, NOW, NOW, NOW, NOW);
    }

    private void insertUserNotification(String eventKey, Long recipientCustomerId) {
        jdbcTemplate.update("""
                insert into user_notifications (
                    event_key, notification_type, recipient_type, customer_id, repair_request_id,
                    request_number, target, target_id, payload_json, created_at, updated_at
                )
                values (?, 'REQUEST_CREATED', 'CUSTOMER', ?, ?, 'REP-ADV-001', 'REPAIR_REQUEST', ?, '{}', ?, ?)
                """, eventKey, recipientCustomerId, requestId, requestId, NOW, NOW);
    }

    private Long insertConversation(String type) {
        return jdbcTemplate.queryForObject("""
                insert into conversations (repair_request_id, conversation_type, status, created_at, updated_at)
                values (?, ?, 'ACTIVE', ?, ?)
                returning id
                """, Long.class, requestId, type, NOW, NOW);
    }

    private void insertChatMessage(Long conversationId, String clientMessageId) {
        jdbcTemplate.update("""
                insert into chat_messages (
                    conversation_id, sender_type, sender_id, client_message_id, message_type, text, created_at
                )
                values (?, 'CUSTOMER', ?, ?, 'TEXT', 'hello', ?)
                """, conversationId, customerId, clientMessageId, NOW);
    }
}
