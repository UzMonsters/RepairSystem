package com.example.darks.repair_auto.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.auth.domain.RefreshSessionRepository;
import com.example.darks.repair_auto.auth.service.EmailNormalizer;
import com.example.darks.repair_auto.auth.service.PasswordService;
import com.example.darks.repair_auto.security.AuthenticatedUser;
import com.example.darks.repair_auto.user.domain.User;
import com.example.darks.repair_auto.user.domain.UserRepository;
import com.example.darks.repair_auto.user.domain.UserRole;
import com.jayway.jsonpath.JsonPath;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFoundationIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    private User admin;
    private User manager;

    @BeforeEach
    void setUp() {
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("Admin", "admin@example.com", "AdminPass123!", UserRole.ADMIN);
        manager = createUser("Manager", "manager@example.com", "ManagerPass123!", UserRole.MANAGER);
    }

    @Test
    void givenNoAuthenticationWhenApiRouteIsDeniedThenIncomingTraceIdIsPreservedAndNoSessionIsCreated()
            throws Exception {
        mockMvc.perform(get("/api/v1/protected").header("X-Trace-Id", "audit-trace-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Trace-Id", "audit-trace-123"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.traceId").value("audit-trace-123"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void givenNoTraceIdWhenApiRouteIsDeniedThenGeneratedTraceIdMatchesHeaderAndBody()
            throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andReturn();

        String headerTraceId = result.getResponse().getHeader("X-Trace-Id");
        String bodyTraceId = JsonPath.read(result.getResponse().getContentAsString(), "$.traceId");
        assertThat(headerTraceId).isNotBlank();
        assertThat(bodyTraceId).isEqualTo(headerTraceId);
    }

    @Test
    void givenAuthenticatedPrincipalWhenDeniedRouteIsRequestedThenForbiddenResponseHasTraceAndNoSession()
            throws Exception {
        mockMvc.perform(get("/phase0-denied")
                        .with(user("phase0-auditor"))
                        .header("X-Trace-Id", "forbidden-trace"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Trace-Id", "forbidden-trace"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.traceId").value("forbidden-trace"));
    }

    @Test
    void givenPhase0WhenUserLookupOccursThenNoDefaultUserExists() {
        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> userDetailsService.loadUserByUsername("user")))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void givenPublicAuthEndpointWhenAnonymousRequestThenSecurityDoesNotBlockItBeforeValidation()
            throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenManagerPrincipalWhenUsersEndpointRequestedThenForbiddenIsReturned()
            throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(user(new AuthenticatedUser(manager)))
                        .header("X-Trace-Id", "manager-denied-trace"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Trace-Id", "manager-denied-trace"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void givenAdminPrincipalWhenUsersEndpointRequestedThenAccessIsAllowed()
            throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void givenAdminBearerTokenWhenUsersEndpointRequestedThenAccessIsAllowed()
            throws Exception {
        String accessToken = loginAndExtractAccessToken("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void givenManagerBearerTokenWhenUsersEndpointRequestedThenForbiddenIsReturned()
            throws Exception {
        String accessToken = loginAndExtractAccessToken("manager@example.com", "ManagerPass123!");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Trace-Id", "manager-jwt-denied"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Trace-Id", "manager-jwt-denied"))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void givenInvalidBearerTokenWhenProtectedEndpointRequestedThenUnauthorizedIsReturned()
            throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
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

    private String loginAndExtractAccessToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
