package com.example.darks.repair_auto.technician;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.api.dto.TechnicianCreateRequest;
import com.example.darks.repair_auto.technician.api.dto.TechnicianDetailResponse;
import com.example.darks.repair_auto.technician.application.TechnicianService;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
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
class TechnicianAvatarIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TechnicianService technicianService;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository attachmentRepository;

    @Autowired
    private com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository repairAssignmentRepository;

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
        repairAssignmentRepository.deleteAll();
        repairRequestRepository.deleteAll();
        technicianRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = userRepository.saveAndFlush(new User(
                "Admin",
                emailNormalizer.normalize("admin_tech_avatar@example.com"),
                passwordService.hash("AdminPass123!"),
                UserRole.ADMIN,
                true,
                OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void technicianAvatar_lifecycle_uploadStreamDelete() throws Exception {
        TechnicianDetailResponse tech = technicianService.create(
                new TechnicianCreateRequest("Tech Avatar", "+998901238888", "Master", "Notes", 5, LanguageCode.UZ, true));

        Long techId = tech.id();

        // 1. Initial details has null avatar
        mockMvc.perform(get("/api/v1/technicians/{id}", techId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").doesNotExist());

        // 2. Upload avatar
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1};
        MockMultipartFile file = new MockMultipartFile("file", "tech.jpg", "image/jpeg", jpegBytes);

        mockMvc.perform(multipart("/api/v1/technicians/{id}/avatar", techId)
                        .file(file)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        })
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("tech.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.downloadUrl").value("/api/v1/technicians/" + techId + "/avatar"));

        // 3. Detail includes avatar
        mockMvc.perform(get("/api/v1/technicians/{id}", techId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.fileName").value("tech.jpg"))
                .andExpect(jsonPath("$.avatar.downloadUrl").value("/api/v1/technicians/" + techId + "/avatar"));

        // 4. Stream avatar
        mockMvc.perform(get("/api/v1/technicians/{id}/avatar", techId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"tech.jpg\""))
                .andExpect(header().string("Cache-Control", "private, no-store"));

        // 5. Delete avatar
        mockMvc.perform(delete("/api/v1/technicians/{id}/avatar", techId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isNoContent());

        // 6. Verify removed
        mockMvc.perform(get("/api/v1/technicians/{id}", techId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").doesNotExist());
    }
}