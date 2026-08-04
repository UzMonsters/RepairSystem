package com.example.darks.repair_auto.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class AuthVersionIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User admin;
    private User manager;

    @BeforeEach
    void setUp() {
        repairRequestRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("Admin", "admin@example.com", "AdminPass123!", UserRole.ADMIN, true);
        manager = createUser("Manager", "manager@example.com", "ManagerPass123!", UserRole.MANAGER, true);
    }

    @Test
    void givenIssuedTokenWhenValidatedThenItContainsCurrentAuthVersion() throws Exception {
        String accessToken = login("admin@example.com", "AdminPass123!").accessToken();

        JwtTokenService.ValidatedAccessToken token = jwtTokenService.validate(accessToken);

        assertThat(token.authVersion()).isEqualTo(userRepository.findById(admin.getId()).orElseThrow().getAuthVersion());
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void givenDbVersionGreaterThanJwtVersionWhenRequestThenTokenIsRejected() throws Exception {
        String accessToken = login("admin@example.com", "AdminPass123!").accessToken();
        transactionTemplate.executeWithoutResult(status -> userRepository.incrementAuthVersion(
                admin.getId(),
                OffsetDateTime.now(ZoneOffset.UTC)));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Trace-Id", "stale-token-trace"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Trace-Id", "stale-token-trace"))
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"))
                .andExpect(jsonPath("$.traceId").value("stale-token-trace"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void givenPasswordChangeWhenRequestUsesOldTokenThenItFailsImmediatelyAndNewLoginWorks() throws Exception {
        LoginTokens login = login("admin@example.com", "AdminPass123!");

        mockMvc.perform(patch("/api/v1/auth/password")
                        .header("Authorization", "Bearer " + login.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"AdminPass123!","newPassword":"NewAdminPass123!"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + login.accessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@example.com","password":"NewAdminPass123!"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void givenRoleChangeWhenRequestUsesOldManagerTokenThenItFailsAndNewLoginHasNewRole() throws Exception {
        String adminToken = login("admin@example.com", "AdminPass123!").accessToken();
        String managerToken = login("manager@example.com", "ManagerPass123!").accessToken();

        mockMvc.perform(patch("/api/v1/users/{id}/role", manager.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
        String newManagerToken = login("manager@example.com", "ManagerPass123!").accessToken();
        assertThat(jwtTokenService.validate(newManagerToken).role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void givenDeactivationAndReactivationWhenUsingOldTokenThenReactivationDoesNotRestoreIt() throws Exception {
        String adminToken = login("admin@example.com", "AdminPass123!").accessToken();
        String managerToken = login("manager@example.com", "ManagerPass123!").accessToken();

        setActive(adminToken, false);
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
        setActive(adminToken, true);
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void givenAdminRevokeSessionsAndLogoutAllWhenUsingOldTokensThenTheyFail() throws Exception {
        String adminToken = login("admin@example.com", "AdminPass123!").accessToken();
        String managerToken = login("manager@example.com", "ManagerPass123!").accessToken();

        mockMvc.perform(post("/api/v1/users/{id}/revoke-sessions", manager.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isUnauthorized());

        String freshAdminToken = login("admin@example.com", "AdminPass123!").accessToken();
        mockMvc.perform(post("/api/v1/auth/logout-all").header("Authorization", "Bearer " + freshAdminToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + freshAdminToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenSingleSessionLogoutWhenUsingAccessTokenThenItRemainsValidByPolicy() throws Exception {
        LoginTokens tokens = login("admin@example.com", "AdminPass123!");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens.refreshToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk());
    }

    private void setActive(String adminToken, boolean active) throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/activation", manager.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":%s}
                                """.formatted(active)))
                .andExpect(status().isOk());
    }

    private LoginTokens login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return new LoginTokens(
                JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken"),
                JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken"));
    }

    private User createUser(String fullName, String email, String password, UserRole role, boolean active) {
        return userRepository.saveAndFlush(new User(
                fullName,
                emailNormalizer.normalize(email),
                passwordService.hash(password),
                role,
                active,
                OffsetDateTime.now(ZoneOffset.UTC)));
    }

    private record LoginTokens(String accessToken, String refreshToken) {
    }
}
