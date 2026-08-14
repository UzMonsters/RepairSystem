package com.example.darks.repair_auto.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class Phase2SchemaIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void givenMigratedDatabaseThenPhase2TablesConstraintsAndIndexesExist() {
        assertThat(tableExists("customers")).isTrue();
        assertThat(tableExists("technicians")).isTrue();
        assertThat(tableExists("repair_categories")).isTrue();

        assertThat(constraintExists("customers", "customers_phone_unique")).isTrue();
        assertThat(constraintExists("technicians", "technicians_phone_unique")).isTrue();
        assertThat(columnExists("technicians", "preferred_language")).isTrue();
        assertThat(constraintExists("customers", "customers_preferred_language_check")).isTrue();
        assertThat(constraintExists("technicians", "technicians_preferred_language_check")).isTrue();
        assertThat(columnExists("repair_categories", "name_en")).isTrue();
        assertThat(columnExists("repair_categories", "name_en_normalized")).isTrue();
        assertThat(columnExists("repair_categories", "description_en")).isTrue();
        assertThat(constraintExists("repair_categories", "repair_categories_name_en_normalized_unique")).isTrue();
        assertThat(constraintExists("repair_categories", "repair_categories_name_uz_normalized_unique")).isTrue();
        assertThat(constraintExists("repair_categories", "repair_categories_name_ru_normalized_unique")).isTrue();
        assertThat(constraintExists("technicians", "technicians_maximum_concurrent_requests_check")).isTrue();

        assertThat(indexExists("customers_telegram_user_id_unique")).isTrue();
        assertThat(indexExists("technicians_telegram_user_id_unique")).isTrue();
    }

    @Test
    void givenNullableTelegramIdsThenNullsAreAllowedButNonNullValuesAreUnique() {
        jdbcTemplate.update("""
                insert into customers(full_name, phone, preferred_language, registration_source, active, created_at, updated_at)
                values ('One', '+998901111111', 'UZ', 'ADMIN', true, now(), now())
                """);
        jdbcTemplate.update("""
                insert into customers(full_name, phone, preferred_language, registration_source, active, created_at, updated_at)
                values ('Two', '+998902222222', 'EN', 'ADMIN', true, now(), now())
                """);
        jdbcTemplate.update("""
                insert into customers(full_name, phone, telegram_user_id, preferred_language, registration_source, active, created_at, updated_at)
                values ('Three', '+998903333333', 42, 'UZ', 'ADMIN', true, now(), now())
                """);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into customers(full_name, phone, telegram_user_id, preferred_language, registration_source, active, created_at, updated_at)
                values ('Four', '+998904444444', 42, 'UZ', 'ADMIN', true, now(), now())
                """)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void givenPhase2SchemaThenExistingPhase1UsersRemainUsable() {
        jdbcTemplate.update("""
                insert into users(full_name, email, password_hash, role, active, password_changed_at, created_at, updated_at, auth_version)
                values ('Admin', 'phase2-admin@example.com', 'hash', 'ADMIN', true, now(), now(), now(), 1)
                """);

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from users where email = 'phase2-admin@example.com' and auth_version = 1",
                Integer.class);

        assertThat(count).isEqualTo(1);
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

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = 'public' and table_name = ? and column_name = ?
                """, Integer.class, tableName, columnName);
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
