package com.example.darks.repair_auto.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class Phase4SchemaIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void givenPhase4SchemaThenRepairAssignmentObjectsExist() {
        assertThat(tableExists("repair_assignments")).isTrue();
        assertThat(constraintExists("repair_assignments", "repair_assignments_request_fk")).isTrue();
        assertThat(constraintExists("repair_assignments", "repair_assignments_technician_fk")).isTrue();
        assertThat(constraintExists("repair_assignments", "repair_assignments_assigned_by_user_fk")).isTrue();
        assertThat(constraintExists("repair_assignments", "repair_assignments_status_check")).isTrue();
        assertThat(indexExists("idx_repair_assignments_request_id")).isTrue();
        assertThat(indexExists("idx_repair_assignments_technician_id")).isTrue();
        assertThat(indexExists("idx_repair_assignments_status")).isTrue();
        assertThat(indexExists("idx_repair_assignments_scheduled_visit_at")).isTrue();
        assertThat(indexExists("idx_repair_assignments_technician_active_workload")).isTrue();
        assertThat(indexExists("idx_repair_assignments_one_active_per_request")).isTrue();
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
