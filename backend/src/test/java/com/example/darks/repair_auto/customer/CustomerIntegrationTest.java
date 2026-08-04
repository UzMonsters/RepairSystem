package com.example.darks.repair_auto.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.customer.api.dto.CustomerCreateRequest;
import com.example.darks.repair_auto.customer.application.CustomerService;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

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

    private User admin;
    private User manager;

    @BeforeEach
    void setUp() {
        repairRequestRepository.deleteAll();
        customerRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("Admin", "admin@example.com", "AdminPass123!", UserRole.ADMIN);
        manager = createUser("Manager", "manager@example.com", "ManagerPass123!", UserRole.MANAGER);
    }

    @Test
    void givenAdminOrManagerWhenCreatingCustomerThenCustomerIsStoredWithNormalizedPhone() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Ali Valiyev","phone":"+998 90 111 22 33","preferredLanguage":"UZ"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+998901112233"))
                .andExpect(jsonPath("$.registrationSource").value("ADMIN"))
                .andExpect(jsonPath("$.telegramLinked").value(false))
                .andExpect(jsonPath("$.telegramUserId").doesNotExist());

        mockMvc.perform(post("/api/v1/customers")
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Ivan Petrov","phone":"90 222 33 44","preferredLanguage":"RU"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+998902223344"))
                .andExpect(jsonPath("$.preferredLanguage").value("RU"));

        mockMvc.perform(post("/api/v1/customers")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"John Smith","phone":"90 555 66 77","preferredLanguage":"EN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredLanguage").value("EN"));
    }

    @Test
    void givenDuplicatePhoneWhenCreatingThenConflictIsReturned() throws Exception {
        createCustomer("Ali", "+998901112233", LanguageCode.UZ);

        mockMvc.perform(post("/api/v1/customers")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Other","phone":"90 111 22 33","preferredLanguage":"UZ"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMER_PHONE_ALREADY_EXISTS"));
    }

    @Test
    void givenCustomersWhenListingThenFiltersSortingAndBoundsAreEnforced() throws Exception {
        createCustomer("Alpha One", "+998901112233", LanguageCode.UZ);
        var archived = createCustomer("Beta Two", "+998902223344", LanguageCode.RU);
        customerService.changeActivation(archived.id(), false, "test");

        mockMvc.perform(get("/api/v1/customers")
                        .with(user(new AuthenticatedUser(manager)))
                        .param("search", "alpha")
                        .param("language", "UZ")
                        .param("active", "true")
                        .param("registrationSource", "ADMIN")
                        .param("sort", "fullName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Alpha One"));

        mockMvc.perform(get("/api/v1/customers")
                        .with(user(new AuthenticatedUser(manager)))
                        .param("search", "90 111 22 33"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].phone").value("+998901112233"));

        mockMvc.perform(get("/api/v1/customers")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("sort", "telegramUserId,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));

        mockMvc.perform(get("/api/v1/customers")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));
    }

    @Test
    void givenCustomerWhenUpdatingArchivingReactivatingOrLookingUpMissingThenExpectedResponsesReturn()
            throws Exception {
        Long id = createCustomer("Ali", "+998901112233", LanguageCode.UZ).id();

        mockMvc.perform(put("/api/v1/customers/{id}", id)
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Ali Updated","phone":"90 333 44 55","preferredLanguage":"RU"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ali Updated"))
                .andExpect(jsonPath("$.phone").value("+998903334455"));

        mockMvc.perform(patch("/api/v1/customers/{id}/activation", id)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false,"reason":"inactive"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/v1/customers/{id}/activation", id)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":true,"reason":"back"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/v1/customers/999999").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void givenAnonymousOrDeleteWhenCustomersEndpointRequestedThenDeniedOrMethodNotAllowed() throws Exception {
        Long id = createCustomer("Ali", "+998901112233", LanguageCode.UZ).id();

        mockMvc.perform(get("/api/v1/customers").header("X-Trace-Id", "customer-anonymous"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Trace-Id", "customer-anonymous"));

        mockMvc.perform(delete("/api/v1/customers/{id}", id).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void givenConcurrentDuplicatePhoneCreatesThenOnlyOneSucceeds() throws Exception {
        List<Object> results = runConcurrently(
                () -> customerService.create(new CustomerCreateRequest("One", "90 111 22 33", LanguageCode.UZ)),
                () -> customerService.create(new CustomerCreateRequest("Two", "+998901112233", LanguageCode.RU)));

        assertThat(results).anyMatch(result -> result instanceof BusinessRuleException
                && ((BusinessRuleException) result).code().equals("CUSTOMER_PHONE_ALREADY_EXISTS"));
        assertThat(customerRepository.count()).isEqualTo(1);
    }

    private com.example.darks.repair_auto.customer.api.dto.CustomerDetailResponse createCustomer(
            String fullName,
            String phone,
            LanguageCode language) {
        return customerService.create(new CustomerCreateRequest(fullName, phone, language));
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

    private List<Object> runConcurrently(Callable<?> firstAction, Callable<?> secondAction) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(() -> runAfterStart(firstAction, start));
            var secondResult = executor.submit(() -> runAfterStart(secondAction, start));
            start.countDown();
            return List.of(firstResult.get(15, TimeUnit.SECONDS), secondResult.get(15, TimeUnit.SECONDS));
        }
    }

    private Object runAfterStart(Callable<?> action, CountDownLatch start) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        try {
            return action.call();
        } catch (BusinessRuleException exception) {
            return exception;
        }
    }
}
