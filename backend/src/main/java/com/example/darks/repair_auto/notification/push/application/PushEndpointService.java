package com.example.darks.repair_auto.notification.push.application;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointRegisterRequest;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointResponse;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointUnregisterRequest;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.notification.push.domain.PushEndpoint;
import com.example.darks.repair_auto.notification.push.domain.PushFirebaseApp;
import com.example.darks.repair_auto.notification.push.domain.PushOwnerType;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import com.example.darks.repair_auto.notification.push.infrastructure.PushEndpointRepository;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushEndpointService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PushEndpointService.class);

    private final PushEndpointRepository repository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    @Autowired
    public PushEndpointService(
            PushEndpointRepository repository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            MeterRegistry meterRegistry) {
        this(repository, userRepository, customerRepository, technicianRepository, meterRegistry, Clock.systemUTC());
    }

    public PushEndpointService(
            PushEndpointRepository repository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository) {
        this(repository, userRepository, customerRepository, technicianRepository, null, Clock.systemUTC());
    }

    public PushEndpointService(
            PushEndpointRepository repository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            Clock clock) {
        this(repository, userRepository, customerRepository, technicianRepository, null, clock);
    }

    public PushEndpointService(
            PushEndpointRepository repository,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Transactional
    public PushEndpointResponse registerForStaff(Long userId, PushEndpointRegisterRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        validateCompatibility(PushOwnerType.STAFF, request.clientType(), request.platform(), request.firebaseAppKey());
        String fcmRegistrationToken = normalizeFcmRegistrationToken(request.fcmRegistrationToken());
        String appVersion = normalizeAppVersion(request.appVersion());
        OffsetDateTime now = now();

        PushEndpoint endpoint = repository.findByFirebaseAppKeyAndFcmRegistrationToken(request.firebaseAppKey(), fcmRegistrationToken)
                .map(existing -> {
                    if (existing.isOwnedByStaff(userId)) {
                        existing.touch(appVersion, now);
                    } else {
                        LOGGER.info("Transferring push endpoint id={} from ownerType={} to STAFF user id={}",
                                existing.getId(), existing.getOwnerType(), userId);
                        existing.reassignToStaff(user, request.clientType(), request.platform(), request.firebaseAppKey(), appVersion, now);
                    }
                    return existing;
                })
                .orElseGet(() -> PushEndpoint.forStaff(user, request.clientType(), request.platform(), request.firebaseAppKey(), fcmRegistrationToken, appVersion, now));

        return saveAndMap(endpoint, request.firebaseAppKey(), fcmRegistrationToken);
    }

    @Transactional
    public PushEndpointResponse registerForMobile(AuthenticatedMobileActor actor, PushEndpointRegisterRequest request) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        if (!actor.active()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }

        String fcmRegistrationToken = normalizeFcmRegistrationToken(request.fcmRegistrationToken());
        String appVersion = normalizeAppVersion(request.appVersion());
        OffsetDateTime now = now();

        if (actor.isCustomer()) {
            Customer customer = customerRepository.findById(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
            if (!customer.isActive()) {
                throw new BusinessException(ErrorCode.CUSTOMER_INACTIVE);
            }
            validateCompatibility(PushOwnerType.CUSTOMER, request.clientType(), request.platform(), request.firebaseAppKey());

            PushEndpoint endpoint = repository.findByFirebaseAppKeyAndFcmRegistrationToken(request.firebaseAppKey(), fcmRegistrationToken)
                    .map(existing -> {
                        if (existing.isOwnedByCustomer(actor.actorId())) {
                            existing.touch(appVersion, now);
                        } else {
                            LOGGER.info("Transferring push endpoint id={} from ownerType={} to CUSTOMER id={}",
                                    existing.getId(), existing.getOwnerType(), actor.actorId());
                            existing.reassignToCustomer(customer, request.clientType(), request.platform(), request.firebaseAppKey(), appVersion, now);
                        }
                        return existing;
                    })
                    .orElseGet(() -> PushEndpoint.forCustomer(customer, request.clientType(), request.platform(), request.firebaseAppKey(), fcmRegistrationToken, appVersion, now));

            return saveAndMap(endpoint, request.firebaseAppKey(), fcmRegistrationToken);
        } else if (actor.isTechnician()) {
            Technician technician = technicianRepository.findById(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.TECHNICIAN_NOT_FOUND));
            if (!technician.isActive()) {
                throw new BusinessException(ErrorCode.TECHNICIAN_INACTIVE);
            }
            validateCompatibility(PushOwnerType.TECHNICIAN, request.clientType(), request.platform(), request.firebaseAppKey());

            PushEndpoint endpoint = repository.findByFirebaseAppKeyAndFcmRegistrationToken(request.firebaseAppKey(), fcmRegistrationToken)
                    .map(existing -> {
                        if (existing.isOwnedByTechnician(actor.actorId())) {
                            existing.touch(appVersion, now);
                        } else {
                            LOGGER.info("Transferring push endpoint id={} from ownerType={} to TECHNICIAN id={}",
                                    existing.getId(), existing.getOwnerType(), actor.actorId());
                            existing.reassignToTechnician(technician, request.clientType(), request.platform(), request.firebaseAppKey(), appVersion, now);
                        }
                        return existing;
                    })
                    .orElseGet(() -> PushEndpoint.forTechnician(technician, request.clientType(), request.platform(), request.firebaseAppKey(), fcmRegistrationToken, appVersion, now));

            return saveAndMap(endpoint, request.firebaseAppKey(), fcmRegistrationToken);
        } else {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    @Transactional
    public void unregisterForStaff(Long userId, PushEndpointUnregisterRequest request) {
        if (userId == null || request == null) {
            return;
        }
        String fcmRegistrationToken = normalizeFcmRegistrationToken(request.fcmRegistrationToken());
        Optional<PushEndpoint> existing = repository.findByFirebaseAppKeyAndFcmRegistrationToken(request.firebaseAppKey(), fcmRegistrationToken);
        if (existing.isPresent()) {
            PushEndpoint endpoint = existing.get();
            if (endpoint.isOwnedByStaff(userId)) {
                endpoint.disable(now());
                repository.saveAndFlush(endpoint);
                countDisabledMetric("unregistered", 1);
            } else {
                LOGGER.debug("Unregister request for FCM registration token ignored as endpoint is not owned by current staff user");
            }
        }
    }

    @Transactional
    public void unregisterForMobile(AuthenticatedMobileActor actor, PushEndpointUnregisterRequest request) {
        if (actor == null || request == null) {
            return;
        }
        String fcmRegistrationToken = normalizeFcmRegistrationToken(request.fcmRegistrationToken());
        Optional<PushEndpoint> existing = repository.findByFirebaseAppKeyAndFcmRegistrationToken(request.firebaseAppKey(), fcmRegistrationToken);
        if (existing.isPresent()) {
            PushEndpoint endpoint = existing.get();
            boolean isOwner = (actor.isCustomer() && endpoint.isOwnedByCustomer(actor.actorId()))
                    || (actor.isTechnician() && endpoint.isOwnedByTechnician(actor.actorId()));
            if (isOwner) {
                endpoint.disable(now());
                repository.saveAndFlush(endpoint);
                countDisabledMetric("unregistered", 1);
            } else {
                LOGGER.debug("Unregister request for FCM registration token ignored as endpoint is not owned by current mobile actor");
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PushEndpoint> findEnabledForStaff(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return repository.findByStaffUserIdAndEnabledTrue(userId);
    }

    @Transactional(readOnly = true)
    public List<PushEndpoint> findEnabledForCustomer(Long customerId) {
        if (customerId == null) {
            return List.of();
        }
        return repository.findByCustomerIdAndEnabledTrue(customerId);
    }

    @Transactional(readOnly = true)
    public List<PushEndpoint> findEnabledForTechnician(Long technicianId) {
        if (technicianId == null) {
            return List.of();
        }
        return repository.findByTechnicianIdAndEnabledTrue(technicianId);
    }

    @Transactional
    public int disableAllForStaff(Long userId) {
        if (userId == null) {
            return 0;
        }
        int count = repository.disableAllForStaff(userId, now());
        if (count > 0) {
            countDisabledMetric("logout_all", count);
        }
        return count;
    }

    @Transactional
    public int disableAllForCustomer(Long customerId) {
        if (customerId == null) {
            return 0;
        }
        int count = repository.disableAllForCustomer(customerId, now());
        if (count > 0) {
            countDisabledMetric("logout_all", count);
        }
        return count;
    }

    @Transactional
    public int disableAllForTechnician(Long technicianId) {
        if (technicianId == null) {
            return 0;
        }
        int count = repository.disableAllForTechnician(technicianId, now());
        if (count > 0) {
            countDisabledMetric("logout_all", count);
        }
        return count;
    }

    @Transactional
    public void disableInvalidEndpoint(Long endpointId) {
        if (endpointId == null) {
            return;
        }
        repository.findById(endpointId).ifPresent(endpoint -> {
            LOGGER.info("Disabling invalid push endpoint id={} (ownerType={}) due to permanent delivery failure",
                    endpoint.getId(), endpoint.getOwnerType());
            endpoint.disable(now());
            repository.saveAndFlush(endpoint);
            countDisabledMetric("unregistered", 1);
        });
    }

    public void validateCompatibility(
            PushOwnerType ownerType,
            PushClientType clientType,
            PushPlatform platform,
            PushFirebaseApp firebaseAppKey) {
        if (ownerType == null || clientType == null || platform == null || firebaseAppKey == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        boolean valid = switch (ownerType) {
            case STAFF -> clientType == PushClientType.ADMIN_WEB
                    && platform == PushPlatform.WEB
                    && firebaseAppKey == PushFirebaseApp.ADMIN_WEB;
            case CUSTOMER -> clientType == PushClientType.CUSTOMER_MOBILE
                    && ((platform == PushPlatform.ANDROID && firebaseAppKey == PushFirebaseApp.CUSTOMER_ANDROID)
                    || (platform == PushPlatform.IOS && firebaseAppKey == PushFirebaseApp.CUSTOMER_IOS));
            case TECHNICIAN -> clientType == PushClientType.TECHNICIAN_MOBILE
                    && ((platform == PushPlatform.ANDROID && firebaseAppKey == PushFirebaseApp.TECHNICIAN_ANDROID)
                    || (platform == PushPlatform.IOS && firebaseAppKey == PushFirebaseApp.TECHNICIAN_IOS));
        };

        if (!valid) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private PushEndpointResponse saveAndMap(PushEndpoint endpoint, PushFirebaseApp appKey, String fcmRegistrationToken) {
        try {
            PushEndpoint saved = repository.saveAndFlush(endpoint);
            LOGGER.info("Successfully registered push endpoint id={}, ownerType={}, clientType={}, platform={}, firebaseAppKey={}",
                    saved.getId(), saved.getOwnerType(), saved.getClientType(), saved.getPlatform(), saved.getFirebaseAppKey());
            return new PushEndpointResponse(
                    saved.getId(),
                    saved.getClientType(),
                    saved.getPlatform(),
                    saved.getFirebaseAppKey(),
                    saved.isEnabled(),
                    saved.getLastSeenAt());
        } catch (DataIntegrityViolationException e) {
            LOGGER.debug("Handling concurrency conflict during push endpoint registration");
            PushEndpoint existing = repository.findByFirebaseAppKeyAndFcmRegistrationToken(appKey, fcmRegistrationToken)
                    .orElseThrow(() -> e);
            existing.touch(endpoint.getAppVersion(), now());
            PushEndpoint updated = repository.saveAndFlush(existing);
            return new PushEndpointResponse(
                    updated.getId(),
                    updated.getClientType(),
                    updated.getPlatform(),
                    updated.getFirebaseAppKey(),
                    updated.isEnabled(),
                    updated.getLastSeenAt());
        }
    }

    private void countDisabledMetric(String reason, int count) {
        if (meterRegistry != null) {
            Counter.builder("repairauto.push.endpoint.disabled")
                    .tag("reason", reason)
                    .register(meterRegistry)
                    .increment(count);
        }
    }

    private String normalizeFcmRegistrationToken(String fcmRegistrationToken) {
        if (fcmRegistrationToken == null || fcmRegistrationToken.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return fcmRegistrationToken.trim();
    }

    private String normalizeAppVersion(String appVersion) {
        if (appVersion == null || appVersion.isBlank()) {
            return null;
        }
        return appVersion.trim();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
