package com.example.darks.repair_auto.repair.request;

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
class RepairRequestLocalizationIntegrationTest extends PostgreSqlIntegrationTest {

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

    private User manager;
    private Customer customer;
    private RepairCategory category;
    private RepairRequest repairRequest;

    @BeforeEach
    void setUp() {
        repairRequestRepository.deleteAll();
        repairCategoryRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        manager = userRepository.saveAndFlush(new User(
                "Manager User",
                emailNormalizer.normalize("manager-loc@example.com"),
                passwordService.hash("Password123!"),
                UserRole.MANAGER,
                true,
                now));

        customer = customerRepository.saveAndFlush(new Customer(
                "Test Customer",
                "+998901234567",
                LanguageCode.UZ,
                now));

        category = repairCategoryRepository.saveAndFlush(new RepairCategory(
                "AC Repair EN",
                "Remont Konditsionerov RU",
                "Konditsioner Ta'mirlash UZ",
                "ac repair en",
                "remont konditsionerov ru",
                "konditsioner tamirlash uz",
                "Air conditioner repair description EN",
                "Opisanie remonta RU",
                "Tavsifi UZ",
                true,
                now));

        repairRequest = repairRequestRepository.saveAndFlush(new RepairRequest(
                "REQ-LOC-001",
                customer,
                category,
                "Description for repair request at least 10 chars",
                "Tashkent city center",
                null,
                null,
                RepairRequestPriority.NORMAL,
                null,
                null,
                manager,
                now));
    }

    @Test
    void givenAcceptLanguageHeaderWhenGettingRepairRequestsThenCategoryIsLocalized() throws Exception {
        // Accept-Language: en
        mockMvc.perform(get("/api/v1/repair-requests")
                        .header("Accept-Language", "en")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category.name").value("AC Repair EN"))
                .andExpect(jsonPath("$.content[0].category.description").value("Air conditioner repair description EN"));

        // Accept-Language: ru
        mockMvc.perform(get("/api/v1/repair-requests")
                        .header("Accept-Language", "ru")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category.name").value("Remont Konditsionerov RU"))
                .andExpect(jsonPath("$.content[0].category.description").value("Opisanie remonta RU"));

        // Accept-Language: uz
        mockMvc.perform(get("/api/v1/repair-requests")
                        .header("Accept-Language", "uz")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category.name").value("Konditsioner Ta'mirlash UZ"))
                .andExpect(jsonPath("$.content[0].category.description").value("Tavsifi UZ"));
    }

    @Test
    void givenAcceptLanguageHeaderWhenGettingRepairRequestDetailThenCategoryIsLocalized() throws Exception {
        mockMvc.perform(get("/api/v1/repair-requests/{id}", repairRequest.getId())
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.name").value("AC Repair EN"))
                .andExpect(jsonPath("$.category.nameEn").value("AC Repair EN"))
                .andExpect(jsonPath("$.category.nameRu").value("Remont Konditsionerov RU"))
                .andExpect(jsonPath("$.category.nameUz").value("Konditsioner Ta'mirlash UZ"));
    }

    @Test
    void givenAcceptLanguageHeaderWhenGettingCustomerHistoryThenCategoryIsLocalized() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}/repair-requests", customer.getId())
                        .header("Accept-Language", "en")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category.name").value("AC Repair EN"));
    }
}
