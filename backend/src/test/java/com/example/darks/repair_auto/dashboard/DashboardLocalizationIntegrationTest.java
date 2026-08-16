package com.example.darks.repair_auto.dashboard;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardLocalizationIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RepairCategoryRepository repairCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    private User admin;

    @BeforeEach
    void setUp() {
        repairRequestRepository.deleteAll();
        repairCategoryRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        admin = userRepository.saveAndFlush(new User(
                "Admin User",
                emailNormalizer.normalize("admin-dashloc@example.com"),
                passwordService.hash("Password123!"),
                UserRole.ADMIN,
                true,
                now));

        Customer customer = customerRepository.saveAndFlush(new Customer(
                "Dash Customer",
                "+998907776655",
                LanguageCode.UZ,
                now));

        RepairCategory category = repairCategoryRepository.saveAndFlush(new RepairCategory(
                "Refrigerator EN",
                "Holodilnik RU",
                "Muzlatgich UZ",
                "refrigerator en",
                "holodilnik ru",
                "muzlatgich uz",
                "Desc EN",
                "Desc RU",
                "Desc UZ",
                true,
                now));

        repairRequestRepository.saveAndFlush(new RepairRequest(
                "REQ-DASH-001",
                customer,
                category,
                "Description for repair request at least 10 chars",
                "Address text",
                null,
                null,
                RepairRequestPriority.NORMAL,
                null,
                null,
                admin,
                now));
    }

    @Test
    void givenAcceptLanguageHeaderWhenGettingRequestsByStatusThenLabelIsResolved() throws Exception {
        // Accept-Language: en
        mockMvc.perform(get("/api/v1/dashboard/requests-by-status")
                        .header("Accept-Language", "en")
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].label.label").value("New"))
                .andExpect(jsonPath("$.items[0].label.labelEn").value("New"))
                .andExpect(jsonPath("$.items[0].label.labelRu").value("Новая"))
                .andExpect(jsonPath("$.items[0].label.labelUz").value("Yangi"));

        // Accept-Language: ru
        mockMvc.perform(get("/api/v1/dashboard/requests-by-status")
                        .header("Accept-Language", "ru")
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].label.label").value("Новая"));

        // Accept-Language: uz
        mockMvc.perform(get("/api/v1/dashboard/requests-by-status")
                        .header("Accept-Language", "uz")
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].label.label").value("Yangi"));
    }

    @Test
    void givenAcceptLanguageHeaderWhenGettingRequestsByCategoryThenCategoryNameIsResolved() throws Exception {
        // Accept-Language: en
        mockMvc.perform(get("/api/v1/dashboard/requests-by-category")
                        .header("Accept-Language", "en")
                        .with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("Refrigerator EN"))
                .andExpect(jsonPath("$.items[0].nameEn").value("Refrigerator EN"))
                .andExpect(jsonPath("$.items[0].nameRu").value("Holodilnik RU"))
                .andExpect(jsonPath("$.items[0].nameUz").value("Muzlatgich UZ"));
    }
}
