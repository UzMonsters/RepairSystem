package com.example.darks.repair_auto.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.MobileSessionService;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.domain.MobileSession;
import com.example.darks.repair_auto.identity.domain.MobileSessionRevocationReason;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MobileSecurityIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    @Autowired
    private MobileSessionService mobileSessionService;

    private User admin;
    private User manager;
    private Customer activeCustomer;
    private Customer inactiveCustomer;
    private Technician activeTechnician;
    private Technician inactiveTechnician;

    @BeforeEach
    void setUp() {
        repairRequestRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        customerRepository.deleteAll();
        technicianRepository.deleteAll();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        admin = createUser("Admin", "admin@example.com", "AdminPass123!", UserRole.ADMIN);
        manager = createUser("Manager", "manager@example.com", "ManagerPass123!", UserRole.MANAGER);

        activeCustomer = customerRepository.saveAndFlush(new Customer(
                "Active Customer",
                "+998901112233",
                LanguageCode.UZ,
                now));

        inactiveCustomer = new Customer(
                "Inactive Customer",
                "+998904445566",
                LanguageCode.RU,
                now);
        inactiveCustomer.setActive(false, now);
        inactiveCustomer = customerRepository.saveAndFlush(inactiveCustomer);

        activeTechnician = technicianRepository.saveAndFlush(new Technician(
                "Active Tech",
                "+998907778899",
                "Electronics",
                "Notes",
                5,
                LanguageCode.UZ,
                true,
                now));

        inactiveTechnician = new Technician(
                "Inactive Tech",
                "+998900001122",
                "Engines",
                "Notes",
                3,
                LanguageCode.EN,
                false,
                now);
        inactiveTechnician = technicianRepository.saveAndFlush(inactiveTechnician);
    }

    private String issueCustomerToken(Customer customer) {
        MobileSession session = mobileSessionService.createForCustomer(
                customer,
                MobileAuthProvider.PHONE,
                null,
                "127.0.0.1",
                "MobileSecurityIntegrationTest");
        return jwtTokenService.issueMobile(
                ActorType.CUSTOMER,
                customer.getId(),
                customer.getAuthVersion(),
                session.getId(),
                PushClientType.CUSTOMER_MOBILE,
                customer.getPhone());
    }

    private String issueTechnicianToken(Technician technician) {
        MobileSession session = mobileSessionService.createForTechnician(
                technician,
                MobileAuthProvider.PHONE,
                null,
                "127.0.0.1",
                "MobileSecurityIntegrationTest");
        return jwtTokenService.issueMobile(
                ActorType.TECHNICIAN,
                technician.getId(),
                technician.getAuthVersion(),
                session.getId(),
                PushClientType.TECHNICIAN_MOBILE,
                technician.getPhone());
    }

    @Test
    void givenActiveCustomerTokenWhenAccessingAdminRouteThenForbiddenIsReturned() throws Exception {
        String token = issueCustomerToken(activeCustomer);

        mockMvc.perform(get("/api/v1/requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void givenActiveTechnicianTokenWhenAccessingAdminRouteThenForbiddenIsReturned() throws Exception {
        String token = issueTechnicianToken(activeTechnician);

        mockMvc.perform(get("/api/v1/requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/technicians").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void givenInactiveCustomerTokenWhenAuthenticatingThenInvalidAccessTokenIsReturned() throws Exception {
        UUID sessionId = UUID.randomUUID();
        String token = jwtTokenService.issueMobile(
                ActorType.CUSTOMER,
                inactiveCustomer.getId(),
                inactiveCustomer.getAuthVersion(),
                sessionId,
                PushClientType.CUSTOMER_MOBILE,
                inactiveCustomer.getPhone());

        mockMvc.perform(get("/api/v1/requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void givenInactiveTechnicianTokenWhenAuthenticatingThenInvalidAccessTokenIsReturned() throws Exception {
        UUID sessionId = UUID.randomUUID();
        String token = jwtTokenService.issueMobile(
                ActorType.TECHNICIAN,
                inactiveTechnician.getId(),
                inactiveTechnician.getAuthVersion(),
                sessionId,
                PushClientType.TECHNICIAN_MOBILE,
                inactiveTechnician.getPhone());

        mockMvc.perform(get("/api/v1/requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void givenUnknownCustomerIdTokenWhenAuthenticatingThenInvalidAccessTokenIsReturned() throws Exception {
        UUID sessionId = UUID.randomUUID();
        String token = jwtTokenService.issueMobile(
                ActorType.CUSTOMER,
                999999L,
                0L,
                sessionId,
                PushClientType.CUSTOMER_MOBILE,
                "+998909999999");

        mockMvc.perform(get("/api/v1/requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void givenUnknownTechnicianIdTokenWhenAuthenticatingThenInvalidAccessTokenIsReturned() throws Exception {
        UUID sessionId = UUID.randomUUID();
        String token = jwtTokenService.issueMobile(
                ActorType.TECHNICIAN,
                999999L,
                0L,
                sessionId,
                PushClientType.TECHNICIAN_MOBILE,
                "+998909999999");

        mockMvc.perform(get("/api/v1/requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void givenCustomerTokenWithRevokedSessionWhenAuthenticatingThenUnauthorized() throws Exception {
        MobileSession session = mobileSessionService.createForCustomer(
                activeCustomer,
                MobileAuthProvider.PHONE,
                null,
                "127.0.0.1",
                "RevocationTest");

        String token = jwtTokenService.issueMobile(
                ActorType.CUSTOMER,
                activeCustomer.getId(),
                activeCustomer.getAuthVersion(),
                session.getId(),
                PushClientType.CUSTOMER_MOBILE,
                activeCustomer.getPhone());

        // Revoke the session
        mobileSessionService.revoke(session.getId(), ActorType.CUSTOMER, activeCustomer.getId(), MobileSessionRevocationReason.LOGOUT);

        mockMvc.perform(get("/api/v1/requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenExistingStaffAdminTokenWhenAccessingAdminRouteThenSucceeds() throws Exception {
        String token = jwtTokenService.issue(admin);

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    void givenExistingStaffManagerTokenWhenAccessingManagerRouteThenSucceeds() throws Exception {
        String token = jwtTokenService.issue(manager);

        mockMvc.perform(get("/api/v1/requests").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("manager@example.com"));
    }

    @Test
    void givenLegacyStaffTokenWithoutActorTypeClaimWhenAccessingStaffRouteThenSucceeds() throws Exception {
        String legacyToken = createLegacyStaffToken(admin.getId(), admin.getEmail(), UserRole.ADMIN, admin.getAuthVersion());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    private User createUser(String fullName, String email, String password, UserRole role) {
        String normalizedEmail = emailNormalizer.normalize(email);
        User user = new User(
                fullName,
                normalizedEmail,
                passwordService.hash(password),
                role,
                true,
                OffsetDateTime.now(ZoneOffset.UTC));
        return userRepository.saveAndFlush(user);
    }

    private String createLegacyStaffToken(Long userId, String email, UserRole role, long authVersion) {
        Instant now = Instant.now();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "repair-auto");
        claims.put("sub", email);
        claims.put("userId", userId);
        claims.put("role", role.name());
        claims.put("authVersion", authVersion);
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plus(Duration.ofMinutes(15)).getEpochSecond());
        claims.put("jti", "legacy-test-jti");
        claims.put("tokenType", "access");
        String unsigned = encode(header) + "." + encode(claims);
        return unsigned + "." + ReflectionTestUtils.invokeMethod(jwtTokenService, "sign", unsigned);
    }

    private String encode(Map<String, Object> claims) {
        try {
            return java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(new ObjectMapper().writeValueAsBytes(claims));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
