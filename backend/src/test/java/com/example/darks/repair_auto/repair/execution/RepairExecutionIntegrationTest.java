package com.example.darks.repair_auto.repair.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.example.darks.repair_auto.repair.assignment.api.dto.ReassignmentRequest;
import com.example.darks.repair_auto.repair.assignment.api.dto.UnassignmentRequest;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.execution.api.dto.CancelRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.CompleteRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.DiagnosisRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.ResumeRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.WaitForPartsRequest;
import com.example.darks.repair_auto.repair.execution.application.RepairExecutionService;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairExecutionRepository;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairRequestStatusHistoryRepository;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RepairExecutionIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepairExecutionService repairExecutionService;

    @Autowired
    private RepairExecutionRepository repairExecutionRepository;

    @Autowired
    private RepairRequestStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private RepairAssignmentService repairAssignmentService;

    @Autowired
    private RepairAssignmentRepository repairAssignmentRepository;

    @Autowired
    private RepairAttachmentRepository repairAttachmentRepository;

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
    private Long technicianId;
    private Long secondTechnicianId;

    @BeforeEach
    void setUp() {
        statusHistoryRepository.deleteAll();
        repairExecutionRepository.deleteAll();
        repairAssignmentRepository.deleteAll();
        repairAttachmentRepository.deleteAll();
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
                "Air Conditioner", "Konditsioner RU", "Konditsioner", null, null, null, 10, true)).id();
        technicianId = createTechnician("Alex", "+998902223344", true);
        secondTechnicianId = createTechnician("Botir", "+998903334455", true);
    }

    @Test
    void givenAcceptedAssignmentWhenStartingFromAssignedOrScheduledThenRepairBegins() throws Exception {
        Long assignedRequest = acceptedRequest(false);
        mockMvc.perform(post("/api/v1/requests/{requestId}/start", assignedRequest)
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.execution.startedAt").exists());

        Long scheduledRequest = acceptedRequest(true);
        mockMvc.perform(post("/api/v1/requests/{requestId}/start", scheduledRequest)
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void givenPendingInactiveOrStartedRepairWhenStartingThenStableConflictsReturn() {
        Long pendingRequest = createRequest("Pending assignment cannot start repair.");
        repairAssignmentService.assign(
                pendingRequest,
                new AssignmentRequest(technicianId, null),
                new AuthenticatedUser(admin));

        assertCode(
                runCatching(() -> repairExecutionService.start(pendingRequest, new AuthenticatedUser(admin))),
                "ACTIVE_ACCEPTED_ASSIGNMENT_REQUIRED");

        Long inactiveTechnician = createTechnician("Inactive", "+998904445566", true);
        Long inactiveRequest = acceptedRequestWithTechnician(inactiveTechnician, false);
        technicianService.changeActivation(inactiveTechnician, false, "away");
        assertCode(
                runCatching(() -> repairExecutionService.start(inactiveRequest, new AuthenticatedUser(admin))),
                "TECHNICIAN_INACTIVE");

        Long startedRequest = acceptedRequest(false);
        repairExecutionService.start(startedRequest, new AuthenticatedUser(admin));
        assertCode(
                runCatching(() -> repairExecutionService.start(startedRequest, new AuthenticatedUser(admin))),
                "REPAIR_NOT_STARTABLE");
    }

    @Test
    void givenInProgressRepairWhenDiagnosingWaitingResumingAndCompletingThenHistoryAndWorkloadUpdate()
            throws Exception {
        Long requestId = startedRequest();

        mockMvc.perform(patch("/api/v1/requests/{requestId}/diagnosis", requestId)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diagnosis":"EN relay failed. RU: Реле повреждено. UZ: Rele shikastlangan."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnosis").value("EN relay failed. RU: Реле повреждено. UZ: Rele shikastlangan."));

        mockMvc.perform(post("/api/v1/requests/{requestId}/wait-for-parts", requestId)
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Replacement relay is required."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PARTS"));

        mockMvc.perform(post("/api/v1/requests/{requestId}/resume", requestId)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"Part received."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        addCompletionPhoto(requestId);
        mockMvc.perform(post("/api/v1/requests/{requestId}/complete", requestId)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workPerformed":"Replaced relay and tested cooling.","completionNote":"Cooling restored."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.currentAssignment").doesNotExist())
                .andExpect(jsonPath("$.execution.completedAt").exists());

        assertThat(repairAssignmentRepository.findByRepairRequestIdOrderByCreatedAtDesc(requestId))
                .first()
                .extracting(assignment -> assignment.getStatus())
                .isEqualTo(AssignmentStatus.COMPLETED);
        assertThat(repairAssignmentService.workload(technicianId).totalActiveAssignments()).isZero();
        assertThat(statusHistoryRepository.findByRepairRequestIdOrderByChangedAtDescIdDesc(requestId))
                .extracting(history -> history.getToStatus())
                .containsSequence(
                        RepairRequestStatus.COMPLETED,
                        RepairRequestStatus.IN_PROGRESS,
                        RepairRequestStatus.WAITING_FOR_PARTS,
                        RepairRequestStatus.IN_PROGRESS);
    }

    @Test
    void givenInvalidWaitCompletionAndTerminalActionsThenStableErrorsReturn() {
        Long requestId = startedRequest();
        assertCode(
                runCatching(() -> repairExecutionService.waitForParts(
                        requestId,
                        new WaitForPartsRequest(" "),
                        new AuthenticatedUser(admin))),
                "INVALID_WAITING_REASON");
        assertCode(
                runCatching(() -> repairExecutionService.complete(
                        requestId,
                        new CompleteRepairRequest("Repaired wiring.", null),
                        new AuthenticatedUser(admin))),
                "DIAGNOSIS_REQUIRED");

        repairExecutionService.updateDiagnosis(
                requestId,
                new DiagnosisRequest("Relay failure."),
                new AuthenticatedUser(admin));
        assertCode(
                runCatching(() -> repairExecutionService.complete(
                        requestId,
                        new CompleteRepairRequest(" ", null),
                        new AuthenticatedUser(admin))),
                "WORK_PERFORMED_REQUIRED");
        addCompletionPhoto(requestId);
        repairExecutionService.complete(
                requestId,
                new CompleteRepairRequest("Replaced relay.", null),
                new AuthenticatedUser(admin));
        assertCode(
                runCatching(() -> repairExecutionService.complete(
                        requestId,
                        new CompleteRepairRequest("Replaced relay.", null),
                        new AuthenticatedUser(admin))),
                "REPAIR_ALREADY_COMPLETED");
        assertCode(
                runCatching(() -> repairExecutionService.cancel(
                        requestId,
                        new CancelRepairRequest("Customer changed mind."),
                        new AuthenticatedUser(admin))),
                "REPAIR_ALREADY_COMPLETED");
    }

    @Test
    void givenCancellationBeforeStartAfterStartOrWhileWaitingThenAssignmentClosesAndCapacityReleases() {
        Long beforeStart = acceptedRequest(false);
        repairExecutionService.cancel(beforeStart, new CancelRepairRequest("Customer cancelled."), new AuthenticatedUser(admin));
        assertCancelled(beforeStart);

        Long afterStart = startedRequest();
        repairExecutionService.cancel(afterStart, new CancelRepairRequest("Customer unavailable."), new AuthenticatedUser(admin));
        assertCancelled(afterStart);

        Long waiting = startedRequest();
        repairExecutionService.updateDiagnosis(waiting, new DiagnosisRequest("Motor issue."), new AuthenticatedUser(admin));
        repairExecutionService.waitForParts(waiting, new WaitForPartsRequest("Motor required."), new AuthenticatedUser(admin));
        repairExecutionService.cancel(waiting, new CancelRepairRequest("Customer declined parts."), new AuthenticatedUser(admin));
        assertCancelled(waiting);
    }

    @Test
    void givenSecurityCasesThenAdminManagerAllowedAnonymousDeniedAndTracePreserved() throws Exception {
        Long requestId = acceptedRequest(false);

        mockMvc.perform(post("/api/v1/requests/{requestId}/start", requestId)
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/requests/{requestId}/execution", requestId))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"));

        mockMvc.perform(post("/api/v1/requests/{requestId}/wait-for-parts", requestId)
                        .header("X-Trace-Id", "phase5-trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Part needed."}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Trace-Id", "phase5-trace"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.traceId").value("phase5-trace"));
    }

    @Test
    void givenConcurrentStartOperationsThenOnlyOneSucceeds() throws Exception {
        Long requestId = acceptedRequest(false);
        List<Object> results = runConcurrently(
                () -> repairExecutionService.start(requestId, new AuthenticatedUser(admin)),
                () -> repairExecutionService.start(requestId, new AuthenticatedUser(manager)));

        assertOneSuccess(results);
        assertThat(statusHistoryRepository.countByRepairRequestIdAndToStatus(requestId, RepairRequestStatus.IN_PROGRESS))
                .isEqualTo(1);
    }

    @Test
    void givenConcurrentLifecycleOperationsThenStateRemainsConsistent() throws Exception {
        Long startUnassign = acceptedRequest(false);
        List<Object> startAndUnassign = runConcurrently(
                () -> repairExecutionService.start(startUnassign, new AuthenticatedUser(admin)),
                () -> repairAssignmentService.unassign(
                        startUnassign,
                        new UnassignmentRequest("unassign"),
                        new AuthenticatedUser(manager)));
        assertOneSuccess(startAndUnassign);
        assertThat(activeAssignments(startUnassign)).isLessThanOrEqualTo(1);

        Long waitComplete = startedRequest();
        repairExecutionService.updateDiagnosis(waitComplete, new DiagnosisRequest("Relay failure."), new AuthenticatedUser(admin));
        addCompletionPhoto(waitComplete);
        List<Object> waitAndComplete = runConcurrently(
                () -> repairExecutionService.waitForParts(
                        waitComplete,
                        new WaitForPartsRequest("Relay needed."),
                        new AuthenticatedUser(admin)),
                () -> repairExecutionService.complete(
                        waitComplete,
                        new CompleteRepairRequest("Replaced relay.", null),
                        new AuthenticatedUser(manager)));
        assertOneSuccess(waitAndComplete);

        Long resumeCancel = startedRequest();
        repairExecutionService.updateDiagnosis(resumeCancel, new DiagnosisRequest("Motor failure."), new AuthenticatedUser(admin));
        repairExecutionService.waitForParts(resumeCancel, new WaitForPartsRequest("Motor needed."), new AuthenticatedUser(admin));
        List<Object> resumeAndCancel = runConcurrently(
                () -> repairExecutionService.resume(resumeCancel, new ResumeRepairRequest("Part received."), new AuthenticatedUser(admin)),
                () -> repairExecutionService.cancel(resumeCancel, new CancelRepairRequest("Customer cancelled."), new AuthenticatedUser(manager)));
        assertThat(resumeAndCancel).hasSize(2);
        assertThat(activeAssignments(resumeCancel)).isLessThanOrEqualTo(1);
    }

    @Test
    void givenConcurrentCompletionDiagnosisReassignmentOrCancellationThenNoActiveAssignmentLeaks()
            throws Exception {
        Long doubleComplete = startedRequest();
        repairExecutionService.updateDiagnosis(doubleComplete, new DiagnosisRequest("Relay failure."), new AuthenticatedUser(admin));
        addCompletionPhoto(doubleComplete);
        List<Object> completeTwice = runConcurrently(
                () -> repairExecutionService.complete(
                        doubleComplete,
                        new CompleteRepairRequest("Replaced relay.", null),
                        new AuthenticatedUser(admin)),
                () -> repairExecutionService.complete(
                        doubleComplete,
                        new CompleteRepairRequest("Replaced relay again.", null),
                        new AuthenticatedUser(manager)));
        assertOneSuccess(completeTwice);
        assertThat(statusHistoryRepository.countByRepairRequestIdAndToStatus(doubleComplete, RepairRequestStatus.COMPLETED))
                .isEqualTo(1);

        Long diagnosisComplete = startedRequest();
        repairExecutionService.updateDiagnosis(diagnosisComplete, new DiagnosisRequest("Old diagnosis."), new AuthenticatedUser(admin));
        addCompletionPhoto(diagnosisComplete);
        List<Object> diagnosisAndComplete = runConcurrently(
                () -> repairExecutionService.updateDiagnosis(
                        diagnosisComplete,
                        new DiagnosisRequest("Updated diagnosis."),
                        new AuthenticatedUser(admin)),
                () -> repairExecutionService.complete(
                        diagnosisComplete,
                        new CompleteRepairRequest("Completed work.", null),
                        new AuthenticatedUser(manager)));
        assertThat(diagnosisAndComplete).hasSize(2);

        Long completeReassign = startedRequest();
        repairExecutionService.updateDiagnosis(completeReassign, new DiagnosisRequest("Relay failure."), new AuthenticatedUser(admin));
        addCompletionPhoto(completeReassign);
        List<Object> completeAndReassign = runConcurrently(
                () -> repairExecutionService.complete(
                        completeReassign,
                        new CompleteRepairRequest("Replaced relay.", null),
                        new AuthenticatedUser(admin)),
                () -> repairAssignmentService.reassign(
                        completeReassign,
                        new ReassignmentRequest(secondTechnicianId, null, "change"),
                        new AuthenticatedUser(manager)));
        assertThat(completeAndReassign).hasSize(2);
        if (repairRequestService.get(completeReassign).status() == RepairRequestStatus.COMPLETED) {
            assertThat(activeAssignments(completeReassign)).isZero();
        }

        Long cancelAssign = createRequest("Cancellation and assignment should not leak active work.");
        List<Object> cancelAndAssign = runConcurrently(
                () -> repairExecutionService.cancel(
                        cancelAssign,
                        new CancelRepairRequest("cancelled"),
                        new AuthenticatedUser(admin)),
                () -> repairAssignmentService.assign(
                        cancelAssign,
                        new AssignmentRequest(technicianId, null),
                        new AuthenticatedUser(manager)));
        assertThat(cancelAndAssign).hasSize(2);
        if (repairRequestService.get(cancelAssign).status() == RepairRequestStatus.CANCELLED) {
            assertThat(activeAssignments(cancelAssign)).isZero();
        }
    }

    private Long startedRequest() {
        Long requestId = acceptedRequest(false);
        repairExecutionService.start(requestId, new AuthenticatedUser(admin));
        return requestId;
    }

    private Long acceptedRequest(boolean scheduled) {
        return acceptedRequestWithTechnician(technicianId, scheduled);
    }

    private Long acceptedRequestWithTechnician(Long technicianId, boolean scheduled) {
        Long requestId = createRequest("The appliance starts but does not complete the repair flow.");
        repairAssignmentService.assign(
                requestId,
                new AssignmentRequest(
                        technicianId,
                        scheduled ? OffsetDateTime.parse("2026-08-05T10:00:00+05:00") : null),
                new AuthenticatedUser(admin));
        repairAssignmentService.accept(requestId, new AuthenticatedUser(manager));
        return requestId;
    }

    private Long createRequest(String description) {
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

    private Long createTechnician(String fullName, String phone, boolean active) {
        return technicianService.create(new TechnicianCreateRequest(
                fullName,
                phone,
                "AC",
                null,
                5,
                LanguageCode.UZ,
                active)).id();
    }

    private void addCompletionPhoto(Long requestId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        RepairAttachment attachment = new RepairAttachment(
                repairRequestRepository.findById(requestId).orElseThrow(),
                AttachmentType.COMPLETION_PHOTO,
                "test/completion/" + requestId + "/" + UUID.randomUUID(),
                "yakuniy-rasm.jpg",
                admin,
                now);
        attachment.markAvailable(
                "image/jpeg",
                4,
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                now);
        repairAttachmentRepository.saveAndFlush(attachment);
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

    private void assertCancelled(Long requestId) {
        assertThat(repairRequestService.get(requestId).status()).isEqualTo(RepairRequestStatus.CANCELLED);
        assertThat(repairAssignmentRepository.findByRepairRequestIdOrderByCreatedAtDesc(requestId))
                .filteredOn(assignment -> assignment.getStatus() == AssignmentStatus.CANCELLED)
                .hasSize(1);
        assertThat(repairAssignmentService.workload(technicianId).totalActiveAssignments()).isZero();
    }

    private long activeAssignments(Long requestId) {
        return repairAssignmentRepository.findByRepairRequestIdOrderByCreatedAtDesc(requestId)
                .stream()
                .filter(assignment -> assignment.isActive())
                .count();
    }

    private void assertOneSuccess(List<Object> results) {
        assertThat(results).hasSize(2);
        assertThat(results).filteredOn(result -> !(result instanceof Exception)).hasSize(1);
    }

    private void assertCode(Object result, String code) {
        assertThat(result)
                .isInstanceOf(BusinessRuleException.class)
                .extracting(exception -> ((BusinessRuleException) exception).code())
                .isEqualTo(code);
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
}
