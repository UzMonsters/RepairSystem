package com.example.darks.repair_auto.shared.cleanup;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CleanupMetrics {

    private final Counter staleUploadsFailed;
    private final Counter deletedObjectsPurged;
    private final Counter deletedObjectFailures;
    private final Counter technicalRowsDeleted;

    public CleanupMetrics(MeterRegistry registry) {
        this.staleUploadsFailed = Counter.builder("repairauto.attachments.stale_uploads.failed")
                .description("Stale uploading attachment records marked failed.")
                .register(registry);
        this.deletedObjectsPurged = Counter.builder("repairauto.attachments.deleted_objects.purged")
                .description("Soft-deleted attachment objects purged from storage.")
                .register(registry);
        this.deletedObjectFailures = Counter.builder("repairauto.attachments.deleted_objects.cleanup_failures")
                .description("Deleted attachment object cleanup failures.")
                .register(registry);
        this.technicalRowsDeleted = Counter.builder("repairauto.cleanup.technical_rows.deleted")
                .description("Expired technical rows deleted by cleanup jobs.")
                .register(registry);
    }

    void staleUploadsFailed(int count) {
        staleUploadsFailed.increment(count);
    }

    void deletedObjectsPurged(int count) {
        deletedObjectsPurged.increment(count);
    }

    void deletedObjectFailures() {
        deletedObjectFailures.increment();
    }

    void technicalRowsDeleted(int count) {
        technicalRowsDeleted.increment(count);
    }
}
