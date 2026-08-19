package com.example.darks.repair_auto.repair.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.catalog.category.api.dto.CategoryCreateRequest;
import com.example.darks.repair_auto.catalog.category.application.RepairCategoryService;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
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
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateResponse;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RepairRequestIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepairRequestService repairRequestService;

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RepairCategoryService repairCategoryService;

    @Autowired
    private RepairCategoryRepository repairCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User admin;
    private User manager;
    private Long customerId;
    private Long secondCustomerId;
    private Long categoryId;
    private Long secondCategoryId;

    @BeforeEach
    void setUp() {
        repairRequestRepository.deleteAll();
        customerRepository.deleteAll();
        repairCategoryRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("Admin", "admin@example.com", "AdminPass123!", UserRole.ADMIN);
        manager = createUser("Manager", "manager@example.com", "ManagerPass123!", UserRole.MANAGER);
        customerId = customerService.create(new CustomerCreateRequest("Ali Valiyev", "90 111 22 33", LanguageCode.UZ)).id();
        secondCustomerId = customerService.create(new CustomerCreateRequest("Ivan Petrov", "90 222 33 44", LanguageCode.RU)).id();
        categoryId = repairCategoryService.create(new CategoryCreateRequest(
                "Air Conditioner", "Konditsioner RU", "Konditsioner", null, null, null, true)).id();
        secondCategoryId = repairCategoryService.create(
                new CategoryCreateRequest("Phone", "Telefon RU", "Telefon", null, null, null, true)).id();
    }

    @Test
    void givenAdminOrManagerWhenCreatingRequestThenBackendControlledFieldsAreStored() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(customerId, categoryId, "NORMAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestNumber").value(org.hamcrest.Matchers.matchesPattern("REP-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.priority").value("NORMAL"))
                .andExpect(jsonPath("$.source").value("ADMIN"));

        mockMvc.perform(post("/api/v1/requests")
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(secondCustomerId, secondCategoryId, "HIGH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.source").value("ADMIN"));

        assertThat(repairRequestRepository.findAll())
                .extracting(request -> request.getCreatedByUser().getId())
                .contains(admin.getId(), manager.getId());
    }

    @Test
    void givenInvalidCreationInputsThenStableBusinessErrorsReturn() throws Exception {
        customerService.changeActivation(customerId, false, "archived");
        mockMvc.perform(post("/api/v1/requests")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(customerId, categoryId, "NORMAL")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPAIR_REQUEST_CUSTOMER_INACTIVE"));

        customerService.changeActivation(customerId, true, "active");
        repairCategoryService.changeActivation(categoryId, false, "archived");
        mockMvc.perform(post("/api/v1/requests")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(customerId, categoryId, "NORMAL")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPAIR_REQUEST_CATEGORY_INACTIVE"));

        repairCategoryService.changeActivation(categoryId, true, "active");
        mockMvc.perform(post("/api/v1/requests")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":%d,"categoryId":%d,"description":"short","address":"Tashkent"}
                                """.formatted(customerId, categoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REPAIR_REQUEST_DESCRIPTION"));

        mockMvc.perform(post("/api/v1/requests")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":%d,"categoryId":%d,"description":"The appliance makes loud noise.","latitude":41.2856}
                                """.formatted(customerId, categoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REPAIR_REQUEST_LOCATION"));

        mockMvc.perform(post("/api/v1/requests")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":%d,"categoryId":%d,"description":"The appliance makes loud noise.","address":"Tashkent","customerPreferredVisitAt":"2020-01-01T10:00:00Z"}
                                """.formatted(customerId, categoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PREFERRED_VISIT_TIME"));
    }

    @Test
    void givenRequestsWhenListingThenSearchFiltersSortingPaginationAndDateValidationWork() throws Exception {
        var first = repairRequestService.create(createRequest(customerId, categoryId, "Cooling problem near Chilanzar", "Chilanzar", "NORMAL"), new AuthenticatedUser(admin));
        repairRequestService.create(createRequest(secondCustomerId, secondCategoryId, "Phone screen is cracked badly", "Yunusabad", "URGENT"), new AuthenticatedUser(manager));

        mockMvc.perform(get("/api/v1/requests")
                        .with(user(new AuthenticatedUser(manager)))
                        .param("search", "90 111 22 33"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].requestNumber").value(first.requestNumber()))
                .andExpect(jsonPath("$.content[0].customerFullName").value("Ali Valiyev"));

        mockMvc.perform(get("/api/v1/requests")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("requestNumber", first.requestNumber())
                        .param("customerId", customerId.toString())
                        .param("categoryId", categoryId.toString())
                        .param("status", "NEW")
                        .param("priority", "NORMAL")
                        .param("source", "ADMIN")
                        .param("sort", "customerName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/requests")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));

        mockMvc.perform(get("/api/v1/requests")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("sort", "createdByUserId,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));

        mockMvc.perform(get("/api/v1/requests")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("createdFrom", "2026-08-02T00:00:00Z")
                        .param("createdTo", "2026-08-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_DATE_RANGE"));
    }

    @Test
    void givenRequestWhenViewingOrUpdatingThenHistoricalReferencesAndEditabilityRulesApply() throws Exception {
        var created = repairRequestService.create(createRequest(customerId, categoryId, "AC does not cool the room", "Chilanzar", "NORMAL"), new AuthenticatedUser(admin));
        customerService.changeActivation(customerId, false, "historical");
        repairCategoryService.changeActivation(categoryId, false, "historical");

        mockMvc.perform(get("/api/v1/requests/{id}", created.id()).with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.active").value(false))
                .andExpect(jsonPath("$.category.active").value(false))
                .andExpect(jsonPath("$.internalNote").value("Customer prefers afternoon"));

        mockMvc.perform(put("/api/v1/requests/{id}", created.id())
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(secondCustomerId, secondCategoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestNumber").value(created.requestNumber()))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.source").value("ADMIN"))
                .andExpect(jsonPath("$.customer.id").value(secondCustomerId));

        jdbcTemplate.update("update repair_requests set status = 'ASSIGNED' where id = ?", created.id());
        mockMvc.perform(put("/api/v1/requests/{id}", created.id())
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody(secondCustomerId, secondCategoryId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPAIR_REQUEST_NOT_EDITABLE"));
    }

    @Test
    void givenCustomerHistoryWhenQueriedThenOnlyThatCustomersRequestsReturn() throws Exception {
        var first = repairRequestService.create(createRequest(customerId, categoryId, "AC does not cool the room", "Chilanzar", "NORMAL"), new AuthenticatedUser(admin));
        repairRequestService.create(createRequest(secondCustomerId, secondCategoryId, "Phone screen is cracked badly", "Yunusabad", "URGENT"), new AuthenticatedUser(admin));
        customerService.changeActivation(customerId, false, "history stays available");

        mockMvc.perform(get("/api/v1/customers/{customerId}/requests", customerId)
                        .with(user(new AuthenticatedUser(manager)))
                        .param("status", "NEW")
                        .param("priority", "NORMAL")
                        .param("categoryId", categoryId.toString())
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(first.id()))
                .andExpect(jsonPath("$.content[0].customerFullName").value("Ali Valiyev"));

        mockMvc.perform(get("/api/v1/customers/999999/requests").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    @Test
    void givenConcurrentCreationThenRequestNumbersRemainUnique() throws Exception {
        List<Object> results = runConcurrently(
                () -> repairRequestService.create(createRequest(customerId, categoryId, "AC does not cool the room", "Chilanzar", "NORMAL"), new AuthenticatedUser(admin)),
                () -> repairRequestService.create(createRequest(secondCustomerId, secondCategoryId, "Phone screen is cracked badly", "Yunusabad", "HIGH"), new AuthenticatedUser(manager)));

        assertThat(results).hasSize(2);
        assertThat(results)
                .allMatch(RepairRequestCreateResponse.class::isInstance)
                .extracting(result -> ((RepairRequestCreateResponse) result).requestNumber())
                .doesNotHaveDuplicates();
    }

    @Test
    void givenAnonymousOrDeleteWhenRequestsEndpointRequestedThenDeniedOrMethodNotAllowed() throws Exception {
        var created = repairRequestService.create(createRequest(customerId, categoryId, "AC does not cool the room", "Chilanzar", "NORMAL"), new AuthenticatedUser(admin));

        mockMvc.perform(get("/api/v1/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));

        mockMvc.perform(delete("/api/v1/requests/{id}", created.id()).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isMethodNotAllowed());
    }

    private RepairRequestCreateRequest createRequest(
            Long customerId,
            Long categoryId,
            String description,
            String address,
            String priority) {
        return new RepairRequestCreateRequest(
                customerId,
                categoryId,
                description,
                address,
                null,
                null,
                RepairRequestPriority.valueOf(priority),
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(1),
                "Customer prefers afternoon");
    }

    private String requestBody(Long customerId, Long categoryId, String priority) {
        return """
                {"customerId":%d,"categoryId":%d,"description":"The air conditioner starts but does not cool the room.","address":"Chilanzar district, Tashkent","latitude":41.285600,"longitude":69.203400,"priority":"%s","customerPreferredVisitAt":"%s","internalNote":"Customer prefers an afternoon visit."}
                """.formatted(customerId, categoryId, priority, OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).withNano(0));
    }

    private String updateBody(Long customerId, Long categoryId) {
        return """
                {"customerId":%d,"categoryId":%d,"description":"Updated description of the appliance problem.","address":"Updated address","latitude":41.285600,"longitude":69.203400,"priority":"HIGH","customerPreferredVisitAt":"%s","internalNote":"Customer changed the preferred visit time."}
                """.formatted(customerId, categoryId, OffsetDateTime.now(ZoneOffset.UTC).plusDays(3).withNano(0));
    }

    private List<Object> runConcurrently(Callable<?> firstAction, Callable<?> secondAction) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> runAfterStart(firstAction, start));
            var second = executor.submit(() -> runAfterStart(secondAction, start));
            start.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private Object runAfterStart(Callable<?> action, CountDownLatch start) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        try {
            return action.call();
        } catch (Exception exception) {
            return exception;
        }
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
