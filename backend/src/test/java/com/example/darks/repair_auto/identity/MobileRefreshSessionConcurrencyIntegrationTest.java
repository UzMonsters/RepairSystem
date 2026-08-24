package com.example.darks.repair_auto.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.customer.api.dto.CustomerCreateRequest;
import com.example.darks.repair_auto.customer.application.CustomerService;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.MobileRefreshSessionService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.domain.MobileRefreshRevocationReason;
import com.example.darks.repair_auto.identity.domain.MobileRefreshSession;
import com.example.darks.repair_auto.identity.domain.MobileSession;
import com.example.darks.repair_auto.identity.infrastructure.persistence.MobileRefreshSessionRepository;
import com.example.darks.repair_auto.identity.infrastructure.persistence.MobileSessionRepository;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.api.dto.TechnicianCreateRequest;
import com.example.darks.repair_auto.technician.application.TechnicianService;
import com.example.darks.repair_auto.technician.domain.Technician;
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

@SpringBootTest
class MobileRefreshSessionConcurrencyIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MobileRefreshSessionService refreshSessionService;

    @Autowired
    private MobileRefreshSessionRepository refreshSessionRepository;

    @Autowired
    private MobileSessionRepository mobileSessionRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TechnicianService technicianService;

    @Autowired
    private TechnicianRepository technicianRepository;

    private Customer customer;
    private Technician technician;
    private MobileSession customerSession;
    private MobileSession technicianSession;

    @BeforeEach
    void setUp() {
        refreshSessionRepository.deleteAll();
        mobileSessionRepository.deleteAll();
        technicianRepository.deleteAll();
        customerRepository.deleteAll();
        Long customerId = customerService.create(new CustomerCreateRequest(
                "Mobile Race Customer",
                "90 111 88 77",
                LanguageCode.UZ)).id();
        Long technicianId = technicianService.create(new TechnicianCreateRequest(
                "Mobile Race Technician",
                "90 222 88 77",
                "AC",
                null,
                5,
                LanguageCode.UZ,
                true)).id();
        customer = customerRepository.findById(customerId).orElseThrow();
        technician = technicianRepository.findById(technicianId).orElseThrow();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        customerSession = mobileSessionRepository.save(MobileSession.forCustomer(
                customer,
                MobileAuthProvider.PHONE,
                PushPlatform.ANDROID,
                null,
                null,
                null,
                "127.0.0.1",
                "test-agent",
                now,
                now.plusDays(30)));
        technicianSession = mobileSessionRepository.save(MobileSession.forTechnician(
                technician,
                MobileAuthProvider.PHONE,
                PushPlatform.ANDROID,
                null,
                null,
                null,
                "127.0.0.1",
                "test-agent",
                now,
                now.plusDays(30)));
    }

    @Test
    void givenCustomerMobileRefreshTokenUsedTwiceConcurrentlyThenAtMostOneRotationSucceeds() throws Exception {
        String rawToken = refreshSessionService.createForCustomer(customer, customerSession).rawToken();

        List<Object> results = runConcurrently(
                () -> refreshSessionService.rotate(rawToken),
                () -> refreshSessionService.rotate(rawToken));

        assertThat(results)
                .filteredOn(MobileRefreshSessionService.MobileRotationResult.class::isInstance)
                .hasSizeLessThanOrEqualTo(1);
        assertThat(results).anyMatch(result -> result instanceof BusinessException
                && ((BusinessException) result).getErrorCode().name().equals("MOBILE_REFRESH_TOKEN_REUSED"));
        assertThat(activeSessionsForCustomer()).isLessThanOrEqualTo(1);
        assertOriginalTokenIsNotUsable(rawToken);
    }

    @Test
    void givenTechnicianMobileRefreshTokenUsedTwiceConcurrentlyThenAtMostOneRotationSucceeds() throws Exception {
        String rawToken = refreshSessionService.createForTechnician(technician, technicianSession).rawToken();

        List<Object> results = runConcurrently(
                () -> refreshSessionService.rotate(rawToken),
                () -> refreshSessionService.rotate(rawToken));

        assertThat(results)
                .filteredOn(MobileRefreshSessionService.MobileRotationResult.class::isInstance)
                .hasSizeLessThanOrEqualTo(1);
        assertThat(results).anyMatch(result -> result instanceof BusinessException
                && ((BusinessException) result).getErrorCode().name().equals("MOBILE_REFRESH_TOKEN_REUSED"));
        assertThat(activeSessionsForTechnician()).isLessThanOrEqualTo(1);
        assertOriginalTokenIsNotUsable(rawToken);
    }

    @Test
    void givenCustomerRefreshAndLogoutRaceThenOriginalTokenCannotRemainUsable() throws Exception {
        String rawToken = refreshSessionService.createForCustomer(customer, customerSession).rawToken();

        List<Object> results = runConcurrently(
                () -> refreshSessionService.rotate(rawToken),
                () -> {
                    refreshSessionService.revokeByRawToken(rawToken);
                    return "LOGOUT";
                });

        assertThat(results).hasSize(2);
        assertThat(activeSessionsForCustomer()).isLessThanOrEqualTo(1);
        assertOriginalTokenIsNotUsable(rawToken);
    }

    @Test
    void givenTechnicianRefreshAndLogoutAllRaceThenOwnershipAndFamilyRevocationRemainCoherent() throws Exception {
        String rawToken = refreshSessionService.createForTechnician(technician, technicianSession).rawToken();

        List<Object> results = runConcurrently(
                () -> refreshSessionService.rotate(rawToken),
                () -> {
                    refreshSessionService.revokeAllForTechnician(
                            technician.getId(),
                            MobileRefreshRevocationReason.LOGOUT_ALL);
                    return "LOGOUT_ALL";
                });

        assertThat(results).hasSize(2);
        assertThat(activeSessionsForTechnician()).isLessThanOrEqualTo(1);
        assertThat(refreshSessionRepository.findByTechnicianId(technician.getId()))
                .allMatch(session -> session.getActorType() == ActorType.TECHNICIAN
                && session.getTechnician().getId().equals(technician.getId())
                && session.getCustomer() == null);
        assertOriginalTokenIsNotUsable(rawToken);
    }

    @Test
    void givenCustomerRefreshAndLogoutAllRaceThenOwnershipAndFamilyRevocationRemainCoherent() throws Exception {
        String rawToken = refreshSessionService.createForCustomer(customer, customerSession).rawToken();

        List<Object> results = runConcurrently(
                () -> refreshSessionService.rotate(rawToken),
                () -> {
                    refreshSessionService.revokeAllForCustomer(
                            customer.getId(),
                            MobileRefreshRevocationReason.LOGOUT_ALL);
                    return "LOGOUT_ALL";
                });

        assertThat(results).hasSize(2);
        assertThat(activeSessionsForCustomer()).isLessThanOrEqualTo(1);
        assertThat(refreshSessionRepository.findByCustomerId(customer.getId()))
                .allMatch(session -> session.getActorType() == ActorType.CUSTOMER
                && session.getCustomer().getId().equals(customer.getId())
                && session.getTechnician() == null);
        assertOriginalTokenIsNotUsable(rawToken);
    }

    private void assertOriginalTokenIsNotUsable(String rawToken) {
        Object result = runCatching(() -> refreshSessionService.rotate(rawToken));
        assertThat(result).isInstanceOf(BusinessException.class);
    }

    private long activeSessionsForCustomer() {
        return refreshSessionRepository.findByCustomerId(customer.getId())
                .stream()
                .filter(this::isUsable)
                .count();
    }

    private long activeSessionsForTechnician() {
        return refreshSessionRepository.findByTechnicianId(technician.getId())
                .stream()
                .filter(this::isUsable)
                .count();
    }

    private boolean isUsable(MobileRefreshSession session) {
        return !session.isUsed() && !session.isRevoked();
    }

    private List<Object> runConcurrently(Callable<?> firstAction, Callable<?> secondAction) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> runAfterStart(firstAction, start));
            var second = executor.submit(() -> runAfterStart(secondAction, start));
            start.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }
    }

    private Object runAfterStart(Callable<?> action, CountDownLatch start) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        return runCatching(action);
    }

    private Object runCatching(Callable<?> action) {
        try {
            return action.call();
        } catch (Exception exception) {
            return exception;
        }
    }
}
