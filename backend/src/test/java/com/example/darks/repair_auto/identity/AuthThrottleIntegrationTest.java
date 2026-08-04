package com.example.darks.repair_auto.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthThrottleIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from refresh_sessions");
        jdbcTemplate.update("delete from repair_requests");
        jdbcTemplate.update("delete from users");
        userRepository.saveAndFlush(new User(
                "Admin User",
                emailNormalizer.normalize("admin@example.com"),
                passwordService.hash("AdminPass123!"),
                UserRole.ADMIN,
                true,
                OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void givenRepeatedInvalidLoginWhenThresholdIsExceededThenLoginIsThrottledWithoutLeakingSessionCookies()
            throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType("application/json")
                            .content("""
                                    {"email":"admin@example.com","password":"WrongPass123!"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().doesNotExist("Set-Cookie"));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"admin@example.com","password":"AdminPass123!"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.code").value("AUTH_THROTTLED"));

        Integer entries = jdbcTemplate.queryForObject("""
                select count(*) from auth_throttle_entries
                where throttle_key like 'login:%'
                    and blocked_until is not null
                """, Integer.class);
        assertThat(entries).isEqualTo(1);
    }

    @Test
    void givenRepeatedInvalidRefreshWhenThresholdIsExceededThenRefreshIsThrottled()
            throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType("application/json")
                            .content("""
                                    {"refreshToken":"not-a-refresh-token"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("""
                                {"refreshToken":"not-a-refresh-token"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AUTH_THROTTLED"));

        Integer entries = jdbcTemplate.queryForObject("""
                select count(*) from auth_throttle_entries
                where throttle_key like 'refresh:%'
                    and blocked_until is not null
                """, Integer.class);
        assertThat(entries).isEqualTo(1);
    }
}
