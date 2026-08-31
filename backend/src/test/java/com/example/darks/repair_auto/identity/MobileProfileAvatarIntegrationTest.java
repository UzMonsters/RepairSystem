package com.example.darks.repair_auto.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
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
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
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
class MobileProfileAvatarIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private RepairAttachmentRepository attachmentRepository;

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MobileSessionService mobileSessionService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        attachmentRepository.deleteAll();
        repairRequestRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        customerRepository.deleteAll();
        technicianRepository.deleteAll();
    }

    private String issueCustomerToken(Customer customer) {
        MobileSession session = mobileSessionService.createForCustomer(
                customer,
                MobileAuthProvider.PHONE,
                null,
                "127.0.0.1",
                "MobileProfileAvatarIntegrationTest");
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
                "MobileProfileAvatarIntegrationTest");
        return jwtTokenService.issueMobile(
                ActorType.TECHNICIAN,
                technician.getId(),
                technician.getAuthVersion(),
                session.getId(),
                PushClientType.TECHNICIAN_MOBILE,
                technician.getPhone());
    }

    @Test
    void customerMobileActor_avatarLifecycle() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Customer customer = customerRepository.saveAndFlush(new Customer("Mobile Customer", "+998901235555", LanguageCode.UZ, now));
        String token = issueCustomerToken(customer);

        // 1. Check profile initially has no avatar
        mockMvc.perform(get("/api/v1/mobile/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").doesNotExist());

        // 2. Upload avatar
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1};
        MockMultipartFile file = new MockMultipartFile("file", "self_cust.jpg", "image/jpeg", jpegBytes);

        mockMvc.perform(multipart("/api/v1/mobile/me/avatar")
                        .file(file)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("self_cust.jpg"))
                .andExpect(jsonPath("$.downloadUrl").value("/api/v1/mobile/me/avatar"));

        // 3. Profile now includes avatar
        mockMvc.perform(get("/api/v1/mobile/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.fileName").value("self_cust.jpg"))
                .andExpect(jsonPath("$.avatar.downloadUrl").value("/api/v1/mobile/me/avatar"));

        // 4. Stream avatar
        mockMvc.perform(get("/api/v1/mobile/me/avatar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"self_cust.jpg\""))
                .andExpect(header().string("Cache-Control", "private, no-store"));

        // 5. Delete avatar
        mockMvc.perform(delete("/api/v1/mobile/me/avatar").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // 6. Profile no longer has avatar
        mockMvc.perform(get("/api/v1/mobile/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").doesNotExist());
    }

    @Test
    void technicianMobileActor_avatarLifecycle() throws Exception {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Technician tech = technicianRepository.saveAndFlush(new Technician(
                "Mobile Tech", "+998901236666", "Specialist", "Notes", 5, LanguageCode.UZ, true, now));
        String token = issueTechnicianToken(tech);

        // 1. Upload avatar
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1};
        MockMultipartFile file = new MockMultipartFile("file", "self_tech.jpg", "image/jpeg", jpegBytes);

        mockMvc.perform(multipart("/api/v1/mobile/me/avatar")
                        .file(file)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        })
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("self_tech.jpg"))
                .andExpect(jsonPath("$.downloadUrl").value("/api/v1/mobile/me/avatar"));

        // 2. Profile includes avatar
        mockMvc.perform(get("/api/v1/mobile/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar.fileName").value("self_tech.jpg"))
                .andExpect(jsonPath("$.avatar.downloadUrl").value("/api/v1/mobile/me/avatar"));

        // 3. Stream avatar
        mockMvc.perform(get("/api/v1/mobile/me/avatar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"self_tech.jpg\""));

        // 4. Delete avatar
        mockMvc.perform(delete("/api/v1/mobile/me/avatar").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // 5. Check profile after delete
        mockMvc.perform(get("/api/v1/mobile/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").doesNotExist());
    }
}