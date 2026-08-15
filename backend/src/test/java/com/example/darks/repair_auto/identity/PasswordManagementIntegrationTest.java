package com.example.darks.repair_auto.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.identity.api.dto.ResetPasswordRequest;
import com.example.darks.repair_auto.identity.application.AuthenticationService;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.application.UserManagementService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PasswordManagementIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    private User adminUser;
    private User managerUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        adminUser = userRepository.saveAndFlush(new User(
                "Admin User",
                "admin_pwd@example.com",
                passwordService.hash("AdminPass123!"),
                UserRole.ADMIN,
                true,
                now
        ));
        managerUser = userRepository.saveAndFlush(new User(
                "Manager User",
                "manager_pwd@example.com",
                passwordService.hash("ManagerPass123!"),
                UserRole.MANAGER,
                true,
                now
        ));
    }

    @Test
    void shouldAllowUserToChangeOwnPasswordWithValidCredentials() {
        authenticationService.changePassword(
                managerUser.getId(),
                "ManagerPass123!",
                "NewManagerPass456!",
                "NewManagerPass456!"
        );

        User updated = userRepository.findById(managerUser.getId()).orElseThrow();
        assertThat(passwordService.matches("NewManagerPass456!", updated.getPasswordHash())).isTrue();
        assertThat(passwordService.matches("ManagerPass123!", updated.getPasswordHash())).isFalse();
    }

    @Test
    void shouldRejectOwnPasswordChangeWithIncorrectOldPassword() {
        assertThatThrownBy(() -> authenticationService.changePassword(
                managerUser.getId(),
                "WrongOldPass123!",
                "NewManagerPass456!",
                "NewManagerPass456!"
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Current password is invalid");
    }

    @Test
    void shouldRejectOwnPasswordChangeWhenConfirmationMismatch() {
        assertThatThrownBy(() -> authenticationService.changePassword(
                managerUser.getId(),
                "ManagerPass123!",
                "NewManagerPass456!",
                "MismatchPass456!"
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    void shouldRejectOwnPasswordChangeWhenSameAsCurrent() {
        assertThatThrownBy(() -> authenticationService.changePassword(
                managerUser.getId(),
                "ManagerPass123!",
                "ManagerPass123!",
                "ManagerPass123!"
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must differ");
    }

    @Test
    void shouldAllowAdminToResetUserPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest("TempAdminResetPass1!", "TempAdminResetPass1!");
        userManagementService.resetPassword(managerUser.getId(), request, adminUser.getId());

        User updated = userRepository.findById(managerUser.getId()).orElseThrow();
        assertThat(passwordService.matches("TempAdminResetPass1!", updated.getPasswordHash())).isTrue();
    }

    @Test
    void shouldRejectAdminResetWhenConfirmationMismatch() {
        ResetPasswordRequest request = new ResetPasswordRequest("TempAdminResetPass1!", "MismatchPass1!");
        assertThatThrownBy(() -> userManagementService.resetPassword(managerUser.getId(), request, adminUser.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    void shouldRejectAdminResetForNonExistentUser() {
        ResetPasswordRequest request = new ResetPasswordRequest("TempAdminResetPass1!", "TempAdminResetPass1!");
        assertThatThrownBy(() -> userManagementService.resetPassword(999999L, request, adminUser.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("User was not found");
    }
}
