package com.example.darks.repair_auto.shared.cleanup;

import com.example.darks.repair_auto.identity.application.AuthThrottleService;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TechnicalDataCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TechnicalDataCleanupService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectStorageService storageService;
    private final AuthThrottleService authThrottleService;
    private final CleanupProperties properties;
    private final CleanupMetrics metrics;
    private final Clock clock;

    public TechnicalDataCleanupService(
            JdbcTemplate jdbcTemplate,
            ObjectStorageService storageService,
            AuthThrottleService authThrottleService,
            CleanupProperties properties,
            CleanupMetrics metrics,
            Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.storageService = storageService;
        this.authThrottleService = authThrottleService;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.cleanup.interval:PT15M}")
    public void scheduledRun() {
        if (properties.enabled()) {
            runOnce();
        }
    }

    public CleanupResult runOnce() {
        int staleUploads = markStaleUploadsFailed();
        int deletedObjects = purgeDeletedAttachmentObjects();
        int technicalRows = cleanupExpiredTechnicalRows();
        return new CleanupResult(staleUploads, deletedObjects, technicalRows);
    }

    @Transactional
    public int markStaleUploadsFailed() {
        OffsetDateTime now = now();
        List<String> keys = jdbcTemplate.queryForList("""
                with claimed as (
                    select id
                    from repair_attachments
                    where status = 'UPLOADING'
                        and uploaded_at < ?
                    order by id
                    limit ?
                    for update skip locked
                )
                update repair_attachments a
                set status = 'FAILED',
                    failure_reason = 'STALE_UPLOAD',
                    updated_at = ?
                from claimed
                where a.id = claimed.id
                returning a.storage_key
                """, String.class, now.minus(properties.staleUploadThreshold()), properties.batchSize(), now);
        for (String key : keys) {
            try {
                storageService.delete(key);
            } catch (RuntimeException exception) {
                LOGGER.warn("cleanup_stale_upload_object_delete_failed");
            }
        }
        metrics.staleUploadsFailed(keys.size());
        return keys.size();
    }

    @Transactional
    public int purgeDeletedAttachmentObjects() {
        OffsetDateTime now = now();
        List<AttachmentObject> objects = jdbcTemplate.query("""
                select id, storage_key
                from repair_attachments
                where status = 'DELETED'
                    and object_purged_at is null
                    and deleted_at < ?
                order by id
                limit ?
                for update skip locked
                """, (rs, rowNum) -> new AttachmentObject(rs.getLong("id"), rs.getString("storage_key")),
                now.minus(properties.deletedObjectRetention()), properties.batchSize());
        int purged = 0;
        for (AttachmentObject object : objects) {
            try {
                storageService.delete(object.storageKey());
                jdbcTemplate.update("""
                        update repair_attachments
                        set object_purged_at = ?,
                            updated_at = ?
                        where id = ?
                        """, now, now, object.id());
                purged++;
            } catch (RuntimeException exception) {
                metrics.deletedObjectFailures();
                LOGGER.warn("cleanup_deleted_attachment_object_failed attachmentId={}", object.id());
            }
        }
        metrics.deletedObjectsPurged(purged);
        return purged;
    }

    @Transactional
    public int cleanupExpiredTechnicalRows() {
        OffsetDateTime now = now();
        int deleted = 0;
        deleted += jdbcTemplate.update("""
                delete from refresh_sessions
                where expires_at < ?
                    and (revoked_at is not null or used_at is not null)
                """, now.minus(properties.refreshSessionRetention()));
        deleted += jdbcTemplate.update("""
                delete from telegram_updates
                where status in ('PROCESSED', 'FAILED')
                    and received_at < ?
                """, now.minus(properties.telegramUpdateRetention()));
        deleted += jdbcTemplate.update("""
                delete from notification_delivery_attempts
                where created_at < ?
                    and notification_id in (
                        select id from notification_outbox
                        where status in ('DELIVERED', 'SKIPPED', 'DEAD')
                    )
                """, now.minus(properties.notificationAttemptRetention()));
        deleted += authThrottleService.cleanupExpiredEntries();
        metrics.technicalRowsDeleted(deleted);
        return deleted;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    public record CleanupResult(int staleUploadsFailed, int deletedObjectsPurged, int technicalRowsDeleted) {
    }

    private record AttachmentObject(Long id, String storageKey) {
    }
}
