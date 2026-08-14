package com.example.darks.repair_auto.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRequest;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.execution.api.dto.CompleteRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.DiagnosisRequest;
import com.example.darks.repair_auto.repair.execution.application.RepairExecutionService;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairExecutionRepository;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairRequestStatusHistoryRepository;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.review.application.RepairReviewService;
import com.example.darks.repair_auto.review.infrastructure.RepairReviewRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.api.dto.TechnicianCreateRequest;
import com.example.darks.repair_auto.technician.application.TechnicianService;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RepairReviewIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepairReviewService reviewService;

    @Autowired
    private RepairReviewRepository reviewRepository;

    @Autowired
    private RepairExecutionService executionService;

    @Autowired
    private RepairAssignmentService assignmentService;

    @Autowired
    private RepairAssignmentRepository assignmentRepository;

    @Autowired
    private RepairAttachmentRepository attachmentRepository;

    @Autowired
    private RepairRequestService requestService;

    @Autowired
    private RepairRequestRepository requestRepository;

    @Autowired
    private RepairExecutionRepository executionRepository;

    @Autowired
    private RepairRequestStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RepairCategoryService categoryService;

    @Autowired
    private RepairCategoryRepository categoryRepository;

    @Autowired
    private TechnicianService technicianService;

    @Autowired
    private TechnicianRepository technicianRepository;

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
    private Long categoryId;
    private Long technicianId;
    private long telegramUserId;
    private long telegramChatId;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        statusHistoryRepository.deleteAll();
        executionRepository.deleteAll();
        assignmentRepository.deleteAll();
        attachmentRepository.deleteAll();
        requestRepository.deleteAll();
        technicianRepository.deleteAll();
        customerRepository.deleteAll();
        categoryRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("Admin", "review-admin@example.com", "AdminPass123!", UserRole.ADMIN);
        manager = createUser("Manager", "review-manager@example.com", "ManagerPass123!", UserRole.MANAGER);
        customerId = customerService.create(new CustomerCreateRequest("Ali Valiyev", "+998 90 111 22 33", LanguageCode.UZ)).id();
        telegramUserId = 71001L;
        telegramChatId = 81001L;
        linkTelegram(customerId, telegramUserId, telegramChatId);
        categoryId = categoryService.create(new CategoryCreateRequest(
                "Washer", "Стиральная машина", "Kir yuvish mashinasi", null, null, null, true)).id();
        technicianId = technicianService.create(new TechnicianCreateRequest(
                "Usta Karim", "+998 90 222 33 44", "Appliances", null, 5, LanguageCode.UZ, true)).id();
    }

    @Test
    void completedOwnedRequestCanBeReviewedOnceAndAdminCanReadSafeData() throws Exception {
        Long requestId = completedRequest("Reviewable completed request.");

        var response = reviewService.submitFromTelegram(
                telegramUserId,
                telegramChatId,
                requestId,
                5,
                "Great work. RU: отлично. UZ: zo'r.",
                LanguageCode.EN);

        assertThat(response.rating()).isEqualTo(5);
        assertThat(reviewRepository.existsByRepairRequestId(requestId)).isTrue();
        assertCode(runCatching(() -> reviewService.submitFromTelegram(
                telegramUserId,
                telegramChatId,
                requestId,
                4,
                "Second review",
                LanguageCode.EN)), "REVIEW_ALREADY_EXISTS");

        String body = mockMvc.perform(get("/api/v1/reviews")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("rating", "5")
                        .param("sort", "requestNumber,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].repairRequestId").value(requestId))
                .andExpect(jsonPath("$.content[0].customerName").value("Ali Valiyev"))
                .andExpect(jsonPath("$.content[0].technicianName").value("Usta Karim"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(body)
                .doesNotContain("+998")
                .doesNotContain("telegram")
                .doesNotContain("storageKey");

        mockMvc.perform(get("/api/v1/reviews/summary")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.00));
    }

    @Test
    void nonCompletedForeignOrInactiveCustomerReviewAttemptsAreRejected() {
        Long openRequest = createRequest("Not completed yet.");
        assertCode(runCatching(() -> reviewService.submitFromTelegram(
                telegramUserId,
                telegramChatId,
                openRequest,
                5,
                null,
                LanguageCode.UZ)), "REVIEW_REQUEST_NOT_COMPLETED");

        Long otherCustomerId = customerService.create(new CustomerCreateRequest(
                "Other Customer", "+998 90 333 44 55", LanguageCode.EN)).id();
        linkTelegram(otherCustomerId, 71002L, 81002L);
        Long completed = completedRequest("Foreign review target.");
        assertCode(runCatching(() -> reviewService.submitFromTelegram(
                71002L,
                81002L,
                completed,
                4,
                null,
                LanguageCode.EN)), "REVIEW_REQUEST_NOT_OWNED");

        customerService.changeActivation(customerId, false, "archived");
        assertCode(runCatching(() -> reviewService.submitFromTelegram(
                telegramUserId,
                telegramChatId,
                completed,
                4,
                null,
                LanguageCode.UZ)), "REVIEW_CUSTOMER_INACTIVE");
    }

    @Test
    void concurrentReviewSubmissionCreatesExactlyOneReview() throws Exception {
        Long requestId = completedRequest("Concurrent review target.");

        List<Object> results = runConcurrently(
                () -> reviewService.submitFromTelegram(telegramUserId, telegramChatId, requestId, 5, "one", LanguageCode.EN),
                () -> reviewService.submitFromTelegram(telegramUserId, telegramChatId, requestId, 4, "two", LanguageCode.EN));

        assertThat(results).hasSize(2);
        assertThat(results).filteredOn(result -> !(result instanceof Exception)).hasSize(1);
        assertThat(reviewRepository.findAll()).hasSize(1);
    }

    @Test
    void adminReviewApiRequiresAuthenticationAndValidPaging() throws Exception {
        mockMvc.perform(get("/api/v1/reviews"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));

        mockMvc.perform(get("/api/v1/reviews")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("sort", "phone,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Set-Cookie"));

        mockMvc.perform(get("/api/v1/reviews")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void phase10SchemaContainsReviewConstraintsIndexesAndSessionDraftColumns() {
        assertThat(tableExists("repair_reviews")).isTrue();
        assertThat(constraintExists("repair_reviews", "repair_reviews_request_unique")).isTrue();
        assertThat(constraintExists("repair_reviews", "repair_reviews_rating_check")).isTrue();
        assertThat(constraintExists("repair_reviews", "repair_reviews_source_check")).isTrue();
        assertThat(constraintExists("repair_reviews", "repair_reviews_language_check")).isTrue();
        assertThat(indexExists("idx_repair_reviews_customer_id")).isTrue();
        assertThat(indexExists("idx_repair_reviews_technician_id")).isTrue();
        assertThat(columnExists("repair_reviews", "version")).isTrue();
        assertThat(columnExists("telegram_customer_sessions", "review_request_id")).isTrue();
        assertThat(constraintExists(
                "telegram_customer_sessions",
                "telegram_customer_sessions_review_rating_check")).isTrue();
    }

    private Long completedRequest(String description) {
        Long requestId = createRequest(description);
        assignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin));
        assignmentService.accept(requestId, new AuthenticatedUser(manager));
        executionService.start(requestId, new AuthenticatedUser(admin));
        executionService.updateDiagnosis(requestId, new DiagnosisRequest("Motor failure."), new AuthenticatedUser(admin));
        addCompletionPhoto(requestId);
        executionService.complete(
                requestId,
                new CompleteRepairRequest("Replaced motor and tested.", null),
                new AuthenticatedUser(manager));
        return requestId;
    }

    private Long createRequest(String description) {
        return requestService.create(new RepairRequestCreateRequest(
                        customerId,
                        categoryId,
                        description,
                        "Tashkent",
                        null,
                        null,
                        RepairRequestPriority.NORMAL,
                        OffsetDateTime.now(ZoneOffset.UTC).plusDays(1),
                        null),
                new AuthenticatedUser(admin)).id();
    }

    private void addCompletionPhoto(Long requestId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        RepairAttachment attachment = new RepairAttachment(
                requestRepository.findById(requestId).orElseThrow(),
                AttachmentType.COMPLETION_PHOTO,
                "test/reviews/" + requestId + "/" + UUID.randomUUID(),
                "completion.jpg",
                admin,
                now);
        attachment.markAvailable(
                "image/jpeg",
                4,
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                now);
        attachmentRepository.saveAndFlush(attachment);
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

    private void linkTelegram(Long customerId, Long userId, Long chatId) {
        jdbcTemplate.update("""
                update customers
                set telegram_user_id = ?, telegram_chat_id = ?
                where id = ?
                """, userId, chatId, customerId);
    }

    private Object runCatching(Callable<?> action) {
        try {
            return action.call();
        } catch (Exception exception) {
            return exception;
        }
    }

    private void assertCode(Object result, String code) {
        assertThat(result)
                .isInstanceOf(BusinessRuleException.class)
                .extracting(exception -> ((BusinessRuleException) exception).code())
                .isEqualTo(code);
    }

    private List<Object> runConcurrently(Callable<?> firstAction, Callable<?> secondAction) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> runAfterStart(firstAction, start));
            var second = executor.submit(() -> runAfterStart(secondAction, start));
            start.countDown();
            return List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
        }
    }

    private Object runAfterStart(Callable<?> action, CountDownLatch start) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        return runCatching(action);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean constraintExists(String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.table_constraints
                where table_schema = 'public' and table_name = ? and constraint_name = ?
                """, Integer.class, tableName, constraintName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.columns
                where table_schema = 'public' and table_name = ? and column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from pg_indexes
                where schemaname = 'public' and indexname = ?
                """, Integer.class, indexName);
        return count != null && count > 0;
    }
}
