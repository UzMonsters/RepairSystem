package com.example.darks.repair_auto.repair.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import com.example.darks.repair_auto.repair.attachment.api.dto.AttachmentDeleteRequest;
import com.example.darks.repair_auto.repair.attachment.api.dto.AttachmentResponse;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentService;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageUpload;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObject;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObjectDownload;
import com.example.darks.repair_auto.repair.execution.api.dto.CompleteRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.DiagnosisRequest;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@SpringBootTest(properties = {
        "app.storage.max-files-per-type=1",
        "app.storage.max-files-per-request=30"
})
@AutoConfigureMockMvc
class AttachmentIntegrationTest extends PostgreSqlIntegrationTest {

    private static final byte[] JPEG = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01};
    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01
    };
    private static final byte[] WEBP = new byte[] {
            0x52, 0x49, 0x46, 0x46, 0x04, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50, 0x01
    };
    private static final byte[] PDF = "%PDF-1.7\n".getBytes();
    private static final byte[] SVG = "<svg></svg>".getBytes();
    private static final byte[] HTML = "<html></html>".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private RepairAttachmentRepository attachmentRepository;

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

    @BeforeEach
    void setUp() {
        attachmentRepository.deleteAll();
        statusHistoryRepository.deleteAll();
        repairExecutionRepository.deleteAll();
        repairAssignmentRepository.deleteAll();
        repairRequestRepository.deleteAll();
        technicianRepository.deleteAll();
        customerRepository.deleteAll();
        repairCategoryRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("Admin", "admin@example.com", "AdminPass123!", UserRole.ADMIN);
        manager = createUser("Manager", "manager@example.com", "ManagerPass123!", UserRole.MANAGER);
        customerId = customerService.create(new CustomerCreateRequest("Vali Tester", "90 111 22 33", LanguageCode.UZ)).id();
        categoryId = repairCategoryService.create(new CategoryCreateRequest(
                "Washer", "Стиральная машина", "Kir yuvish mashinasi", null, null, null, true)).id();
        technicianId = technicianService.create(new TechnicianCreateRequest(
                "Technician",
                "+998902223344",
                "Appliance",
                null,
                5,
                LanguageCode.UZ,
                true)).id();
    }

    @Test
    void givenAdminOrManagerWhenUploadingListingDownloadingAndDeletingThenBusinessMetadataIsReturned()
            throws Exception {
        Long requestId = createRequest("Upload photo.");

        mockMvc.perform(multipart("/api/v1/requests/{requestId}/attachments", requestId)
                        .file(file("file", "muammo-расм.jpg", "image/jpeg", JPEG))
                        .param("type", "CUSTOMER_PROBLEM_PHOTO")
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CUSTOMER_PROBLEM_PHOTO"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"));

        Long attachmentId = attachmentRepository.findByRepairRequestIdAndStatusOrderByUploadedAtDesc(
                        requestId,
                        AttachmentStatus.AVAILABLE)
                .getFirst()
                .getId();

        mockMvc.perform(get("/api/v1/requests/{requestId}/attachments", requestId)
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(attachmentId))
                .andExpect(jsonPath("$[0].storageKey").doesNotExist());

        mockMvc.perform(get("/api/v1/attachments/{attachmentId}/download-url", attachmentId)
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://storage.test/1"))
                .andExpect(jsonPath("$.expiresAt").exists());

        mockMvc.perform(delete("/api/v1/attachments/{attachmentId}", attachmentId)
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""" 
                                {"reason":"Incorrect photo"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Set-Cookie"));

        mockMvc.perform(get("/api/v1/attachments/{attachmentId}", attachmentId)
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ATTACHMENT_NOT_FOUND"));
    }

    @Test
    void givenInvalidFilesWhenUploadingThenControlledValidationErrorsReturn() {
        Long requestId = createRequest("Validate files.");
        assertCode(
                runCatching(() -> upload(
                        requestId,
                        AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                        file("file", "empty.jpg", "image/jpeg"))),
                "ATTACHMENT_EMPTY");
        assertCode(runCatching(() -> upload(
                requestId,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                file("file", "bad.svg", "image/svg+xml", SVG))),
                "ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED");
        assertCode(runCatching(() -> upload(
                requestId,
                AttachmentType.GENERAL_DOCUMENT,
                file("file", "page.html", "text/html", HTML))),
                "ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED");
        assertCode(runCatching(() -> upload(
                requestId,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                file("file", "fake.png", "image/png", JPEG))),
                "ATTACHMENT_CONTENT_MISMATCH");
        assertCode(runCatching(() -> upload(
                requestId,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                file("file", "bad\nname.jpg", "image/jpeg", JPEG))),
                "VALIDATION_FAILED");
    }

    @Test
    void givenSupportedFormatsWhenUploadingThenChecksumAndUnicodeFilenameArePreserved() {
        Long jpegRequest = createRequest("JPEG.");
        AttachmentResponse jpeg = upload(jpegRequest, AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                file("file", "problem-rasm-экран.jpg", "image/jpeg", JPEG));
        assertThat(jpeg.originalFileName()).isEqualTo("problem-rasm-экран.jpg");
        assertThat(checksum(jpeg.id())).isEqualTo(sha256(JPEG));

        Long pngRequest = createRequest("PNG.");
        assertThat(upload(pngRequest, AttachmentType.GENERAL_DOCUMENT, file("file", "hujjat.png", "image/png", PNG))
                .contentType()).isEqualTo("image/png");

        Long webpRequest = createRequest("WebP.");
        assertThat(upload(webpRequest, AttachmentType.CUSTOMER_PROBLEM_PHOTO, file("file", "photo.webp", "image/webp", WEBP))
                .contentType()).isEqualTo("image/webp");

        Long pdfRequest = createRequest("PDF.");
        assertThat(upload(pdfRequest, AttachmentType.GENERAL_DOCUMENT, file("file", "акт.pdf", "application/pdf", PDF))
                .contentType()).isEqualTo("application/pdf");
    }

    @Test
    void givenLifecycleRulesThenCompletionPhotoIsRequiredAndTerminalAttachmentsAreImmutable() {
        Long requestId = createRequest("Lifecycle.");
        upload(requestId, AttachmentType.CUSTOMER_PROBLEM_PHOTO, file("file", "problem.jpg", "image/jpeg", JPEG));
        assertCode(runCatching(() -> upload(
                        requestId,
                        AttachmentType.DIAGNOSIS_PHOTO,
                        file("file", "diagnosis.jpg", "image/jpeg", JPEG))),
                "ATTACHMENT_TYPE_NOT_ALLOWED");

        start(requestId);
        upload(requestId, AttachmentType.DIAGNOSIS_PHOTO, file("file", "diagnosis.jpg", "image/jpeg", JPEG));
        repairExecutionService.updateDiagnosis(requestId, new DiagnosisRequest("Pump failure."), new AuthenticatedUser(admin));
        assertCode(
                runCatching(() -> repairExecutionService.complete(
                        requestId,
                        new CompleteRepairRequest("Replaced pump.", null),
                        new AuthenticatedUser(admin))),
                "COMPLETION_PHOTO_REQUIRED");

        AttachmentResponse completionPhoto = upload(
                requestId,
                AttachmentType.COMPLETION_PHOTO,
                file("file", "complete.jpg", "image/jpeg", JPEG));
        repairExecutionService.complete(
                requestId,
                new CompleteRepairRequest("Replaced pump.", null),
                new AuthenticatedUser(admin));
        assertThat(repairRequestService.get(requestId).status()).isEqualTo(RepairRequestStatus.COMPLETED);
        assertCode(
                runCatching(() -> {
                    attachmentService.delete(
                            completionPhoto.id(),
                            new AttachmentDeleteRequest("too late"),
                            new AuthenticatedUser(admin));
                    return null;
                }),
                "ATTACHMENT_DELETE_NOT_ALLOWED");
        assertCode(runCatching(() -> upload(
                        requestId,
                        AttachmentType.GENERAL_DOCUMENT,
                        file("file", "late.pdf", "application/pdf", PDF))),
                "ATTACHMENT_UPLOAD_NOT_ALLOWED");
    }

    @Test
    void givenWaitingRepairWhenUploadingDiagnosisAndCustomerPhotoThenAllowedButCompletionPhotoRejected() {
        Long requestId = createRequest("Waiting.");
        start(requestId);
        repairExecutionService.waitForParts(
                requestId,
                new WaitForPartsRequest("Need part."),
                new AuthenticatedUser(admin));

        upload(requestId, AttachmentType.DIAGNOSIS_PHOTO, file("file", "diag.jpg", "image/jpeg", JPEG));
        upload(requestId, AttachmentType.CUSTOMER_PROBLEM_PHOTO, file("file", "problem.jpg", "image/jpeg", JPEG));
        assertCode(runCatching(() -> upload(
                        requestId,
                        AttachmentType.COMPLETION_PHOTO,
                        file("file", "done.jpg", "image/jpeg", JPEG))),
                "ATTACHMENT_TYPE_NOT_ALLOWED");
    }

    @Test
    void givenConcurrentUploadsForFinalSlotThenExactlyOneSucceeds() throws Exception {
        Long requestId = createRequest("Final slot.");
        List<Object> results = runConcurrently(
                () -> upload(requestId, AttachmentType.CUSTOMER_PROBLEM_PHOTO, file("file", "first.jpg", "image/jpeg", JPEG)),
                () -> upload(requestId, AttachmentType.CUSTOMER_PROBLEM_PHOTO, file("file", "second.jpg", "image/jpeg", JPEG)));

        assertThat(results).filteredOn(result -> !(result instanceof Exception)).hasSize(1);
        assertThat(attachmentRepository.countByRepairRequestIdAndAttachmentTypeAndStatus(
                requestId,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                AttachmentStatus.AVAILABLE)).isEqualTo(1);
        assertThat(attachmentRepository.countByRepairRequestIdAndAttachmentTypeAndStatus(
                requestId,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                AttachmentStatus.UPLOADING)).isZero();
    }

    @Test
    void givenCompletionAndDeletionConcurrentThenEvidenceRuleIsNotBypassed() throws Exception {
        Long requestId = createRequest("Completion deletion race.");
        start(requestId);
        repairExecutionService.updateDiagnosis(requestId, new DiagnosisRequest("Pump failure."), new AuthenticatedUser(admin));
        AttachmentResponse completionPhoto = upload(
                requestId,
                AttachmentType.COMPLETION_PHOTO,
                file("file", "complete.jpg", "image/jpeg", JPEG));

        List<Object> results = runConcurrently(
                () -> repairExecutionService.complete(
                        requestId,
                        new CompleteRepairRequest("Replaced pump.", null),
                        new AuthenticatedUser(admin)),
                () -> {
                    attachmentService.delete(
                            completionPhoto.id(),
                            new AttachmentDeleteRequest("remove"),
                            new AuthenticatedUser(manager));
                    return null;
                });

        assertThat(results).hasSize(2);
        if (repairRequestService.get(requestId).status() == RepairRequestStatus.COMPLETED) {
            assertThat(attachmentRepository.findById(completionPhoto.id()).orElseThrow().getStatus())
                    .isEqualTo(AttachmentStatus.AVAILABLE);
        } else {
            assertThat(attachmentRepository.findById(completionPhoto.id()).orElseThrow().getStatus())
                    .isEqualTo(AttachmentStatus.DELETED);
        }
    }

    @Test
    void givenSecurityFailuresThenAnonymousDeniedTracePreservedAndNoCookie() throws Exception {
        Long requestId = createRequest("Security.");

        mockMvc.perform(multipart("/api/v1/requests/{requestId}/attachments", requestId)
                        .file(file("file", "problem.jpg", "image/jpeg", JPEG))
                        .param("type", "CUSTOMER_PROBLEM_PHOTO")
                        .header("X-Trace-Id", "phase6-trace"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Trace-Id", "phase6-trace"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.traceId").value("phase6-trace"));
    }

    private AttachmentResponse upload(Long requestId, AttachmentType type, MultipartFile file) {
        return attachmentService.upload(requestId, type, file, new AuthenticatedUser(admin));
    }

    private Long createRequest(String description) {
        String validDescription = description.length() >= 10 ? description : description + " attachment flow";
        return repairRequestService.create(new RepairRequestCreateRequest(
                        customerId,
                        categoryId,
                        validDescription,
                        "Tashkent",
                        null,
                        null,
                        RepairRequestPriority.NORMAL,
                        OffsetDateTime.now(ZoneOffset.UTC).plusDays(2),
                        "Initial note"),
                new AuthenticatedUser(admin)).id();
    }

    private void start(Long requestId) {
        repairAssignmentService.assign(requestId, new AssignmentRequest(technicianId, null), new AuthenticatedUser(admin));
        repairAssignmentService.accept(requestId, new AuthenticatedUser(manager));
        repairExecutionService.start(requestId, new AuthenticatedUser(admin));
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

    private MockMultipartFile file(String partName, String originalName, String contentType, byte... content) {
        return new MockMultipartFile(partName, originalName, contentType, content);
    }

    private String checksum(Long attachmentId) {
        return attachmentRepository.findById(attachmentId).orElseThrow().getSha256Checksum();
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
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
            Object firstResult = first.get(15, TimeUnit.SECONDS);
            Object secondResult = second.get(15, TimeUnit.SECONDS);
            return List.of(
                    firstResult == null ? "SUCCESS" : firstResult,
                    secondResult == null ? "SUCCESS" : secondResult);
        }
    }

    private Object runAfterStart(Callable<?> action, CountDownLatch start) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        return runCatching(action);
    }

    @TestConfiguration
    static class StorageTestConfiguration {

        @Bean
        @Primary
        ObjectStorageService objectStorageService() {
            return new FakeObjectStorageService();
        }
    }

    static class FakeObjectStorageService implements ObjectStorageService {

        private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

        @Override
        public StoredObject upload(StorageUpload command) {
            try {
                objects.put(command.storageKey(), command.inputStream().readAllBytes());
                return new StoredObject(command.storageKey(), command.contentType(), command.sizeBytes());
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public StoredObjectDownload download(String storageKey) {
            byte[] bytes = objects.get(storageKey);
            if (bytes == null) {
                throw new IllegalStateException("Object not found.");
            }
            return new StoredObjectDownload("image/jpeg", bytes.length, new ByteArrayInputStream(bytes));
        }

        @Override
        public URI createDownloadUrl(String storageKey, String downloadFileName, Duration ttl) {
            Long id = objects.keySet()
                    .stream()
                    .filter(key -> key.equals(storageKey))
                    .findFirst()
                    .map(key -> 1L)
                    .orElse(1L);
            return URI.create("https://storage.test/" + id);
        }

        @Override
        public void delete(String storageKey) {
            objects.remove(storageKey);
        }

        @Override
        public boolean exists(String storageKey) {
            return objects.containsKey(storageKey);
        }
    }
}
