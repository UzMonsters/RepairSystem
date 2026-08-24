package com.example.darks.repair_auto.identity.mobile.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.TokenHashService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.EmailVerificationChallenge;
import com.example.darks.repair_auto.identity.infrastructure.persistence.EmailVerificationChallengeRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.email.dto.EmailVerificationConfirmRequest;
import com.example.darks.repair_auto.identity.mobile.email.dto.EmailVerificationRequest;
import com.example.darks.repair_auto.identity.mobile.email.dto.EmailVerificationResponse;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmailVerificationServiceTest {

    private EmailVerificationChallengeRepository challengeRepository;
    private CustomerRepository customerRepository;
    private TechnicianRepository technicianRepository;
    private EmailNormalizer emailNormalizer;
    private TokenHashService tokenHashService;
    private EmailVerificationProperties properties;
    private EmailSender emailSender;
    private Clock clock;
    private EmailVerificationService service;

    private final Instant nowInstant = Instant.parse("2026-08-24T10:00:00Z");
    private final OffsetDateTime nowUtc = OffsetDateTime.ofInstant(nowInstant, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        challengeRepository = mock(EmailVerificationChallengeRepository.class);
        customerRepository = mock(CustomerRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        emailNormalizer = new EmailNormalizer();
        tokenHashService = new TokenHashService();
        properties = new EmailVerificationProperties();
        properties.setEnabled(true);
        emailSender = mock(EmailSender.class);
        when(emailSender.isEnabled()).thenReturn(true);
        clock = Clock.fixed(nowInstant, ZoneOffset.UTC);

        service = new EmailVerificationService(
                challengeRepository,
                customerRepository,
                technicianRepository,
                emailNormalizer,
                tokenHashService,
                properties,
                emailSender,
                clock);
    }

    @Test
    void givenCustomerWhenRequestVerificationThenSavesChallengeAndSendsEmail() {
        Customer customer = Customer.google("Customer", "test@example.com", nowUtc, LanguageCode.EN, nowUtc);
        ReflectionTestUtils.setField(customer, "id", 100L);
        when(customerRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(customer));
        when(challengeRepository.findTopActiveByCustomer(100L, nowUtc)).thenReturn(Optional.empty());
        when(challengeRepository.save(any(EmailVerificationChallenge.class))).thenAnswer(i -> i.getArgument(0));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 100L);
        EmailVerificationResponse response = service.request(actor, new EmailVerificationRequest("newemail@example.com"));

        assertThat(response.deliveryStatus()).isEqualTo("SENT");
        verify(emailSender).sendVerificationCode(eq("newemail@example.com"), any(String.class), eq(LanguageCode.EN));
    }

    @Test
    void givenCooldownActiveWhenRequestVerificationThenThrowsCooldownException() {
        Customer customer = Customer.google("Customer", "test@example.com", nowUtc, LanguageCode.EN, nowUtc);
        ReflectionTestUtils.setField(customer, "id", 100L);
        EmailVerificationChallenge existingChallenge = EmailVerificationChallenge.forCustomer(
                customer,
                "test@example.com",
                "hash",
                5,
                nowUtc,
                nowUtc.plusMinutes(10),
                nowUtc.plusSeconds(30));

        when(challengeRepository.findTopActiveByCustomer(100L, nowUtc)).thenReturn(Optional.of(existingChallenge));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 100L);
        assertThatThrownBy(() -> service.request(actor, new EmailVerificationRequest("newemail@example.com")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PHONE_OTP_RESEND_COOLDOWN);
    }

    @Test
    void givenValidCodeWhenVerifyCustomerThenUpdatesCustomerEmailAndConsumesChallenge() {
        Customer customer = Customer.google("Customer", "old@example.com", nowUtc, LanguageCode.EN, nowUtc);
        ReflectionTestUtils.setField(customer, "id", 100L);

        String rawCode = "123456";
        String codeHash = tokenHashService.hash(rawCode);
        EmailVerificationChallenge challenge = EmailVerificationChallenge.forCustomer(
                customer,
                "new@example.com",
                codeHash,
                5,
                nowUtc,
                nowUtc.plusMinutes(10),
                nowUtc.plusSeconds(60));

        UUID challengeId = challenge.getId();
        when(challengeRepository.findByIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 100L);
        service.verify(actor, new EmailVerificationConfirmRequest(challengeId, rawCode));

        assertThat(customer.getEmail()).isEqualTo("new@example.com");
        assertThat(customer.getEmailVerifiedAt()).isNotNull();
        assertThat(challenge.isConsumed()).isTrue();
    }

    @Test
    void givenInvalidCodeWhenVerifyThenIncrementsAttemptsAndThrowsException() {
        Customer customer = Customer.google("Customer", "old@example.com", nowUtc, LanguageCode.EN, nowUtc);
        ReflectionTestUtils.setField(customer, "id", 100L);

        String codeHash = tokenHashService.hash("123456");
        EmailVerificationChallenge challenge = EmailVerificationChallenge.forCustomer(
                customer,
                "new@example.com",
                codeHash,
                5,
                nowUtc,
                nowUtc.plusMinutes(10),
                nowUtc.plusSeconds(60));

        UUID challengeId = challenge.getId();
        when(challengeRepository.findByIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 100L);
        assertThatThrownBy(() -> service.verify(actor, new EmailVerificationConfirmRequest(challengeId, "999999")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_VERIFICATION_INVALID);

        assertThat(challenge.isConsumed()).isFalse();
    }

    @Test
    void givenCustomerWhenRemoveEmailThenClearsEmail() {
        Customer customer = Customer.google("Customer", "test@example.com", nowUtc, LanguageCode.EN, nowUtc);
        ReflectionTestUtils.setField(customer, "id", 100L);
        when(customerRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(customer));

        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 100L);
        service.remove(actor);

        assertThat(customer.getEmail()).isNull();
        assertThat(customer.getEmailVerifiedAt()).isNull();
    }
}
