package com.example.darks.repair_auto.identity.application;

import com.example.darks.repair_auto.identity.domain.MobileRefreshRevocationReason;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.notification.push.application.PushEndpointService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActorAccessLifecycleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActorAccessLifecycleService.class);

    private final RefreshSessionService staffRefreshSessionService;
    private final MobileRefreshSessionService mobileRefreshSessionService;
    private final PushEndpointService pushEndpointService;
    private final UserRepository userRepository;
    private final Clock clock;

    @Autowired
    public ActorAccessLifecycleService(
            RefreshSessionService staffRefreshSessionService,
            MobileRefreshSessionService mobileRefreshSessionService,
            PushEndpointService pushEndpointService,
            UserRepository userRepository) {
        this(staffRefreshSessionService, mobileRefreshSessionService, pushEndpointService, userRepository, Clock.systemUTC());
    }

    public ActorAccessLifecycleService(
            RefreshSessionService staffRefreshSessionService,
            MobileRefreshSessionService mobileRefreshSessionService,
            PushEndpointService pushEndpointService,
            UserRepository userRepository,
            Clock clock) {
        this.staffRefreshSessionService = staffRefreshSessionService;
        this.mobileRefreshSessionService = mobileRefreshSessionService;
        this.pushEndpointService = pushEndpointService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public void onStaffLogoutAll(Long userId) {
        LOGGER.info("Executing staff logout-all lifecycle for userId={}", userId);
        staffRefreshSessionService.revokeAllForUser(userId, "LOGOUT_ALL");
        userRepository.incrementAuthVersion(userId, now());
        int disabledEndpoints = pushEndpointService.disableAllForStaff(userId);
        LOGGER.info("Staff logout-all completed: userId={}, disabledEndpoints={}", userId, disabledEndpoints);
    }

    @Transactional
    public void onStaffPasswordChanged(Long userId, String reason) {
        LOGGER.info("Executing staff password changed lifecycle for userId={}, reason={}", userId, reason);
        staffRefreshSessionService.revokeAllForUser(userId, reason);
        userRepository.incrementAuthVersion(userId, now());
        int disabledEndpoints = pushEndpointService.disableAllForStaff(userId);
        LOGGER.info("Staff password change lifecycle completed: userId={}, disabledEndpoints={}", userId, disabledEndpoints);
    }

    @Transactional
    public void onStaffDeactivated(Long userId) {
        LOGGER.info("Executing staff deactivation lifecycle for userId={}", userId);
        staffRefreshSessionService.revokeAllForUser(userId, "USER_DISABLED");
        userRepository.incrementAuthVersion(userId, now());
        int disabledEndpoints = pushEndpointService.disableAllForStaff(userId);
        LOGGER.info("Staff deactivation lifecycle completed: userId={}, disabledEndpoints={}", userId, disabledEndpoints);
    }

    @Transactional
    public void onStaffRoleChanged(Long userId) {
        LOGGER.info("Executing staff role change lifecycle for userId={}", userId);
        staffRefreshSessionService.revokeAllForUser(userId, "ROLE_CHANGED");
        userRepository.incrementAuthVersion(userId, now());
        int disabledEndpoints = pushEndpointService.disableAllForStaff(userId);
        LOGGER.info("Staff role change lifecycle completed: userId={}, disabledEndpoints={}", userId, disabledEndpoints);
    }

    @Transactional
    public void onStaffSessionsRevoked(Long userId, String reason) {
        LOGGER.info("Executing staff session revocation lifecycle for userId={}, reason={}", userId, reason);
        staffRefreshSessionService.revokeAllForUser(userId, reason);
        userRepository.incrementAuthVersion(userId, now());
        int disabledEndpoints = pushEndpointService.disableAllForStaff(userId);
        LOGGER.info("Staff session revocation completed: userId={}, disabledEndpoints={}", userId, disabledEndpoints);
    }

    @Transactional
    public void onCustomerLogoutAll(Long customerId) {
        LOGGER.info("Executing customer logout-all lifecycle for customerId={}", customerId);
        mobileRefreshSessionService.revokeAllForCustomer(customerId, MobileRefreshRevocationReason.LOGOUT_ALL);
        int disabledEndpoints = pushEndpointService.disableAllForCustomer(customerId);
        LOGGER.info("Customer logout-all completed: customerId={}, disabledEndpoints={}", customerId, disabledEndpoints);
    }

    @Transactional
    public void onCustomerDeactivated(Long customerId) {
        LOGGER.info("Executing customer deactivation lifecycle for customerId={}", customerId);
        mobileRefreshSessionService.revokeAllForCustomer(customerId, MobileRefreshRevocationReason.ACCOUNT_INACTIVE);
        int disabledEndpoints = pushEndpointService.disableAllForCustomer(customerId);
        LOGGER.info("Customer deactivation completed: customerId={}, disabledEndpoints={}", customerId, disabledEndpoints);
    }

    @Transactional
    public void onTechnicianLogoutAll(Long technicianId) {
        LOGGER.info("Executing technician logout-all lifecycle for technicianId={}", technicianId);
        mobileRefreshSessionService.revokeAllForTechnician(technicianId, MobileRefreshRevocationReason.LOGOUT_ALL);
        int disabledEndpoints = pushEndpointService.disableAllForTechnician(technicianId);
        LOGGER.info("Technician logout-all completed: technicianId={}, disabledEndpoints={}", technicianId, disabledEndpoints);
    }

    @Transactional
    public void onTechnicianDeactivated(Long technicianId) {
        LOGGER.info("Executing technician deactivation lifecycle for technicianId={}", technicianId);
        mobileRefreshSessionService.revokeAllForTechnician(technicianId, MobileRefreshRevocationReason.ACCOUNT_INACTIVE);
        int disabledEndpoints = pushEndpointService.disableAllForTechnician(technicianId);
        LOGGER.info("Technician deactivation completed: technicianId={}, disabledEndpoints={}", technicianId, disabledEndpoints);
    }

    @Transactional
    public void onTechnicianTelegramIdentityChanged(Long technicianId) {
        LOGGER.info("Executing technician telegram identity changed lifecycle for technicianId={}", technicianId);
        mobileRefreshSessionService.revokeAllForTechnician(technicianId, MobileRefreshRevocationReason.TELEGRAM_IDENTITY_CHANGED);
        int disabledEndpoints = pushEndpointService.disableAllForTechnician(technicianId);
        LOGGER.info("Technician telegram identity changed lifecycle completed: technicianId={}, disabledEndpoints={}", technicianId, disabledEndpoints);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
