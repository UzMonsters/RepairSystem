package com.example.darks.repair_auto.identity.mobile.auth;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.application.MobileRefreshSessionService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.MobileAuthIdentity;
import com.example.darks.repair_auto.identity.domain.MobileAuthProvider;
import com.example.darks.repair_auto.identity.domain.MobileRefreshRevocationReason;
import com.example.darks.repair_auto.identity.infrastructure.persistence.MobileAuthIdentityRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.auth.dto.GoogleLinkRequest;
import com.example.darks.repair_auto.identity.mobile.auth.dto.MobileAuthMethodResponse;
import com.example.darks.repair_auto.identity.mobile.google.GoogleIdTokenVerifier;
import com.example.darks.repair_auto.identity.mobile.google.GoogleIdentity;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MobileAuthMethodService {

    private final MobileAuthIdentityRepository identityRepository;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final EmailNormalizer emailNormalizer;
    private final MobileRefreshSessionService refreshSessionService;
    private final Clock clock;

    @Autowired
    public MobileAuthMethodService(
            MobileAuthIdentityRepository identityRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            GoogleIdTokenVerifier googleIdTokenVerifier,
            EmailNormalizer emailNormalizer,
            MobileRefreshSessionService refreshSessionService) {
        this(identityRepository, customerRepository, technicianRepository, googleIdTokenVerifier, emailNormalizer,
                refreshSessionService, Clock.systemUTC());
    }

    MobileAuthMethodService(
            MobileAuthIdentityRepository identityRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            GoogleIdTokenVerifier googleIdTokenVerifier,
            EmailNormalizer emailNormalizer,
            MobileRefreshSessionService refreshSessionService,
            Clock clock) {
        this.identityRepository = identityRepository;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.emailNormalizer = emailNormalizer;
        this.refreshSessionService = refreshSessionService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MobileAuthMethodResponse> list(AuthenticatedMobileActor actor) {
        requireActor(actor);
        Map<MobileAuthProvider, MobileAuthIdentity> byProvider = new EnumMap<>(MobileAuthProvider.class);
        for (MobileAuthIdentity identity : identityRepository.findActiveForActor(actor.actorType(), actor.actorId())) {
            byProvider.put(identity.getProvider(), identity);
        }
        return List.of(MobileAuthProvider.TELEGRAM, MobileAuthProvider.GOOGLE, MobileAuthProvider.PHONE).stream()
                .map(provider -> toResponse(provider, byProvider.get(provider)))
                .toList();
    }

    @Transactional
    public void linkGoogle(AuthenticatedMobileActor actor, GoogleLinkRequest request) {
        requireActor(actor);
        PushClientType clientType = MobileClientTypeResolver.clientType(actor.actorType());
        GoogleIdentity google = googleIdTokenVerifier.verify(request.idToken(), clientType);
        if (!google.emailVerified()) {
            throw new BusinessException(ErrorCode.GOOGLE_EMAIL_NOT_VERIFIED);
        }
        String email = google.email() == null ? null : emailNormalizer.normalize(google.email());
        identityRepository.findActiveByProviderForUpdate(actor.actorType(), MobileAuthProvider.GOOGLE, google.subject())
                .ifPresent(existing -> {
                    if (!existing.getActorId().equals(actor.actorId())) {
                        throw new BusinessException(ErrorCode.MOBILE_PROVIDER_CONFLICT);
                    }
                });
        if (identityRepository.findActiveActorProviderForUpdate(
                actor.actorType(),
                actor.actorId(),
                MobileAuthProvider.GOOGLE).isPresent()) {
            throw new BusinessException(ErrorCode.MOBILE_PROVIDER_CONFLICT);
        }
        OffsetDateTime now = now();
        try {
            if (actor.actorType() == ActorType.CUSTOMER) {
                Customer customer = customerRepository.findByIdForUpdate(actor.actorId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
                identityRepository.saveAndFlush(MobileAuthIdentity.forCustomer(
                        customer,
                        MobileAuthProvider.GOOGLE,
                        google.subject(),
                        email,
                        null,
                        now));
                if (customer.getEmail() == null && email != null) {
                    customer.setEmail(email, now, now);
                } else if (email != null && email.equalsIgnoreCase(customer.getEmail()) && customer.getEmailVerifiedAt() == null) {
                    customer.setEmail(customer.getEmail(), now, now);
                }
            } else {
                Technician technician = technicianRepository.findByIdForUpdate(actor.actorId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
                identityRepository.saveAndFlush(MobileAuthIdentity.forTechnician(
                        technician,
                        MobileAuthProvider.GOOGLE,
                        google.subject(),
                        email,
                        null,
                        now));
                if (technician.getEmail() == null && email != null) {
                    technician.setEmail(email, now, now);
                } else if (email != null && email.equalsIgnoreCase(technician.getEmail()) && technician.getEmailVerifiedAt() == null) {
                    technician.setEmail(technician.getEmail(), now, now);
                }
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.MOBILE_PROVIDER_CONFLICT);
        }
    }

    @Transactional
    public void unlinkGoogle(AuthenticatedMobileActor actor, UUID currentSessionId) {
        requireActor(actor);
        OffsetDateTime now = now();

        List<MobileAuthIdentity> activeIdentities = identityRepository.findActiveForActor(actor.actorType(), actor.actorId());
        if (activeIdentities.size() <= 1) {
            throw new BusinessException(ErrorCode.LAST_AUTH_METHOD);
        }

        identityRepository.findActiveActorProviderForUpdate(actor.actorType(), actor.actorId(), MobileAuthProvider.GOOGLE)
                .ifPresent(identity -> identity.disable(now));

        if (refreshSessionService != null) {
            refreshSessionService.revokeOtherFamiliesForActor(
                    currentSessionId,
                    actor.actorType(),
                    actor.actorId(),
                    MobileRefreshRevocationReason.AUTH_METHOD_UNLINKED);
        }
    }

    @Transactional
    public void unlinkTelegram(AuthenticatedMobileActor actor, UUID currentSessionId) {
        requireActor(actor);
        OffsetDateTime now = now();

        List<MobileAuthIdentity> activeIdentities = identityRepository.findActiveForActor(actor.actorType(), actor.actorId());
        if (activeIdentities.size() <= 1) {
            throw new BusinessException(ErrorCode.LAST_AUTH_METHOD);
        }

        identityRepository.findActiveActorProviderForUpdate(actor.actorType(), actor.actorId(), MobileAuthProvider.TELEGRAM)
                .ifPresent(identity -> identity.disable(now));

        if (actor.actorType() == ActorType.CUSTOMER) {
            customerRepository.findByIdForUpdate(actor.actorId())
                    .ifPresent(c -> c.unlinkTelegram(now));
        } else {
            technicianRepository.findByIdForUpdate(actor.actorId())
                    .ifPresent(t -> t.unlinkTelegram(now));
        }

        if (refreshSessionService != null) {
            refreshSessionService.revokeOtherFamiliesForActor(
                    currentSessionId,
                    actor.actorType(),
                    actor.actorId(),
                    MobileRefreshRevocationReason.AUTH_METHOD_UNLINKED);
        }
    }

    @Transactional
    public void unlinkPhone(AuthenticatedMobileActor actor, UUID currentSessionId) {
        requireActor(actor);
        OffsetDateTime now = now();

        if (actor.actorType() == ActorType.TECHNICIAN) {
            throw new BusinessException(ErrorCode.LAST_AUTH_METHOD);
        }

        List<MobileAuthIdentity> activeIdentities = identityRepository.findActiveForActor(actor.actorType(), actor.actorId());
        if (activeIdentities.size() <= 1) {
            throw new BusinessException(ErrorCode.LAST_AUTH_METHOD);
        }

        identityRepository.findActiveActorProviderForUpdate(actor.actorType(), actor.actorId(), MobileAuthProvider.PHONE)
                .ifPresent(identity -> identity.disable(now));

        customerRepository.findByIdForUpdate(actor.actorId())
                .ifPresent(c -> c.removePhone(now));

        if (refreshSessionService != null) {
            refreshSessionService.revokeOtherFamiliesForActor(
                    currentSessionId,
                    actor.actorType(),
                    actor.actorId(),
                    MobileRefreshRevocationReason.AUTH_METHOD_UNLINKED);
        }
    }

    private MobileAuthMethodResponse toResponse(MobileAuthProvider provider, MobileAuthIdentity identity) {
        if (identity == null) {
            return new MobileAuthMethodResponse(provider, false, null, null, null);
        }
        String display = switch (provider) {
            case TELEGRAM -> identity.getProviderSubject();
            case GOOGLE -> maskEmail(identity.getProviderEmail());
            case PHONE -> maskPhone(identity.getProviderPhone());
        };
        return new MobileAuthMethodResponse(provider, true, display, identity.getLinkedAt(), identity.getLastUsedAt());
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        if (phone.length() >= 12 && phone.startsWith("+998")) {
            return phone.substring(0, 6) + " *** ** " + phone.substring(phone.length() - 2);
        }
        if (phone.length() > 4) {
            return phone.substring(0, 3) + " *** " + phone.substring(phone.length() - 2);
        }
        return phone;
    }

    private void requireActor(AuthenticatedMobileActor actor) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
