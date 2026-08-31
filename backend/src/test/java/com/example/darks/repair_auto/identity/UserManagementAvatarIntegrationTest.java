package com.example.darks.repair_auto.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.identity.api.dto.UserCreateRequest;
import com.example.darks.repair_auto.identity.api.dto.UserDetailsResponse;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.application.UserManagementService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.profile.application.ProfileService;
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
class UserManagementAvatarIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository attachmentRepository;

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
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = userRepository.saveAndFlush(new User(
                "Admin",
                emailNormalizer.normalize("admin_user_mgmt_avatar@example.com"),
                passwordService.hash("AdminPass123!"),
                UserRole.ADMIN,
                true,
                OffsetDateTime.now(ZoneOffset.UTC)));
    }

    @Test
    void userAvatar_streamAndMappingInUserManagement() throws Exception {
        UserDetailsResponse created = userManagementService.create(
                new UserCreateRequest("Staff User", "staff_avatar@example.com", "Secret123!", UserRole.MANAGER, true));

        Long staffId = created.id();

        // 1. Initial details has no avatar
        mockMvc.perform(get("/api/v1/users/{id}", staffId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").doesNotExist());

        // 2. Upload avatar via profile self endpoint for the created user
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1};
        MockMultipartFile file = new MockMultipartFile("file", "staff.jpg", "image/jpeg", jpegBytes);

        profileService.uploadAvatar(staffId, file);

        // 3. User details now includes avatar with staff URL
        mockMvc.perform(get("/api/v1/users/{id}", staffId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.fileName").value("staff.jpg"))
                .andExpect(jsonPath("$.avatar.downloadUrl").value("/api/v1/users/" + staffId + "/avatar"));

        // 4. Stream avatar via admin endpoint /api/v1/users/{id}/avatar
        mockMvc.perform(get("/api/v1/users/{id}/avatar", staffId).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"staff.jpg\""))
                .andExpect(header().string("Cache-Control", "private, no-store"));
    }
}