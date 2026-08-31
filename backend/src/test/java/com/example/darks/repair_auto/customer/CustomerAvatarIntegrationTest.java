package com.example.darks.repair_auto.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.customer.api.dto.CustomerCreateRequest;
import com.example.darks.repair_auto.customer.api.dto.CustomerDetailResponse;
import com.example.darks.repair_auto.customer.application.CustomerService;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(com.example.darks.repair_auto.repair.attachment.infrastructure.storage.TestStorageConfiguration.class)
class CustomerAvatarIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RepairAttachmentRepository attachmentRepository;

    @Autowired
    private com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository repairRequestRepository;

    @Autowired
    private com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    private User admin;

    @BeforeEach
    void setUp() {
        attachmentRepository.deleteAll();
        repairRequestRepository.deleteAll();
        customerRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = userRepository.saveAndFlush(new User(
                "Admin",
                emailNormalizer.normalize("admin_cust_avatar@example.com"),
                passwordService.hash("AdminPass123!"),
                UserRole.ADMIN,
                true,
                OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void customerAvatar_lifecycle_uploadStreamDelete() throws Exception {
        CustomerDetailResponse customer = customerService.create(
                new CustomerCreateRequest("Avatar Customer", "+998901239999", LanguageCode.UZ));

        Long customerId = customer.id();

        // 1. Initial details has null avatar
        mockMvc.perform(get("/api/v1/customers/{id}", customerId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").doesNotExist());

        // 2. Upload avatar
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1};
        MockMultipartFile file = new MockMultipartFile("file", "avatar1.jpg", "image/jpeg", jpegBytes);

        mockMvc.perform(multipart("/api/v1/customers/{id}/avatar", customerId)
                        .file(file)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        })
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("avatar1.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.downloadUrl").value("/api/v1/customers/" + customerId + "/avatar"));

        // 3. Customer detail returns avatar object
        mockMvc.perform(get("/api/v1/customers/{id}", customerId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.fileName").value("avatar1.jpg"))
                .andExpect(jsonPath("$.avatar.downloadUrl").value("/api/v1/customers/" + customerId + "/avatar"));

        // 4. Stream avatar
        mockMvc.perform(get("/api/v1/customers/{id}/avatar", customerId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"avatar1.jpg\""))
                .andExpect(header().string("Cache-Control", "private, no-store"));

        // 5. Replace avatar with new one
        MockMultipartFile file2 = new MockMultipartFile("file", "avatar2.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'});
        mockMvc.perform(multipart("/api/v1/customers/{id}/avatar", customerId)
                        .file(file2)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        })
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("avatar2.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"));

        // 6. Delete avatar
        mockMvc.perform(delete("/api/v1/customers/{id}/avatar", customerId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isNoContent());

        // 7. Verify avatar is removed from detail
        mockMvc.perform(get("/api/v1/customers/{id}", customerId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").doesNotExist());

        // 8. Stream avatar returns 404
        mockMvc.perform(get("/api/v1/customers/{id}/avatar", customerId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isNotFound());
    }
}