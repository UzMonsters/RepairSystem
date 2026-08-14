package com.example.darks.repair_auto.repair.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRejectionRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.ReassignmentRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.ScheduleRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.UnassignmentRequest;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestUpdateRequest;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.api.dto.TechnicianCreateRequest;
import com.example.darks.repair_auto.technician.application.TechnicianService;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
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
class RepairAssignmentIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepairAssignmentService repairAssignmentService;

    @Autowired
    private RepairAssignmentRepository repairAssignmentRepository;

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

    private User admin;
    private User manager;
    private Long customerId;
    private Long categoryId;
    private Long requestId;
    private Long technicianId;
    private Long secondTechnicianId;

    @BeforeEach
    void setUp() {
        repairAssignmentRepository.deleteAll();
        repairRequestRepository.deleteAll();
        technicianRepository.deleteAll();
        customerRepository.deleteAll();
        repairCategoryRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("Admin", "admin@example.com", "AdminPass123!", UserRole.ADMIN);
        manager = createUser("Manager", "manager@example.com", "ManagerPass123!", UserRole.MANAGER);
        customerId = customerService.create(new CustomerCreateRequest("Ali Valiyev", "90 111 22 33", LanguageCode.UZ)).id();
        categoryId = repairCategoryService.create(new CategoryCreateRequest(
                "Air Conditioner", "Konditsioner RU", "Konditsioner", null, null, null, true)).id();
        technicianId = createTechnician("Alex", "+998902223344", 2, true);
        secondTechnicianId = createTechnician("Botir", "+998903334455", 2, true);
        requestId = createRequest(customerId, categoryId, "The appliance starts but does not cool the room.");
    }

    @Test
    void givenAdminOrManagerWhenAssigningThenRequestStatusAndCurrentAssignmentReturn() throws Exception {
        mockMvc.perform(post("/api/v1/requests/{requestId}/assign", requestId)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"technicianId":%d}
                                """.formatted(technicianId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.currentAssignment.status").value("PENDING"))
                .andExpect(jsonPath("$.currentAssignment.technician.id").value(technicianId));

        Long anotherRequest = createRequest(customerId, categoryId, "The second appliance has a noisy compressor.");
        mockMvc.perform(post("/api/v1/requests/{requestId}/assign", anotherRequest)
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"technicianId":%d,"scheduledVisitAt":"2026-08-05T10:00:00+05:00"}
                                """.formatted(secondTechnicianId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.currentAssignment.scheduledVisitAt").exists());
    }

    @Test
    void givenInvalidAssignmentInputsThenStableErrorsReturn() throws Exception {
        Long inactiveTechnician = createTechnician("Inactive", "+998904445566", 1, false);
        mockMvc.perform(post("/api/v1/requests/{requestId}/assign", requestId)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"technicianId":%d}
                                """.formatted(inactiveTechnician)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TECHNICIAN_INACTIVE"));

        mockMvc.perform(post("/api/v1/requests/{requestId}/assign", requestId)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"technicianId":%d,"scheduledVisitAt":"2020-01-01T10:00:00Z"}
                                """.formatted(technicianId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCHEDULED_VISIT_TIME"));

        repairAssignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin));
        mockMvc.perform(post("/api/v1/requests/{requestId}/assign", requestId)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"technicianId":%d}
                                """.formatted(technicianId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPAIR_REQUEST_ALREADY_ASSIGNED"));
    }

    @Test
    void givenCapacityFullWhenAssigningThenCapacityErrorReturns() {
        Long limitedTechnician = createTechnician("Limited", "+998905556677", 1, true);
        repairAssignmentService.assign(requestId, new AssignmentRequest(limitedTechnician, null), new AuthenticatedUser(admin));
        Long anotherRequest = createRequest(customerId, categoryId, "The second appliance needs a technician visit.");

        assertThat(runCatching(() -> repairAssignmentService.assign(
                anotherRequest,
                new AssignmentRequest(limitedTechnician, null),
                new AuthenticatedUser(manager))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(result -> ((BusinessRuleException) result).code())
                .isEqualTo("TECHNICIAN_CAPACITY_EXCEEDED");
    }

    @Test
    void givenActiveAssignmentWhenAcceptRejectUnassignReassignOrScheduleThenRulesApply() throws Exception {
        repairAssignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin));

        mockMvc.perform(patch("/api/v1/requests/{requestId}/schedule", requestId)
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scheduledVisitAt":"2026-08-07T11:30:00+05:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        mockMvc.perform(post("/api/v1/requests/{requestId}/assignment/accept", requestId)
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAssignment.status").value("ACCEPTED"));

        mockMvc.perform(patch("/api/v1/requests/{requestId}/schedule", requestId)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clearSchedule":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.currentAssignment.scheduledVisitAt").doesNotExist());

        mockMvc.perform(post("/api/v1/requests/{requestId}/reassign", requestId)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"technicianId":%d,"scheduledVisitAt":"2026-08-08T11:30:00+05:00","reason":"Original technician unavailable"}
                                """.formatted(secondTechnicianId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.currentAssignment.technician.id").value(secondTechnicianId));

        mockMvc.perform(post("/api/v1/requests/{requestId}/assignment/reject", requestId)
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Outside service area"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.currentAssignment").doesNotExist());

        mockMvc.perform(get("/api/v1/requests/{requestId}/assignments", requestId)
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[1].status").value("REASSIGNED"));
    }

    @Test
    void givenRejectWithoutReasonOrAnonymousRequestsThenExpectedErrorsReturn() throws Exception {
        repairAssignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin));

        mockMvc.perform(post("/api/v1/requests/{requestId}/assignment/reject", requestId)
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(get("/api/v1/requests/{requestId}/assignments", requestId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/technicians/{technicianId}/workload", technicianId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenWorkloadAndArchivedTechnicianThenHistoricalVisibilityRemains() throws Exception {
        repairAssignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin));
        repairAssignmentService.accept(requestId, new AuthenticatedUser(admin));

        mockMvc.perform(get("/api/v1/technicians/{technicianId}/workload", technicianId)
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAssignments").value(0))
                .andExpect(jsonPath("$.acceptedAssignments").value(1))
                .andExpect(jsonPath("$.totalActiveAssignments").value(1))
                .andExpect(jsonPath("$.available").value(true));

        technicianService.changeActivation(technicianId, false, "archived");

        mockMvc.perform(get("/api/v1/requests/{requestId}", requestId)
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAssignment.technician.active").value(false));
    }

    @Test
    void givenUnassignmentThenRequestReturnsToNewAndHistoryIsClosed() throws Exception {
        repairAssignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin));

        mockMvc.perform(post("/api/v1/requests/{requestId}/unassign", requestId)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Customer requested a different date"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.currentAssignment").doesNotExist());

        assertThat(repairAssignmentRepository.findByRepairRequestIdOrderByCreatedAtDesc(requestId))
                .extracting(assignment -> assignment.getStatus())
                .containsExactly(AssignmentStatus.UNASSIGNED);
    }

    @Test
    void givenConcurrentAssignmentsToSameRequestThenOnlyOneActiveAssignmentRemains() throws Exception {
        List<Object> results = runConcurrently(
                () -> repairAssignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin)),
                () -> repairAssignmentService.assign(requestId, new AssignmentRequest(secondTechnicianId, null), new AuthenticatedUser(manager)));

        assertThat(results).anyMatch(result -> result instanceof BusinessRuleException);
        assertThat(repairAssignmentRepository.findByRepairRequestIdOrderByCreatedAtDesc(requestId))
                .filteredOn(assignment -> assignment.isActive())
                .hasSize(1);
    }

    @Test
    void givenConcurrentCapacityUseThenOnlyOneFinalSlotIsConsumed() throws Exception {
        Long limitedTechnician = createTechnician("Limited", "+998905556677", 1, true);
        Long firstRequest = createRequest(customerId, categoryId, "The first appliance needs the only slot.");
        Long secondRequest = createRequest(customerId, categoryId, "The second appliance also needs the only slot.");

        List<Object> results = runConcurrently(
                () -> repairAssignmentService.assign(firstRequest, new AssignmentRequest(limitedTechnician, null), new AuthenticatedUser(admin)),
                () -> repairAssignmentService.assign(secondRequest, new AssignmentRequest(limitedTechnician, null), new AuthenticatedUser(manager)));

        assertThat(results).anyMatch(result -> result instanceof BusinessRuleException
                && ((BusinessRuleException) result).code().equals("TECHNICIAN_CAPACITY_EXCEEDED"));
        assertThat(repairAssignmentRepository.countByTechnicianIdAndStatusIn(
                limitedTechnician,
                RepairAssignmentRepository.ACTIVE_STATUSES)).isEqualTo(1);
    }

    @Test
    void givenConcurrentWorkflowOperationsThenStateRemainsConsistent() throws Exception {
        repairAssignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin));

        List<Object> reassignAndUnassign = runConcurrently(
                () -> repairAssignmentService.reassign(
                        requestId,
                        new ReassignmentRequest(secondTechnicianId, null, "change"),
                        new AuthenticatedUser(admin)),
                () -> repairAssignmentService.unassign(
                        requestId,
                        new UnassignmentRequest("unassign"),
                        new AuthenticatedUser(manager)));

        assertThat(reassignAndUnassign).hasSize(2);
        assertThat(repairAssignmentRepository.findByRepairRequestIdOrderByCreatedAtDesc(requestId))
                .filteredOn(assignment -> assignment.isActive())
                .hasSizeLessThanOrEqualTo(1);
        assertThat(repairRequestService.get(requestId).status())
                .isIn(RepairRequestStatus.NEW, RepairRequestStatus.ASSIGNED, RepairRequestStatus.SCHEDULED);

        Long anotherRequest = createRequest(customerId, categoryId, "Another appliance needs response race coverage.");
        repairAssignmentService.assign(anotherRequest, new AssignmentRequest(secondTechnicianId, null), new AuthenticatedUser(admin));
        List<Object> acceptAndReject = runConcurrently(
                () -> repairAssignmentService.accept(anotherRequest, new AuthenticatedUser(admin)),
                () -> repairAssignmentService.reject(
                        anotherRequest,
                        new AssignmentRejectionRequest("reject"),
                        new AuthenticatedUser(manager)));

        assertThat(acceptAndReject).anyMatch(result -> result instanceof BusinessRuleException);
        assertThat(repairAssignmentRepository.findByRepairRequestIdOrderByCreatedAtDesc(anotherRequest))
                .filteredOn(assignment -> assignment.isActive())
                .hasSizeLessThanOrEqualTo(1);
    }

    @Test
    void givenConcurrentScheduleUpdatesThenAssignmentRemainsConsistent() throws Exception {
        repairAssignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin));
        OffsetDateTime firstVisit = OffsetDateTime.parse("2026-08-09T12:00:00+05:00");
        OffsetDateTime secondVisit = OffsetDateTime.parse("2026-08-10T12:00:00+05:00");

        List<Object> results = runConcurrently(
                () -> repairAssignmentService.schedule(
                        requestId,
                        new ScheduleRequest(firstVisit, false),
                        new AuthenticatedUser(admin)),
                () -> repairAssignmentService.schedule(
                        requestId,
                        new ScheduleRequest(secondVisit, false),
                        new AuthenticatedUser(manager)));

        assertThat(results).hasSize(2);
        assertThat(repairAssignmentRepository.findByRepairRequestIdOrderByCreatedAtDesc(requestId))
                .filteredOn(assignment -> assignment.isActive())
                .hasSize(1)
                .first()
                .extracting(assignment -> assignment.getScheduledVisitAt())
                .isIn(
                        firstVisit.withOffsetSameInstant(ZoneOffset.UTC),
                        secondVisit.withOffsetSameInstant(ZoneOffset.UTC));
        assertThat(repairRequestService.get(requestId).status()).isEqualTo(RepairRequestStatus.SCHEDULED);
    }

    @Test
    void givenConcurrentScheduleAndIntakeUpdateThenNoStateIsOverwritten() throws Exception {
        repairAssignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin));

        List<Object> results = runConcurrently(
                () -> repairAssignmentService.schedule(
                        requestId,
                        new ScheduleRequest(OffsetDateTime.parse("2026-08-09T12:00:00+05:00"), false),
                        new AuthenticatedUser(admin)),
                () -> repairRequestService.update(
                        requestId,
                        new RepairRequestUpdateRequest(
                                customerId,
                                categoryId,
                                "Updated intake description while assignment is racing.",
                                "Updated address",
                                null,
                                null,
                                RepairRequestPriority.HIGH,
                                OffsetDateTime.parse("2026-08-06T10:00:00+05:00"),
                                "Updated note")));

        assertThat(results).anyMatch(result -> result instanceof BusinessRuleException
                && ((BusinessRuleException) result).code().equals("REPAIR_REQUEST_NOT_EDITABLE"));
        var detail = repairRequestService.get(requestId);
        assertThat(detail.currentAssignment()).isNotNull();
        assertThat(detail.status()).isIn(RepairRequestStatus.ASSIGNED, RepairRequestStatus.SCHEDULED);
    }

    private Long createRequest(Long customerId, Long categoryId, String description) {
        return repairRequestService.create(new RepairRequestCreateRequest(
                        customerId,
                        categoryId,
                        description,
                        "Tashkent",
                        null,
                        null,
                        RepairRequestPriority.NORMAL,
                        OffsetDateTime.now(ZoneOffset.UTC).plusDays(2),
                        "Initial note"),
                new AuthenticatedUser(admin)).id();
    }

    private Long createTechnician(String fullName, String phone, int maximumConcurrentRequests, boolean active) {
        return technicianService.create(new TechnicianCreateRequest(
                fullName,
                phone,
                "AC",
                null,
                maximumConcurrentRequests,
                LanguageCode.UZ,
                active)).id();
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

    private Object runCatching(Callable<?> action) {
        try {
            return action.call();
        } catch (Exception exception) {
            return exception;
        }
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
        return runCatching(action);
    }
}
