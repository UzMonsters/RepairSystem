package com.example.darks.repair_auto.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.auth.domain.RefreshSessionRepository;
import com.example.darks.repair_auto.auth.service.EmailNormalizer;
import com.example.darks.repair_auto.auth.service.PasswordService;
import com.example.darks.repair_auto.auth.service.RefreshSessionService;
import com.example.darks.repair_auto.common.error.BusinessRuleException;
import com.example.darks.repair_auto.user.domain.User;
import com.example.darks.repair_auto.user.domain.UserRepository;
import com.example.darks.repair_auto.user.domain.UserRole;
import com.example.darks.repair_auto.user.dto.UserCreateRequest;
import com.example.darks.repair_auto.user.dto.UserUpdateRequest;
import com.example.darks.repair_auto.user.service.UserManagementService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    private RefreshSessionService refreshSessionService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EmailNormalizer emailNormalizer;

    private User admin;

    @BeforeEach
    void setUp() {
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
}
