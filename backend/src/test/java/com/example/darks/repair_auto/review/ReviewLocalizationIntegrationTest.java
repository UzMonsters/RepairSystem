package com.example.darks.repair_auto.review;

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
import com.example.darks.repair_auto.review.domain.RepairReview;
import com.example.darks.repair_auto.review.domain.ReviewSource;
import com.example.darks.repair_auto.review.infrastructure.RepairReviewRepository;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewLocalizationIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepairReviewRepository reviewRepository;

    @Autowired
    private RepairRequestRepository requestRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private RepairCategoryRepository categoryRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    private User manager;
    private Customer customer;
    private RepairCategory category;
    private RepairRequest request;
    private RepairReview review;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        requestRepository.deleteAll();
        categoryRepository.deleteAll();
        customerRepository.deleteAll();
        technicianRepository.deleteAll();
        userRepository.deleteAll();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        manager = userRepository.saveAndFlush(new User(
                "Manager User",
                emailNormalizer.normalize("manager-revloc@example.com"),
                passwordService.hash("Password123!"),
                UserRole.MANAGER,
                true,
                now));

        customer = customerRepository.saveAndFlush(new Customer(
                "Reviewer Customer",
                "+998901112233",
                LanguageCode.UZ,
                now));

        category = categoryRepository.saveAndFlush(new RepairCategory(
                "Washing Machine EN",
                "Stiralnaya Mashina RU",
                "Kir Yuvish Mashinasi UZ",
                "wm en",
                "sm ru",
                "kym uz",
                "WM description EN",
                "SM description RU",
                "KYM description UZ",
                true,
                now));

        request = new RepairRequest(
                "REQ-REV-001",
                customer,
                category,
                "Description for repair request at least 10 chars",
                "Address text",
                null,
                null,
                RepairRequestPriority.NORMAL,
                null,
                null,
                manager,
                now);
        request.markCompleted(now);
        request = requestRepository.saveAndFlush(request);

        Technician technician = technicianRepository.saveAndFlush(new Technician(
                "Tech User",
                "+998909998877",
                "WM",
                "Notes",
                5,
                LanguageCode.UZ,
                true,
                now));

        review = reviewRepository.saveAndFlush(new RepairReview(
                request,
                customer,
                technician,
                5,
                "Great service!",
                ReviewSource.TELEGRAM,
                LanguageCode.EN,
                now));
    }

    @Test
    void givenAcceptLanguageHeaderWhenGettingReviewsThenCategoryNameIsResolved() throws Exception {
        // Accept-Language: en
        mockMvc.perform(get("/api/v1/reviews/{id}", review.getId())
                        .header("Accept-Language", "en")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.name").value("Washing Machine EN"))
                .andExpect(jsonPath("$.category.nameEn").value("Washing Machine EN"))
                .andExpect(jsonPath("$.category.nameRu").value("Stiralnaya Mashina RU"))
                .andExpect(jsonPath("$.category.nameUz").value("Kir Yuvish Mashinasi UZ"));

        // Accept-Language: ru
        mockMvc.perform(get("/api/v1/reviews/{id}", review.getId())
                        .header("Accept-Language", "ru")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.name").value("Stiralnaya Mashina RU"));

        // Accept-Language: uz
        mockMvc.perform(get("/api/v1/reviews/{id}", review.getId())
                        .header("Accept-Language", "uz")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.name").value("Kir Yuvish Mashinasi UZ"));
    }
}
