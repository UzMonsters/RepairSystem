package com.example.darks.repair_auto.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.application.RefreshSessionService;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.api.dto.UserCreateRequest;
import com.example.darks.repair_auto.identity.api.dto.UserUpdateRequest;
import com.example.darks.repair_auto.identity.application.UserManagementService;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
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
import org.springframework.data.domain.PageRequest;

@SpringBootTest
class UserManagementIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private UserManagementService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private RefreshSessionService refreshSessionService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    private User admin;

    @BeforeEach
    void setUp() {
        repairRequestRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("Admin User", "admin@example.com", "AdminPass123!", UserRole.ADMIN, true);
    }

    @Test
    void givenAdminWhenCreatingManagerThenPasswordIsHashedAndEmailNormalized() {
        var response = service.create(new UserCreateRequest(
                "Operations Manager",
                "Manager@Example.COM",
                "ManagerPass123!",
                UserRole.MANAGER,
                true));

        User saved = userRepository.findById(response.id()).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("manager@example.com");
        assertThat(saved.getPasswordHash()).doesNotContain("ManagerPass123!");
        assertThat(passwordService.matches("ManagerPass123!", saved.getPasswordHash())).isTrue();
    }

    @Test
    void givenDuplicateNormalizedEmailWhenCreatingThenStableErrorIsReturned() {
        service.create(new UserCreateRequest("One", "one@example.com", "ManagerPass123!", UserRole.MANAGER, true));

        BusinessRuleException exception = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> service.create(new UserCreateRequest("Two", "ONE@example.com", "ManagerPass123!", UserRole.MANAGER, true)),
                BusinessRuleException.class);

        assertThat(exception.code()).isEqualTo("USER_EMAIL_ALREADY_EXISTS");
    }

    @Test
    void givenUsersWhenListingThenSearchRoleAndActiveFiltersWork() {
        service.create(new UserCreateRequest("Alpha Manager", "alpha@example.com", "ManagerPass123!", UserRole.MANAGER, true));
        service.create(new UserCreateRequest("Beta Manager", "beta@example.com", "ManagerPass123!", UserRole.MANAGER, false));

        var page = service.list("alpha", UserRole.MANAGER, true, PageRequest.of(0, 20));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).email()).isEqualTo("alpha@example.com");
    }

    @Test
    void givenUserWhenUpdatingProfileThenRoleAndActiveAreUnchanged() {
        User manager = createUser("Manager", "manager@example.com", "ManagerPass123!", UserRole.MANAGER, true);

        service.update(manager.getId(), new UserUpdateRequest("Updated Manager", "updated@example.com"));

        User updated = userRepository.findById(manager.getId()).orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("Updated Manager");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getRole()).isEqualTo(UserRole.MANAGER);
        assertThat(updated.isActive()).isTrue();
    }

    @Test
    void givenRoleChangeOrDisableWhenUserHasSessionsThenSessionsAreRevoked() {
        User manager = createUser("Manager", "manager@example.com", "ManagerPass123!", UserRole.MANAGER, true);
        refreshSessionService.create(manager, null, null);

        service.changeRole(manager.getId(), UserRole.ADMIN);

        assertThat(refreshSessionRepository.findByUserId(manager.getId())).allMatch(session -> session.isRevoked());
    }

    @Test
    void givenLastActiveAdminWhenDemoteOrDisableThenOperationFails() {
        BusinessRuleException demote = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> service.changeRole(admin.getId(), UserRole.MANAGER),
                BusinessRuleException.class);
        BusinessRuleException disable = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> service.changeActivation(admin.getId(), false, 999L),
                BusinessRuleException.class);

        assertThat(demote.code()).isEqualTo("LAST_ACTIVE_ADMIN_REQUIRED");
        assertThat(disable.code()).isEqualTo("LAST_ACTIVE_ADMIN_REQUIRED");
    }

    @Test
    void givenAdminWhenSelfDisableThenOperationFails() {
        User secondAdmin = createUser("Second Admin", "second@example.com", "AdminPass123!", UserRole.ADMIN, true);

        BusinessRuleException exception = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> service.changeActivation(secondAdmin.getId(), false, secondAdmin.getId()),
                BusinessRuleException.class);

        assertThat(exception.code()).isEqualTo("SELF_DISABLE_NOT_ALLOWED");
    }

    @Test
    void givenTwoAdminsWhenTheyConcurrentlyDisableEachOtherThenAtLeastOneActiveAdminRemains() throws Exception {
        User secondAdmin = createUser("Second Admin", "second@example.com", "AdminPass123!", UserRole.ADMIN, true);

        List<Object> results = runConcurrently(
                () -> service.changeActivation(admin.getId(), false, secondAdmin.getId()),
                () -> service.changeActivation(secondAdmin.getId(), false, admin.getId()));

        assertThat(results).anyMatch(result -> result instanceof BusinessRuleException
                && ((BusinessRuleException) result).code().equals("LAST_ACTIVE_ADMIN_REQUIRED"));
        assertThat(userRepository.countActiveAdmins()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void givenTwoAdminsWhenTheyConcurrentlyDemoteEachOtherThenAtLeastOneActiveAdminRemains() throws Exception {
        User secondAdmin = createUser("Second Admin", "second2@example.com", "AdminPass123!", UserRole.ADMIN, true);

        List<Object> results = runConcurrently(
                () -> service.changeRole(admin.getId(), UserRole.MANAGER),
                () -> service.changeRole(secondAdmin.getId(), UserRole.MANAGER));

        assertThat(results).anyMatch(result -> result instanceof BusinessRuleException
                && ((BusinessRuleException) result).code().equals("LAST_ACTIVE_ADMIN_REQUIRED"));
        assertThat(userRepository.countActiveAdmins()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void givenTwoAdminsWhenDisableAndDemotionRunConcurrentlyThenAtLeastOneActiveAdminRemains() throws Exception {
        User secondAdmin = createUser("Second Admin", "second3@example.com", "AdminPass123!", UserRole.ADMIN, true);

        List<Object> results = runConcurrently(
                () -> service.changeActivation(admin.getId(), false, secondAdmin.getId()),
                () -> service.changeRole(secondAdmin.getId(), UserRole.MANAGER));

        assertThat(results).anyMatch(result -> result instanceof BusinessRuleException
                && ((BusinessRuleException) result).code().equals("LAST_ACTIVE_ADMIN_REQUIRED"));
        assertThat(userRepository.countActiveAdmins()).isGreaterThanOrEqualTo(1);
    }

    private User createUser(String fullName, String email, String password, UserRole role, boolean active) {
        User user = new User(
                fullName,
                emailNormalizer.normalize(email),
                passwordService.hash(password),
                role,
                active,
                OffsetDateTime.now(ZoneOffset.UTC));
        return userRepository.saveAndFlush(user);
    }

    private List<Object> runConcurrently(Callable<?> firstAction, Callable<?> secondAction) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Callable<Object> first = () -> runAfterStart(firstAction, start);
        Callable<Object> second = () -> runAfterStart(secondAction, start);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(first);
            var secondResult = executor.submit(second);
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
