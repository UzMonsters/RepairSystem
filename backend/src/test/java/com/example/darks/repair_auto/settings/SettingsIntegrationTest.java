package com.example.darks.repair_auto.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.settings.infrastructure.SystemSettingsRepository;
import com.example.darks.repair_auto.settings.infrastructure.UserSettingsRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SettingsIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    private User admin;
    private User manager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from notification_delivery_attempts");
        jdbcTemplate.update("delete from notification_outbox");
        jdbcTemplate.update("delete from user_notifications");
        jdbcTemplate.update("delete from repair_reviews");
        jdbcTemplate.update("delete from repair_attachments");
        jdbcTemplate.update("delete from repair_request_status_history");
        jdbcTemplate.update("delete from repair_executions");
        jdbcTemplate.update("delete from repair_assignments");
        jdbcTemplate.update("delete from repair_requests");
        jdbcTemplate.update("delete from refresh_sessions");
        userSettingsRepository.deleteAll();
        systemSettingsRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("Admin User", "admin@example.com", "AdminPass123!", UserRole.ADMIN);
        manager = createUser("Manager User", "manager@example.com", "ManagerPass123!", UserRole.MANAGER);
    }

    @Test
    void givenUserWhenNoUserSettingsExistThenDefaultsAreReturned() throws Exception {
        mockMvc.perform(get("/api/v1/settings/me")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("UZ"))
                .andExpect(jsonPath("$.dateFormat").value("DD_MM_YYYY"))
                .andExpect(jsonPath("$.timeFormat").value("HOUR_24"))
                .andExpect(jsonPath("$.theme").value("SYSTEM"));
    }

    @Test
    void givenUserWhenUpdatingSettingsThenUserSettingsAreSaved() throws Exception {
        mockMvc.perform(put("/api/v1/settings/me")
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "language": "RU",
                                  "dateFormat": "YYYY_MM_DD",
                                  "timeFormat": "HOUR_12",
                                  "theme": "DARK"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("RU"))
                .andExpect(jsonPath("$.dateFormat").value("YYYY_MM_DD"))
                .andExpect(jsonPath("$.timeFormat").value("HOUR_12"))
                .andExpect(jsonPath("$.theme").value("DARK"));

        var saved = userSettingsRepository.findByUserId(manager.getId());
        assertThat(saved).isPresent();
        assertThat(saved.get().getLanguage().name()).isEqualTo("RU");
    }

    @Test
    void givenManagerWhenUpdatingSystemSettingsThenForbiddenIsReturned() throws Exception {
        mockMvc.perform(put("/api/v1/settings/system")
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "timezone": "Asia/Tashkent",
                                  "defaultLanguage": "RU"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAdminWhenUpdatingSystemSettingsThenSettingsAreSaved() throws Exception {
        mockMvc.perform(put("/api/v1/settings/system")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "timezone": "Asia/Tashkent",
                                  "defaultLanguage": "RU"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Asia/Tashkent"))
                .andExpect(jsonPath("$.defaultLanguage").value("RU"));

        mockMvc.perform(get("/api/v1/settings/system")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Asia/Tashkent"))
                .andExpect(jsonPath("$.defaultLanguage").value("RU"));
    }

    @Test
    void givenAdminWhenUpdatingInvalidTimezoneThenBadRequestIsReturned() throws Exception {
        mockMvc.perform(put("/api/v1/settings/system")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "timezone": "Invalid/Timezone_That_Does_Not_Exist",
                                  "defaultLanguage": "UZ"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private User createUser(String fullName, String email, String password, UserRole role) {
        return userRepository.saveAndFlush(new User(
                fullName,
                emailNormalizer.normalize(email),
                passwordService.hash(password),
                role,
                true,
                OffsetDateTime.now(ZoneOffset.UTC)));
    }
}
