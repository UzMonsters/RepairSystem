package com.example.darks.repair_auto.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardIntegrationTest extends PostgreSqlIntegrationTest {

    private static final OffsetDateTime BEFORE_TODAY =
            OffsetDateTime.parse("2026-08-02T18:59:59Z");
    private static final OffsetDateTime BUSINESS_TODAY =
            OffsetDateTime.parse("2026-08-02T19:00:00Z");
    private static final OffsetDateTime TODAY_LATER =
            OffsetDateTime.parse("2026-08-03T08:00:00Z");

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

    private User admin;
    private User manager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from repair_requests");
        jdbcTemplate.update("delete from repair_categories");
        jdbcTemplate.update("delete from customers");
        jdbcTemplate.update("delete from technicians");
        jdbcTemplate.update("delete from refresh_sessions");
        userRepository.deleteAll();
        admin = createUser("Dashboard Admin", "dashboard-admin@example.com", "AdminPass123!", UserRole.ADMIN, true);
        manager = createUser(
                "Dashboard Manager",
                "dashboard-manager@example.com",
                "ManagerPass123!",
                UserRole.MANAGER,
                true);
    }

    @Test
    void emptyDatabaseReturnsZeroSafeDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/overview").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.generatedAt").value("2026-08-03T10:00:00Z"))
                .andExpect(jsonPath("$.businessDate").value("2026-08-03"))
                .andExpect(jsonPath("$.totalRequests").value(0))
                .andExpect(jsonPath("$.newToday").value(0))
                .andExpect(jsonPath("$.openRequests").value(0))
                .andExpect(jsonPath("$.averageRating").doesNotExist());

        mockMvc.perform(get("/api/v1/dashboard/request-trends")
                        .param("period", "7d")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("LAST_7_DAYS"))
                .andExpect(jsonPath("$.fromDate").value("2026-07-28"))
                .andExpect(jsonPath("$.toDate").value("2026-08-03"))
                .andExpect(jsonPath("$.buckets.length()").value(7))
                .andExpect(jsonPath("$.buckets[0].date").value("2026-07-28"))
                .andExpect(jsonPath("$.buckets[6].date").value("2026-08-03"))
                .andExpect(jsonPath("$.buckets[6].created").value(0));

        mockMvc.perform(get("/api/v1/dashboard/requests-by-status").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items.length()").value(7))
                .andExpect(jsonPath("$.items[0].count").value(0))
                .andExpect(jsonPath("$.items[0].label.en").value("New"))
                .andExpect(jsonPath("$.items[0].label.ru").value("Новая"))
                .andExpect(jsonPath("$.items[0].label.uz").value("Yangi"));

        mockMvc.perform(get("/api/v1/dashboard/requests-by-category").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("LAST_30_DAYS"))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.other.count").value(0));

        mockMvc.perform(get("/api/v1/dashboard/technicians").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTechnicians").value(0))
                .andExpect(jsonPath("$.availableCapacity").value(0));

        mockMvc.perform(get("/api/v1/dashboard/reviews").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(0))
                .andExpect(jsonPath("$.averageRating").doesNotExist())
                .andExpect(jsonPath("$.distribution.rating5").value(0));
    }

    @Test
    void populatedDashboardUsesBusinessTimezoneAndReadOnlyAggregateRules() throws Exception {
        SeedData data = seedDashboardData();

        mockMvc.perform(get("/api/v1/dashboard/overview").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessDate").value("2026-08-03"))
                .andExpect(jsonPath("$.totalRequests").value(8))
                .andExpect(jsonPath("$.newToday").value(6))
                .andExpect(jsonPath("$.openRequests").value(5))
                .andExpect(jsonPath("$.inProgress").value(1))
                .andExpect(jsonPath("$.waitingForParts").value(1))
                .andExpect(jsonPath("$.completedToday").value(1))
                .andExpect(jsonPath("$.completedTotal").value(2))
                .andExpect(jsonPath("$.cancelledTotal").value(1))
                .andExpect(jsonPath("$.activeTechnicians").value(2))
                .andExpect(jsonPath("$.techniciansWithActiveWork").value(2))
                .andExpect(jsonPath("$.pendingAssignments").value(1))
                .andExpect(jsonPath("$.averageRating").value(4.00))
                .andExpect(jsonPath("$.totalReviews").value(3));

        mockMvc.perform(get("/api/v1/dashboard/request-trends")
                        .param("period", "7d")
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets[5].date").value("2026-08-02"))
                .andExpect(jsonPath("$.buckets[5].created").value(2))
                .andExpect(jsonPath("$.buckets[5].completed").value(1))
                .andExpect(jsonPath("$.buckets[6].date").value("2026-08-03"))
                .andExpect(jsonPath("$.buckets[6].created").value(6))
                .andExpect(jsonPath("$.buckets[6].completed").value(1))
                .andExpect(jsonPath("$.buckets[6].cancelled").value(1));

        mockMvc.perform(get("/api/v1/dashboard/requests-by-category")
                        .param("period", "7d")
                        .param("limit", "2")
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(8))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].categoryId").value(data.categoryA()))
                .andExpect(jsonPath("$.items[0].nameEn").value("Air Conditioner"))
                .andExpect(jsonPath("$.items[0].nameRu").value("Кондиционер"))
                .andExpect(jsonPath("$.items[0].nameUz").value("Konditsioner"))
                .andExpect(jsonPath("$.items[0].count").value(4))
                .andExpect(jsonPath("$.items[1].categoryId").value(data.categoryB()))
                .andExpect(jsonPath("$.items[1].count").value(2))
                .andExpect(jsonPath("$.other.count").value(2));

        mockMvc.perform(get("/api/v1/dashboard/requests-by-status").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(8))
                .andExpect(jsonPath("$.items.length()").value(7))
                .andExpect(jsonPath("$.items[0].status").value("NEW"))
                .andExpect(jsonPath("$.items[0].count").value(2))
                .andExpect(jsonPath("$.items[3].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.items[3].count").value(1))
                .andExpect(jsonPath("$.items[6].status").value("CANCELLED"))
                .andExpect(jsonPath("$.items[6].count").value(1));

        mockMvc.perform(get("/api/v1/dashboard/technicians").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTechnicians").value(2))
                .andExpect(jsonPath("$.inactiveTechnicians").value(1))
                .andExpect(jsonPath("$.techniciansWithActiveWork").value(2))
                .andExpect(jsonPath("$.techniciansWithoutActiveWork").value(0))
                .andExpect(jsonPath("$.pendingAssignments").value(1))
                .andExpect(jsonPath("$.acceptedAssignments").value(2))
                .andExpect(jsonPath("$.inProgressRequests").value(1))
                .andExpect(jsonPath("$.waitingForPartsRequests").value(1))
                .andExpect(jsonPath("$.availableCapacity").value(3))
                .andExpect(jsonPath("$.totalCapacity").value(6));

        mockMvc.perform(get("/api/v1/dashboard/reviews").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(3))
                .andExpect(jsonPath("$.averageRating").value(4.00))
                .andExpect(jsonPath("$.reviewsWithComment").value(2))
                .andExpect(jsonPath("$.distribution.rating3").value(1))
                .andExpect(jsonPath("$.distribution.rating4").value(1))
                .andExpect(jsonPath("$.distribution.rating5").value(1));
    }

    @Test
    void validationSecurityAndSchemaControlsAreStable() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/v1/dashboard/request-trends")
                        .param("period", "90d")
                        .header("X-Trace-Id", "dashboard-invalid-period")
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Trace-Id", "dashboard-invalid-period"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.code").value("DASHBOARD_PERIOD_INVALID"))
                .andExpect(jsonPath("$.traceId").value("dashboard-invalid-period"));

        mockMvc.perform(get("/api/v1/dashboard/requests-by-category")
                        .param("limit", "21")
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DASHBOARD_CATEGORY_LIMIT_INVALID"));

        User disabled = createUser(
                "Disabled Dashboard User",
                "dashboard-disabled@example.com",
                "DisabledPass123!",
                UserRole.MANAGER,
                false);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(disabled.getEmail(), "DisabledPass123!")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        String managerToken = loginAndExtractAccessToken(manager.getEmail(), "ManagerPass123!");
        mockMvc.perform(get("/api/v1/dashboard/overview")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));

        assertThat(indexExists("idx_repair_executions_completed_at")).isTrue();
        assertThat(indexExists("idx_repair_executions_cancelled_at")).isTrue();
        assertThat(indexExists("idx_repair_requests_created_category")).isTrue();
    }

    private SeedData seedDashboardData() {
        Long customerId = customer("Dashboard Customer", "+998901111111");
        Long technicianA = technician("Technician A", "+998902222221", true, 4);
        Long technicianB = technician("Technician B", "+998902222222", true, 2);
        Long inactiveTechnician = technician("Inactive Technician", "+998902222223", false, 5);
        Long categoryA = category("Air Conditioner", "Кондиционер", "Konditsioner", true, 10);
        Long categoryB = category("Washer", "Стиральная машина", "Kir yuvish mashinasi", true, 20);
        Long categoryC = category("Archived Printer", "Архивный принтер", "Arxiv printer", false, 30);

        Long beforeNew = request(customerId, categoryA, "NEW", BEFORE_TODAY, "REP-DASH-001");
        Long todayNew = request(customerId, categoryA, "NEW", BUSINESS_TODAY, "REP-DASH-002");
        Long assigned = request(customerId, categoryA, "ASSIGNED", TODAY_LATER, "REP-DASH-003");
        Long inProgress = request(customerId, categoryA, "IN_PROGRESS", TODAY_LATER, "REP-DASH-004");
        Long waiting = request(customerId, categoryB, "WAITING_FOR_PARTS", TODAY_LATER, "REP-DASH-005");
        Long completedToday = request(customerId, categoryB, "COMPLETED", TODAY_LATER, "REP-DASH-006");
        Long completedYesterday = request(customerId, categoryC, "COMPLETED", BEFORE_TODAY, "REP-DASH-007");
        Long cancelled = request(customerId, categoryC, "CANCELLED", TODAY_LATER, "REP-DASH-008");

        assignment(assigned, technicianA, "PENDING", null);
        assignment(inProgress, technicianA, "ACCEPTED", null);
        assignment(waiting, technicianB, "ACCEPTED", null);
        assignment(completedToday, inactiveTechnician, "COMPLETED", OffsetDateTime.parse("2026-08-03T09:00:00Z"));
        assignment(cancelled, inactiveTechnician, "CANCELLED", OffsetDateTime.parse("2026-08-03T09:00:00Z"));

        execution(completedToday, "COMPLETED", OffsetDateTime.parse("2026-08-02T19:30:00Z"));
        execution(completedYesterday, "COMPLETED", OffsetDateTime.parse("2026-08-02T18:59:00Z"));
        execution(cancelled, "CANCELLED", OffsetDateTime.parse("2026-08-03T07:00:00Z"));

        review(completedToday, customerId, inactiveTechnician, 5, "Excellent repair.");
        review(completedYesterday, customerId, inactiveTechnician, 4, null);
        review(beforeNew, customerId, technicianA, 3, "Follow-up needed.");
        return new SeedData(categoryA, categoryB);
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

    private Long customer(String fullName, String phone) {
        return jdbcTemplate.queryForObject("""
                insert into customers (full_name, phone, preferred_language, registration_source)
                values (?, ?, 'UZ', 'ADMIN')
                returning id
                """, Long.class, fullName, phone);
    }

    private Long technician(String fullName, String phone, boolean active, int maximumConcurrentRequests) {
        return jdbcTemplate.queryForObject("""
                insert into technicians (
                    full_name, phone, specialization, maximum_concurrent_requests, active, preferred_language
                ) values (?, ?, 'General', ?, ?, 'UZ')
                returning id
                """, Long.class, fullName, phone, maximumConcurrentRequests, active);
    }

    private Long category(String nameEn, String nameRu, String nameUz, boolean active, int displayOrder) {
        String suffix = nameEn.toLowerCase().replace(" ", "-");
        return jdbcTemplate.queryForObject("""
                insert into repair_categories (
                    name_uz, name_ru, name_en,
                    name_uz_normalized, name_ru_normalized, name_en_normalized,
                    active, display_order
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                returning id
                """, Long.class, nameUz, nameRu, nameEn, suffix + "-uz", suffix + "-ru", suffix, active, displayOrder);
    }

    private Long request(Long customerId, Long categoryId, String status, OffsetDateTime createdAt, String number) {
        return jdbcTemplate.queryForObject("""
                insert into repair_requests (
                    request_number, customer_id, category_id, description, address, priority,
                    status, source, created_by_user_id, created_at, updated_at
                ) values (?, ?, ?, 'Dashboard seeded request.', 'Tashkent', 'NORMAL',
                    ?, 'ADMIN', ?, ?, ?)
                returning id
                """, Long.class, number, customerId, categoryId, status, admin.getId(), createdAt, createdAt);
    }

    private void assignment(Long requestId, Long technicianId, String status, OffsetDateTime closedAt) {
        OffsetDateTime now = closedAt == null ? TODAY_LATER : closedAt;
        OffsetDateTime respondedAt = status.equals("ACCEPTED") ? now : null;
        jdbcTemplate.update("""
                insert into repair_assignments (
                    repair_request_id, technician_id, status, assigned_by_user_id,
                    assigned_at, responded_at, closed_at, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, requestId, technicianId, status, admin.getId(), BUSINESS_TODAY, respondedAt, closedAt, BUSINESS_TODAY, now);
    }

    private void execution(Long requestId, String status, OffsetDateTime terminalAt) {
        OffsetDateTime startedAt = terminalAt.minusHours(1);
        if (status.equals("COMPLETED")) {
            jdbcTemplate.update("""
                    insert into repair_executions (
                        repair_request_id, started_at, started_by_user_id, diagnosis,
                        diagnosis_updated_at, diagnosis_updated_by_user_id, work_performed,
                        completed_at, completed_by_user_id, created_at, updated_at
                    ) values (?, ?, ?, 'Dashboard diagnosis.', ?, ?, 'Dashboard work performed.', ?, ?, ?, ?)
                    """,
                    requestId,
                    startedAt,
                    admin.getId(),
                    startedAt,
                    admin.getId(),
                    terminalAt,
                    admin.getId(),
                    startedAt,
                    terminalAt);
        } else {
            jdbcTemplate.update("""
                    insert into repair_executions (
                        repair_request_id, cancellation_reason, cancelled_at, cancelled_by_user_id, created_at, updated_at
                    ) values (?, 'Cancelled for dashboard test.', ?, ?, ?, ?)
                    """, requestId, terminalAt, admin.getId(), startedAt, terminalAt);
        }
    }

    private void review(Long requestId, Long customerId, Long technicianId, int rating, String comment) {
        jdbcTemplate.update("""
                insert into repair_reviews (
                    repair_request_id, customer_id, technician_id, rating, comment,
                    source, submitted_language, submitted_at, created_at
                ) values (?, ?, ?, ?, ?, 'TELEGRAM', 'UZ', ?, ?)
                """, requestId, customerId, technicianId, rating, comment, TODAY_LATER, TODAY_LATER);
    }

    private String loginAndExtractAccessToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from pg_indexes
                where schemaname = 'public' and indexname = ?
                """, Integer.class, indexName);
        return count != null && count > 0;
    }

    private record SeedData(Long categoryA, Long categoryB) {
    }

    @TestConfiguration
    static class DashboardTestClockConfiguration {

        @Bean
        @Primary
        Clock fixedDashboardClock() {
            return Clock.fixed(Instant.parse("2026-08-03T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
