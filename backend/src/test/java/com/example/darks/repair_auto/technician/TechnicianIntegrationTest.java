package com.example.darks.repair_auto.technician;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.api.dto.TechnicianCreateRequest;
import com.example.darks.repair_auto.technician.application.TechnicianService;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
class TechnicianIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TechnicianService technicianService;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    private User admin;
    private User manager;

    @BeforeEach
    void setUp() {
        repairRequestRepository.deleteAll();
        technicianRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("Admin", "admin@example.com", "AdminPass123!", UserRole.ADMIN);
        manager = createUser("Manager", "manager@example.com", "ManagerPass123!", UserRole.MANAGER);
    }

    @Test
    void givenAdminOrManagerWhenCreatingTechnicianThenTechnicianIsStored() throws Exception {
        mockMvc.perform(post("/api/v1/technicians")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Alex Karimov","phone":"+998 90 222 33 44","specialization":"AC","notes":"City","maximumConcurrentRequests":5,"preferredLanguage":"EN","active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+998902223344"))
                .andExpect(jsonPath("$.preferredLanguage").value("EN"))
                .andExpect(jsonPath("$.telegramLinked").value(false))
                .andExpect(jsonPath("$.telegramUserId").doesNotExist());

        mockMvc.perform(post("/api/v1/technicians")
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Bexruz","phone":"90 333 44 55","specialization":"Phones"}
                """))
        .andExpect(status().isOk())
                .andExpect(jsonPath("$.maximumConcurrentRequests").value(5))
                .andExpect(jsonPath("$.preferredLanguage").value("UZ"));
    }

    @Test
    void givenTechniciansWhenListingThenFiltersSortingAndBoundsAreEnforced() throws Exception {
        createTechnician("Alex Karimov", "+998902223344", "Air conditioners");
        var second = createTechnician("Botir", "+998903334455", "Phones");
        technicianService.changeActivation(second.id(), false, "off");

        mockMvc.perform(get("/api/v1/technicians")
                        .with(user(new AuthenticatedUser(manager)))
                        .param("search", "air")
                        .param("specialization", "conditioners")
                        .param("active", "true")
                        .param("telegramLinked", "false")
                        .param("sort", "specialization,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Alex Karimov"));

        mockMvc.perform(get("/api/v1/technicians")
                        .with(user(new AuthenticatedUser(manager)))
                        .param("search", "90 222 33 44"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].phone").value("+998902223344"));

        mockMvc.perform(get("/api/v1/technicians")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("sort", "telegramUserId,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));

        mockMvc.perform(get("/api/v1/technicians")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));
    }

    @Test
    void givenTechnicianWhenUpdatingDeactivatingReactivatingOrLookingUpMissingThenExpectedResponsesReturn()
            throws Exception {
        Long id = createTechnician("Alex", "+998902223344", "AC").id();

        mockMvc.perform(put("/api/v1/technicians/{id}", id)
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Alex Updated","phone":"90 444 55 66","specialization":"Fridges","notes":"Updated","maximumConcurrentRequests":7,"preferredLanguage":"RU"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Alex Updated"))
                .andExpect(jsonPath("$.maximumConcurrentRequests").value(7))
                .andExpect(jsonPath("$.preferredLanguage").value("RU"));

        mockMvc.perform(patch("/api/v1/technicians/{id}/activation", id)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false,"reason":"away"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/v1/technicians/{id}/activation", id)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/v1/technicians/999999").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TECHNICIAN_NOT_FOUND"));
    }

    @Test
    void givenDuplicateOrInvalidTechnicianRequestsThenStableErrorsReturn() throws Exception {
        createTechnician("Alex", "+998902223344", "AC");

        mockMvc.perform(post("/api/v1/technicians")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Other","phone":"90 222 33 44","maximumConcurrentRequests":5}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TECHNICIAN_PHONE_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/v1/technicians")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Other","phone":"90 111 22 33","maximumConcurrentRequests":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MAXIMUM_CONCURRENT_REQUESTS"));
    }

    @Test
    void givenAnonymousOrDeleteWhenTechniciansEndpointRequestedThenDeniedOrMethodNotAllowed() throws Exception {
        Long id = createTechnician("Alex", "+998902223344", "AC").id();

        mockMvc.perform(get("/api/v1/technicians"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/technicians/{id}", id).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void givenConcurrentDuplicatePhoneCreatesThenOnlyOneSucceeds() throws Exception {
        List<Object> results = runConcurrently(
                () -> technicianService.create(new TechnicianCreateRequest(
                        "One",
                        "90 111 22 33",
                        "AC",
                        null,
                        5,
                        LanguageCode.UZ,
                        true)),
                () -> technicianService.create(new TechnicianCreateRequest(
                        "Two",
                        "+998901112233",
                        "Phone",
                        null,
                        5,
                        LanguageCode.RU,
                        true)));

        assertThat(results).anyMatch(result -> result instanceof BusinessRuleException
                && ((BusinessRuleException) result).code().equals("TECHNICIAN_PHONE_ALREADY_EXISTS"));
        assertThat(technicianRepository.count()).isEqualTo(1);
    }

    private com.example.darks.repair_auto.technician.api.dto.TechnicianDetailResponse createTechnician(
            String fullName,
            String phone,
            String specialization) {
        return technicianService.create(new TechnicianCreateRequest(
                fullName,
                phone,
                specialization,
                null,
                5,
                LanguageCode.UZ,
                true));
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

    private List<Object> runConcurrently(Callable<?> firstAction, Callable<?> secondAction) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(() -> runAfterStart(firstAction, start));
            var secondResult = executor.submit(() -> runAfterStart(secondAction, start));
            start.countDown();
            return List.of(firstResult.get(15, TimeUnit.SECONDS), secondResult.get(15, TimeUnit.SECONDS));
        }
    }

    private Object runAfterStart(Callable<?> action, CountDownLatch start) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        try {
            return action.call();
        } catch (BusinessRuleException exception) {
            return exception;
        }
    }
}
