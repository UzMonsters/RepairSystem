package com.example.darks.repair_auto.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FlywayMigrationV41UnitTest {

    @Test
    void migrationV41_containsTechnicianRejectedInConstraint() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V41__add_technician_rejected_notification_type.sql");
        assertThat(resource.exists()).isTrue();

        String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(sql).contains("alter table notification_outbox drop constraint if exists notification_outbox_type_check;");
        assertThat(sql).contains("add constraint notification_outbox_type_check check (notification_type in (");
        assertThat(sql).contains("'TECHNICIAN_REJECTED'");
        assertThat(sql).contains("'TECHNICIAN_ASSIGNED'");
        assertThat(sql).contains("'TECHNICIAN_UNASSIGNED'");
    }
}
