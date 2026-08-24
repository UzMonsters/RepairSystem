package com.example.darks.repair_auto.identity.mobile.profile.application;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfilePatchRequest;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfileResponse;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.i18n.SupportedLanguage;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MobileProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileProfileService.class);

    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final Clock clock;

    @Autowired
    public MobileProfileService(
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository) {
        this(customerRepository, technicianRepository, Clock.systemUTC());
    }

    public MobileProfileService(
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            Clock clock) {
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MobileProfileResponse getProfile(AuthenticatedMobileActor actor) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        if (actor.isCustomer()) {
            Customer customer = customerRepository.findById(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!customer.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }
            return toCustomerResponse(customer);
        } else if (actor.isTechnician()) {
            Technician technician = technicianRepository.findById(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!technician.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }
            return toTechnicianResponse(technician);
        }

        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    @Transactional
    public MobileProfileResponse updateProfile(AuthenticatedMobileActor actor, MobileProfilePatchRequest request) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        OffsetDateTime now = now();

        if (actor.isCustomer()) {
            Customer customer = customerRepository.findById(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!customer.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }

            String newFullName = customer.getFullName();
            if (request.fullName() != null) {
                if (request.fullName().isBlank()) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
                newFullName = request.fullName().trim();
            }

            LanguageCode newLanguage = customer.getPreferredLanguage();
            if (request.preferredLanguage() != null) {
                SupportedLanguage parsed = SupportedLanguage.fromCode(request.preferredLanguage());
                if (parsed == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
                newLanguage = LanguageCode.valueOf(parsed.name());
            }

            customer.updateProfile(newFullName, customer.getPhone(), newLanguage, now);
            LOGGER.info("Updated mobile customer profile for customerId={}", customer.getId());
            return toCustomerResponse(customer);
        } else if (actor.isTechnician()) {
            Technician technician = technicianRepository.findById(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!technician.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }

            if (request.preferredLanguage() != null) {
                SupportedLanguage parsed = SupportedLanguage.fromCode(request.preferredLanguage());
                if (parsed == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
                LanguageCode newLanguage = LanguageCode.valueOf(parsed.name());
                technician.updateTelegramLanguage(newLanguage, now);
                LOGGER.info("Updated mobile technician preferred language for technicianId={}", technician.getId());
            }

            return toTechnicianResponse(technician);
        }

        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    private MobileProfileResponse toCustomerResponse(Customer customer) {
        String lang = customer.getPreferredLanguage() != null
                ? customer.getPreferredLanguage().name().toLowerCase(Locale.ROOT)
                : "uz";
        return MobileProfileResponse.forCustomer(
                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getPhoneVerifiedAt() != null,
                customer.getEmail(),
                customer.getEmailVerifiedAt() != null,
                lang,
                customer.isTelegramLinked());
    }

    private MobileProfileResponse toTechnicianResponse(Technician technician) {
        String lang = technician.getPreferredLanguage() != null
                ? technician.getPreferredLanguage().name().toLowerCase(Locale.ROOT)
                : "uz";
        return MobileProfileResponse.forTechnician(
                technician.getId(),
                technician.getFullName(),
                technician.getPhone(),
                technician.getPhoneVerifiedAt() != null,
                technician.getEmail(),
                technician.getEmailVerifiedAt() != null,
                lang,
                technician.isTelegramLinked(),
                technician.getSpecialization(),
                technician.getMaximumConcurrentRequests(),
                technician.isActive());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
