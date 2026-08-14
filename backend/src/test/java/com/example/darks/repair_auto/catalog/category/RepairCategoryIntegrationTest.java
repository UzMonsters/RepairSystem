package com.example.darks.repair_auto.catalog.category;

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
import com.example.darks.repair_auto.catalog.category.api.dto.CategoryCreateRequest;
import com.example.darks.repair_auto.catalog.category.application.CategoryNameNormalizer;
import com.example.darks.repair_auto.catalog.category.application.RepairCategoryService;
import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
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
class RepairCategoryIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepairCategoryService repairCategoryService;

    @Autowired
    private RepairCategoryRepository repairCategoryRepository;

    @Autowired
    private CategoryNameNormalizer categoryNameNormalizer;

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
        repairCategoryRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("Admin", "admin@example.com", "AdminPass123!", UserRole.ADMIN);
        manager = createUser("Manager", "manager@example.com", "ManagerPass123!", UserRole.MANAGER);
    }

    @Test
    void givenAdminWhenCreatingCategoryThenCategoryIsStoredWithNormalizedNames() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nameEn":"Air Conditioner","nameRu":"Konditsioner RU","nameUz":"Konditsioner","descriptionEn":"AC repair","descriptionRu":"Remont","descriptionUz":"Ta'mirlash","active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameEn").value("Air Conditioner"))
                .andExpect(jsonPath("$.nameUz").value("Konditsioner"))
                .andExpect(jsonPath("$.nameUzNormalized").doesNotExist());

        assertThat(categoryNameNormalizer.normalize("  KONDITSIONER  ")).isEqualTo("konditsioner");
    }

    @Test
    void givenManagerWhenReadingOrWritingCategoriesThenOnlyReadsAreAllowed() throws Exception {
        Long id = createCategory("Phone", "Telefon").id();

        mockMvc.perform(get("/api/v1/categories").with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/categories/{id}", id).with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/categories")
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nameEn":"New","nameRu":"New RU","nameUz":"New UZ"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nameEn":"Phone Updated","nameRu":"Telefon Updated","nameUz":"Telefon Yangilangan"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/categories/{id}/activation", id)
                        .with(user(new AuthenticatedUser(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenCategoriesWhenListingThenSearchActiveDefaultSortAndSortGuardsWork() throws Exception {
        createCategory("B", "B RU");
        createCategory("A", "A RU");

        mockMvc.perform(get("/api/v1/categories")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("search", "a")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nameUz").value("A"));

        mockMvc.perform(get("/api/v1/categories")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("search", "a ru")
                        .param("sort", "nameEn,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].nameEn").value("A"));

        mockMvc.perform(get("/api/v1/categories")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("sort", "nameUzNormalized,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));

        mockMvc.perform(get("/api/v1/categories")
                        .with(user(new AuthenticatedUser(admin)))
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"));
    }

    @Test
    void givenDuplicateOrInvalidCategoryRequestsThenStableErrorsReturn() throws Exception {
        createCategory("Phone", "Telefon");

        mockMvc.perform(post("/api/v1/categories")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nameEn":"Other EN","nameRu":"Other","nameUz":" phone "}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_UZ_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/v1/categories")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nameEn":"Other EN","nameRu":"telefon","nameUz":"Other UZ"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_RU_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/v1/categories")
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nameEn":"phone","nameRu":"Other RU","nameUz":"Other UZ"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_EN_ALREADY_EXISTS"));
    }

    @Test
    void givenCategoryWhenUpdatingArchivingReactivatingOrLookingUpMissingThenExpectedResponsesReturn()
            throws Exception {
        Long id = createCategory("Phone", "Telefon").id();

        mockMvc.perform(put("/api/v1/categories/{id}", id)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nameEn":"Computer","nameRu":"Kompyuter","nameUz":"Kompyuter UZ","descriptionEn":"PC EN","descriptionRu":"PC RU","descriptionUz":"PC"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameEn").value("Computer"))
                .andExpect(jsonPath("$.nameUz").value("Kompyuter UZ"));

        mockMvc.perform(patch("/api/v1/categories/{id}/activation", id)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false,"reason":"archive"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/v1/categories/{id}/activation", id)
                        .with(user(new AuthenticatedUser(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/v1/categories/999999").with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void givenAnonymousDeleteOrConcurrentDuplicateCreateThenExpectedProtectionApplies() throws Exception {
        Long id = createCategory("Phone", "Telefon").id();

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/categories/{id}", id).with(user(new AuthenticatedUser(admin))))
                .andExpect(status().isMethodNotAllowed());

        List<Object> results = runConcurrently(
                () -> repairCategoryService.create(
                        new CategoryCreateRequest("AC EN", "AC RU", "AC", null, null, null, true)),
                () -> repairCategoryService.create(
                        new CategoryCreateRequest("Other EN", "Other RU", " ac ", null, null, null, true)));

        assertThat(results).anyMatch(result -> result instanceof BusinessRuleException
                && ((BusinessRuleException) result).code().equals("CATEGORY_NAME_UZ_ALREADY_EXISTS"));
    }

    @Test
    void givenAcceptLanguageHeaderWhenGettingCategoriesThenLocalizedNameAndDescriptionAreReturnedWithFallback() throws Exception {
        RepairCategory category = repairCategoryRepository.saveAndFlush(new RepairCategory(
                "Fridge Repair EN",
                "Remont Holodilnikov RU",
                "Konditsioner Ta'mirlash UZ",
                "fridge repair en",
                "remont holodilnikov ru",
                "konditsioner tamirlash uz",
                "English Description",
                null,
                "Uzbek Description",
                true,
                OffsetDateTime.now(ZoneOffset.UTC)));

        mockMvc.perform(get("/api/v1/categories/{id}", category.getId())
                        .header("Accept-Language", "uz")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Konditsioner Ta'mirlash UZ"))
                .andExpect(jsonPath("$.description").value("Uzbek Description"));

        mockMvc.perform(get("/api/v1/categories/{id}", category.getId())
                        .header("Accept-Language", "ru")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Remont Holodilnikov RU"))
                .andExpect(jsonPath("$.description").value("Uzbek Description"));

        mockMvc.perform(get("/api/v1/categories/{id}", category.getId())
                        .header("Accept-Language", "en")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fridge Repair EN"))
                .andExpect(jsonPath("$.description").value("English Description"));

        mockMvc.perform(get("/api/v1/categories")
                        .header("Accept-Language", "uz")
                        .with(user(new AuthenticatedUser(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Konditsioner Ta'mirlash UZ"))
                .andExpect(jsonPath("$.content[0].description").value("Uzbek Description"));
    }

    private com.example.darks.repair_auto.catalog.category.api.dto.CategoryDetailResponse createCategory(
            String nameUz,
            String nameRu) {
        return repairCategoryService.create(
                new CategoryCreateRequest(nameUz, nameRu, nameUz, null, null, null, true));
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
