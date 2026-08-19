package com.example.darks.repair_auto.identity.infrastructure.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.settings.infrastructure.SystemSettingsRepository;
import com.example.darks.repair_auto.settings.infrastructure.UserSettingsRepository;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.error.ApiErrorResponseFactory;
import com.example.darks.repair_auto.shared.error.SecurityErrorHandler;
import com.example.darks.repair_auto.shared.i18n.JsonTranslationCatalog;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.i18n.LocalizationServiceImpl;
import com.example.darks.repair_auto.shared.i18n.RequestLocaleResolver;
import com.example.darks.repair_auto.shared.observability.TraceIdFilter;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class MobileSecurityMockMvcTest {

    private static final String SECRET = "test-local-only-jwt-secret-that-is-long-enough";
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private MockMvc mockMvc;
    private JwtTokenService jwtTokenService;
    private UserRepository userRepository;
    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;

    @RestController
    static class TestSecuredController {

        @GetMapping("/api/v1/test/staff-only")
        public ResponseEntity<Map<String, Object>> staffOnly(@AuthenticationPrincipal AuthenticatedUser user) {
            if (user == null) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(Map.of("role", user.role().name(), "userId", user.id()));
        }

        @GetMapping("/api/v1/test/customer-only")
        public ResponseEntity<Map<String, Object>> customerOnly(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
            if (actor == null || !actor.isCustomer()) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(Map.of("actorType", actor.actorType().name(), "actorId", actor.actorId()));
        }

        @GetMapping("/api/v1/test/technician-only")
        public ResponseEntity<Map<String, Object>> technicianOnly(@AuthenticationPrincipal AuthenticatedMobileActor actor) {
            if (actor == null || !actor.isTechnician()) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(Map.of("actorType", actor.actorType().name(), "actorId", actor.actorId()));
        }
    }

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
                new AppProperties.Cors(List.of(), List.of(), List.of(), List.of(), false),
                new AppProperties.Trace("X-Trace-Id"),
                new AppProperties.Jwt(SECRET, "repair-auto", Duration.ofMinutes(15)),
                Duration.ofDays(1),
                Duration.ofDays(30),
                new AppProperties.BootstrapAdmin(false, "", "", ""));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonTranslationCatalog catalog = new JsonTranslationCatalog(objectMapper);
        catalog.init();

        UserSettingsRepository userSettingsRepository = mock(UserSettingsRepository.class);
        SystemSettingsRepository systemSettingsRepository = mock(SystemSettingsRepository.class);
        RequestLocaleResolver requestLocaleResolver = new RequestLocaleResolver(userSettingsRepository, systemSettingsRepository);
        LocalizationServiceImpl localizationService = new LocalizationServiceImpl(catalog, requestLocaleResolver);
        TraceIdService traceIdService = new TraceIdService();
        ApiErrorResponseFactory responseFactory = new ApiErrorResponseFactory(traceIdService);
        SecurityErrorHandler securityErrorHandler = new SecurityErrorHandler(properties, localizationService, responseFactory, objectMapper);

        jwtTokenService = new JwtTokenService(properties, new tools.jackson.databind.ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
        userRepository = mock(UserRepository.class);
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(
                jwtTokenService,
                userRepository,
                customerRepository,
                technicianRepository,
                securityErrorHandler);

        TraceIdFilter traceFilter = new TraceIdFilter(properties, traceIdService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestSecuredController())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters(traceFilter, jwtFilter)
                .build();
    }

    @Test
    void givenValidCustomerTokenWhenCallingCustomerRouteThenAuthenticatesSuccessfully() throws Exception {
        Customer customer = new Customer("Test Customer", "+998901234567", LanguageCode.UZ, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 100L);
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));

        String token = jwtTokenService.issueMobile(ActorType.CUSTOMER, 100L, "+998901234567");

        mockMvc.perform(get("/api/v1/test/customer-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actorType").value("CUSTOMER"))
                .andExpect(jsonPath("$.actorId").value(100));

        verify(customerRepository).findById(100L);
        verify(userRepository, never()).findById(any());
        verify(technicianRepository, never()).findById(any());
    }

    @Test
    void givenValidTechnicianTokenWhenCallingTechnicianRouteThenAuthenticatesSuccessfully() throws Exception {
        Technician technician = new Technician("Test Tech", "+998909876543", "Electronics", "Notes", 5, LanguageCode.UZ, true, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 200L);
        when(technicianRepository.findById(200L)).thenReturn(Optional.of(technician));

        String token = jwtTokenService.issueMobile(ActorType.TECHNICIAN, 200L, "+998909876543");

        mockMvc.perform(get("/api/v1/test/technician-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actorType").value("TECHNICIAN"))
                .andExpect(jsonPath("$.actorId").value(200));

        verify(technicianRepository).findById(200L);
        verify(userRepository, never()).findById(any());
        verify(customerRepository, never()).findById(any());
    }

    @Test
    void givenValidStaffTokenWhenCallingStaffRouteThenAuthenticatesSuccessfully() throws Exception {
        User user = new User("Admin", "admin@example.com", "hash", UserRole.ADMIN, true, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "authVersion", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String token = jwtTokenService.issue(user);

        mockMvc.perform(get("/api/v1/test/staff-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.userId").value(1));

        verify(userRepository).findById(1L);
        verify(customerRepository, never()).findById(any());
        verify(technicianRepository, never()).findById(any());
    }

    @Test
    void givenInactiveCustomerTokenWhenCallingThenUnauthorizedReturned() throws Exception {
        Customer customer = new Customer("Inactive Customer", "+998901234567", LanguageCode.UZ, OffsetDateTime.now(ZoneOffset.UTC));
        customer.setActive(false, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(customer, "id", 101L);
        when(customerRepository.findById(101L)).thenReturn(Optional.of(customer));

        String token = jwtTokenService.issueMobile(ActorType.CUSTOMER, 101L);

        mockMvc.perform(get("/api/v1/test/customer-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void givenInactiveTechnicianTokenWhenCallingThenUnauthorizedReturned() throws Exception {
        Technician technician = new Technician("Inactive Tech", "+998909876543", "Electronics", "Notes", 5, LanguageCode.UZ, false, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(technician, "id", 201L);
        when(technicianRepository.findById(201L)).thenReturn(Optional.of(technician));

        String token = jwtTokenService.issueMobile(ActorType.TECHNICIAN, 201L);

        mockMvc.perform(get("/api/v1/test/technician-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void givenUnknownCustomerIdWhenCallingThenUnauthorizedReturned() throws Exception {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        String token = jwtTokenService.issueMobile(ActorType.CUSTOMER, 999L);

        mockMvc.perform(get("/api/v1/test/customer-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void givenCustomerTokenWithTechnicianIdWhenCustomerNotFoundThenNeverQueriesTechnician() throws Exception {
        when(customerRepository.findById(55L)).thenReturn(Optional.empty());

        String token = jwtTokenService.issueMobile(ActorType.CUSTOMER, 55L);

        mockMvc.perform(get("/api/v1/test/customer-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));

        verify(customerRepository).findById(55L);
        verify(technicianRepository, never()).findById(any());
    }

    @Test
    void givenLegacyStaffTokenWithoutActorTypeWhenCallingStaffRouteThenAuthenticatesSuccessfully() throws Exception {
        User user = new User("Admin", "legacy@example.com", "hash", UserRole.ADMIN, true, OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(user, "id", 5L);
        ReflectionTestUtils.setField(user, "authVersion", 3L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        String legacyToken = legacyStaffToken(5L, "legacy@example.com", UserRole.ADMIN, 3L);

        mockMvc.perform(get("/api/v1/test/staff-only").header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.userId").value(5));
    }

    private String legacyStaffToken(Long userId, String email, UserRole role, long authVersion) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "repair-auto");
        claims.put("sub", email);
        claims.put("userId", userId);
        claims.put("role", role.name());
        claims.put("authVersion", authVersion);
        claims.put("iat", NOW.getEpochSecond());
        claims.put("exp", NOW.plus(Duration.ofMinutes(15)).getEpochSecond());
        claims.put("jti", "legacy-jti-123");
        claims.put("tokenType", "access");
        String unsigned = encode(header) + "." + encode(claims);
        return unsigned + "." + ReflectionTestUtils.invokeMethod(jwtTokenService, "sign", unsigned);
    }

    private String encode(Map<String, Object> claims) {
        try {
            return java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(new tools.jackson.databind.ObjectMapper().writeValueAsBytes(claims));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
