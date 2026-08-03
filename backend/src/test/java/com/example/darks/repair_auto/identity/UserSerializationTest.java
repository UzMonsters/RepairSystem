package com.example.darks.repair_auto.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class UserSerializationTest {

    @Test
    void givenUserEntityWhenSerializedThenSensitiveAuthenticationFieldsAreNotEmitted() throws Exception {
        User user = new User(
                "Admin User",
                "admin@example.com",
                "$2a$12$sensitivePasswordHashValue",
                UserRole.ADMIN,
                true,
                OffsetDateTime.parse("2026-01-01T00:00:00Z"));

        String json = new ObjectMapper().writeValueAsString(user);

        assertThat(json).doesNotContain("passwordHash");
        assertThat(json).doesNotContain("sensitivePasswordHashValue");
        assertThat(json).doesNotContain("authVersion");
    }
}
