package com.example.darks.repair_auto.identity.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.darks.repair_auto.identity.domain.MobileRefreshRevocationReason;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.notification.push.application.PushEndpointService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActorAccessLifecycleServiceTest {

    private RefreshSessionService staffRefreshSessionService;
    private MobileRefreshSessionService mobileRefreshSessionService;
    private PushEndpointService pushEndpointService;
    private UserRepository userRepository;
    private Clock clock;
    private ActorAccessLifecycleService service;

    @BeforeEach
    void setUp() {
        staffRefreshSessionService = mock(RefreshSessionService.class);
        mobileRefreshSessionService = mock(MobileRefreshSessionService.class);
        pushEndpointService = mock(PushEndpointService.class);
        userRepository = mock(UserRepository.class);
        clock = Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);

        service = new ActorAccessLifecycleService(
                staffRefreshSessionService,
                mobileRefreshSessionService,
                pushEndpointService,
                userRepository,
                clock);
    }

    @Test
    void givenStaffLogoutAll_thenRevokesSessionsIncrementsVersionAndDisablesPushEndpoints() {
        service.onStaffLogoutAll(10L);

        verify(staffRefreshSessionService).revokeAllForUser(10L, "LOGOUT_ALL");
        verify(userRepository).incrementAuthVersion(eq(10L), any());
        verify(pushEndpointService).disableAllForStaff(10L);
    }

    @Test
    void givenStaffPasswordChanged_thenRevokesSessionsIncrementsVersionAndDisablesPushEndpoints() {
        service.onStaffPasswordChanged(10L, "PASSWORD_CHANGED");

        verify(staffRefreshSessionService).revokeAllForUser(10L, "PASSWORD_CHANGED");
        verify(userRepository).incrementAuthVersion(eq(10L), any());
        verify(pushEndpointService).disableAllForStaff(10L);
    }

    @Test
    void givenStaffDeactivated_thenRevokesSessionsIncrementsVersionAndDisablesPushEndpoints() {
        service.onStaffDeactivated(10L);

        verify(staffRefreshSessionService).revokeAllForUser(10L, "USER_DISABLED");
        verify(userRepository).incrementAuthVersion(eq(10L), any());
        verify(pushEndpointService).disableAllForStaff(10L);
    }

    @Test
    void givenStaffRoleChanged_thenRevokesSessionsIncrementsVersionAndDisablesPushEndpoints() {
        service.onStaffRoleChanged(10L);

        verify(staffRefreshSessionService).revokeAllForUser(10L, "ROLE_CHANGED");
        verify(userRepository).incrementAuthVersion(eq(10L), any());
        verify(pushEndpointService).disableAllForStaff(10L);
    }

    @Test
    void givenStaffSessionsRevoked_thenRevokesSessionsIncrementsVersionAndDisablesPushEndpoints() {
        service.onStaffSessionsRevoked(10L, "ADMIN_REVOKED");

        verify(staffRefreshSessionService).revokeAllForUser(10L, "ADMIN_REVOKED");
        verify(userRepository).incrementAuthVersion(eq(10L), any());
        verify(pushEndpointService).disableAllForStaff(10L);
    }

    @Test
    void givenCustomerLogoutAll_thenRevokesMobileSessionsAndDisablesPushEndpoints() {
        service.onCustomerLogoutAll(42L);

        verify(mobileRefreshSessionService).revokeAllForCustomer(42L, MobileRefreshRevocationReason.LOGOUT_ALL);
        verify(pushEndpointService).disableAllForCustomer(42L);
    }

    @Test
    void givenCustomerDeactivated_thenRevokesMobileSessionsAndDisablesPushEndpoints() {
        service.onCustomerDeactivated(42L);

        verify(mobileRefreshSessionService).revokeAllForCustomer(42L, MobileRefreshRevocationReason.ACCOUNT_INACTIVE);
        verify(pushEndpointService).disableAllForCustomer(42L);
    }

    @Test
    void givenTechnicianLogoutAll_thenRevokesMobileSessionsAndDisablesPushEndpoints() {
        service.onTechnicianLogoutAll(17L);

        verify(mobileRefreshSessionService).revokeAllForTechnician(17L, MobileRefreshRevocationReason.LOGOUT_ALL);
        verify(pushEndpointService).disableAllForTechnician(17L);
    }

    @Test
    void givenTechnicianDeactivated_thenRevokesMobileSessionsAndDisablesPushEndpoints() {
        service.onTechnicianDeactivated(17L);

        verify(mobileRefreshSessionService).revokeAllForTechnician(17L, MobileRefreshRevocationReason.ACCOUNT_INACTIVE);
        verify(pushEndpointService).disableAllForTechnician(17L);
    }

    @Test
    void givenTechnicianTelegramIdentityChanged_thenRevokesMobileSessionsAndDisablesPushEndpoints() {
        service.onTechnicianTelegramIdentityChanged(17L);

        verify(mobileRefreshSessionService).revokeAllForTechnician(17L, MobileRefreshRevocationReason.TELEGRAM_IDENTITY_CHANGED);
        verify(pushEndpointService).disableAllForTechnician(17L);
    }
}
