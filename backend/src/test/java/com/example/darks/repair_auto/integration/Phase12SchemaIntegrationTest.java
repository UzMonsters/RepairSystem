package com.example.darks.repair_auto.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class Phase12SchemaIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void givenPhase12SchemaThenThrottleAndCleanupObjectsExist() {
        assertThat(tableExists("auth_throttle_entries")).isTrue();
        assertThat(columnExists("repair_attachments", "object_purged_at")).isTrue();
        assertThat(constraintExists("auth_throttle_entries", "auth_throttle_entries_key_unique")).isTrue();
        assertThat(constraintExists("auth_throttle_entries", "auth_throttle_entries_failed_attempts_check")).isTrue();
        assertThat(indexExists("idx_auth_throttle_entries_blocked_until")).isTrue();
        assertThat(indexExists("idx_auth_throttle_entries_updated_at")).isTrue();
        assertThat(indexExists("idx_repair_attachments_stale_uploading")).isTrue();
        assertThat(indexExists("idx_repair_attachments_deleted_cleanup")).isTrue();
        assertThat(indexExists("idx_refresh_sessions_expires_at")).isTrue();
        assertThat(indexExists("idx_telegram_updates_status_received_at")).isTrue();
        assertThat(indexExists("idx_notification_delivery_attempts_created_at")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = 'public' and table_name = ? and column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean constraintExists(String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.table_constraints
                where table_schema = 'public' and table_name = ? and constraint_name = ?
                """, Integer.class, tableName, constraintName);
        return count != null && count > 0;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from pg_indexes
                where schemaname = 'public' and indexname = ?
                """, Integer.class, indexName);
        return count != null && count > 0;
    }
}
