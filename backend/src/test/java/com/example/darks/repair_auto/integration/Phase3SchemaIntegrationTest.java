package com.example.darks.repair_auto.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class Phase3SchemaIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void givenPhase3SchemaThenRepairRequestObjectsExist() {
        assertThat(tableExists("repair_requests")).isTrue();
        assertThat(sequenceExists("repair_request_number_seq")).isTrue();
        assertThat(constraintExists("repair_requests", "repair_requests_request_number_unique")).isTrue();
        assertThat(constraintExists("repair_requests", "repair_requests_customer_fk")).isTrue();
        assertThat(constraintExists("repair_requests", "repair_requests_category_fk")).isTrue();
        assertThat(constraintExists("repair_requests", "repair_requests_created_by_user_fk")).isTrue();
        assertThat(constraintExists("repair_requests", "repair_requests_status_check")).isTrue();
        assertThat(constraintExists("repair_requests", "repair_requests_priority_check")).isTrue();
        assertThat(constraintExists("repair_requests", "repair_requests_source_check")).isTrue();
        assertThat(constraintExists("repair_requests", "repair_requests_location_coordinate_pair_check")).isTrue();
        assertThat(constraintExists("repair_requests", "repair_requests_location_source_check")).isTrue();
        assertThat(indexExists("idx_repair_requests_customer_id")).isTrue();
        assertThat(indexExists("idx_repair_requests_created_at")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean sequenceExists(String sequenceName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.sequences
                where sequence_schema = 'public' and sequence_name = ?
                """, Integer.class, sequenceName);
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
