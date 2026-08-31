package com.example.darks.repair_auto.repair.attachment.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.catalog.category.api.dto.CategoryCreateRequest;
import com.example.darks.repair_auto.catalog.category.application.RepairCategoryService;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.MobileSessionService;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.domain.MobileSession;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRequest;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairExecutionRepository;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairRequestStatusHistoryRepository;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateResponse;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(com.example.darks.repair_auto.repair.attachment.infrastructure.storage.TestStorageConfiguration.class)
class MobileAttachmentStreamingIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private RepairCategoryService repairCategoryService;

    @Autowired
    private RepairRequestService repairRequestService;

    @Autowired
    private RepairAssignmentService repairAssignmentService;

    @Autowired
    private RepairAttachmentRepository attachmentRepository;

    @Autowired
    private RepairRequestStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private RepairExecutionRepository repairExecutionRepository;

    @Autowired
    private RepairAssignmentRepository repairAssignmentRepository;

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private RepairCategoryRepository repairCategoryRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    @Autowired
    private MobileSessionService mobileSessionService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    private User admin;

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

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        admin = userRepository.saveAndFlush(new User(
                "Admin",
                emailNormalizer.normalize("admin_mob_att@example.com"),
                passwordService.hash("AdminPass123!"),
                UserRole.ADMIN,
                true,
                now));
    }

    private String issueCustomerToken(Customer customer) {
        MobileSession session = mobileSessionService.createForCustomer(
                customer,
                MobileAuthProvider.PHONE,
                null,
                "127.0.0.1",
                "MobileAttachmentStreamingIntegrationTest");
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
                "MobileAttachmentStreamingIntegrationTest");
        return jwtTokenService.issueMobile(
                ActorType.TECHNICIAN,
                technician.getId(),
                technician.getAuthVersion(),
                session.getId(),
                PushClientType.TECHNICIAN_MOBILE,
                technician.getPhone());
    }

    @Test
    void mobileAttachment_uploadAndStreamDownloadByAuthorizedCustomerAndTechnician() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // Create category
        var category = repairCategoryService.create(new CategoryCreateRequest(
                "Body Shop", "Кузовной цех", "Kuzov sexi", null, null, null, true));

        // Create customer 1
        Customer customer1 = customerRepository.saveAndFlush(new Customer(
                "Stream Customer", "+998901237771", LanguageCode.UZ, now));

        // Create customer 2
        Customer customer2 = customerRepository.saveAndFlush(new Customer(
                "Other Customer", "+998901237772", LanguageCode.UZ, now));

        // Create technician
        Technician tech = technicianRepository.saveAndFlush(new Technician(
                "Assigned Tech", "+998901237773", "Welder", "Notes", 5, LanguageCode.UZ, true, now));

        // Create repair request for customer 1
        RepairRequestCreateResponse request = repairRequestService.create(
                new RepairRequestCreateRequest(
                        customer1.getId(),
                        category.id(),
                        "Broken bumper description",
                        "Tashkent",
                        null,
                        null,
                        RepairRequestPriority.NORMAL,
                        now.plusDays(2),
                        "Note"),
                new AuthenticatedUser(admin));

        // Assign request to technician
        repairAssignmentService.assign(request.id(), new AssignmentRequest(tech.getId(), null), new AuthenticatedUser(admin));

        String customer1Token = issueCustomerToken(customer1);
        String customer2Token = issueCustomerToken(customer2);
        String techToken = issueTechnicianToken(tech);

        // 1. Upload photo as Customer 1
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1};
        MockMultipartFile file = new MockMultipartFile("file", "damaged_bumper.jpg", "image/jpeg", jpegBytes);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/mobile/me/repair-requests/{id}/attachments", request.id())
                        .file(file)
                        .param("attachmentType", "CUSTOMER_PROBLEM_PHOTO")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.imagePreview").value(true))
                .andExpect(jsonPath("$.downloadUrl").exists())
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        long attachmentId = uploadJson.get("id").asLong();

        // 2. Customer 1 streams attachment
        mockMvc.perform(get("/api/v1/mobile/me/attachments/{id}/download", attachmentId)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"damaged_bumper.jpg\""))
                .andExpect(header().string("Cache-Control", "private, no-store"));

        // 3. Customer 2 (unauthorized) tries to stream attachment -> 404
        mockMvc.perform(get("/api/v1/mobile/me/attachments/{id}/download", attachmentId)
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isNotFound());

        // 4. Assigned Technician streams attachment -> 200
        mockMvc.perform(get("/api/v1/mobile/me/attachments/{id}/download", attachmentId)
                        .header("Authorization", "Bearer " + techToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"damaged_bumper.jpg\""));
    }
}