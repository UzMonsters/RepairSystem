package com.example.darks.repair_auto.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class Phase6SchemaIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void givenPhase6SchemaThenRepairAttachmentObjectsExist() {
        assertThat(tableExists("repair_attachments")).isTrue();
        assertThat(constraintExists("repair_attachments", "repair_attachments_request_fk")).isTrue();
        assertThat(constraintExists("repair_attachments", "repair_attachments_uploaded_by_user_fk")).isTrue();
        assertThat(constraintExists("repair_attachments", "repair_attachments_deleted_by_user_fk")).isTrue();
        assertThat(constraintExists("repair_attachments", "repair_attachments_type_check")).isTrue();
        assertThat(constraintExists("repair_attachments", "repair_attachments_status_check")).isTrue();
        assertThat(constraintExists("repair_attachments", "repair_attachments_storage_key_unique")).isTrue();
        assertThat(constraintExists("repair_attachments", "repair_attachments_size_non_negative_check")).isTrue();
        assertThat(indexExists("idx_repair_attachments_request_id")).isTrue();
        assertThat(indexExists("idx_repair_attachments_request_type")).isTrue();
        assertThat(indexExists("idx_repair_attachments_status")).isTrue();
        assertThat(indexExists("idx_repair_attachments_uploaded_at")).isTrue();
        assertThat(indexExists("idx_repair_attachments_uploaded_by")).isTrue();
        assertThat(indexExists("idx_repair_attachments_available")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = ?
                """, Integer.class, tableName);
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
