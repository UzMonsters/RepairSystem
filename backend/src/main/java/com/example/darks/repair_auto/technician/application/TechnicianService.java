package com.example.darks.repair_auto.technician.application;

import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.example.darks.repair_auto.shared.phone.PhoneNumberNormalizer;
import com.example.darks.repair_auto.technician.api.dto.TechnicianCreateRequest;
import com.example.darks.repair_auto.technician.api.dto.TechnicianDetailResponse;
import com.example.darks.repair_auto.technician.api.dto.TechnicianMapper;
import com.example.darks.repair_auto.technician.api.dto.TechnicianSummaryResponse;
import com.example.darks.repair_auto.technician.api.dto.TechnicianUpdateRequest;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TechnicianService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TechnicianService.class);

    private final TechnicianRepository technicianRepository;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final com.example.darks.repair_auto.identity.application.ActorAccessLifecycleService actorAccessLifecycleService;

    @org.springframework.beans.factory.annotation.Autowired
    public TechnicianService(
            TechnicianRepository technicianRepository,
            PhoneNumberNormalizer phoneNumberNormalizer,
            com.example.darks.repair_auto.identity.application.ActorAccessLifecycleService actorAccessLifecycleService) {
        this.technicianRepository = technicianRepository;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.actorAccessLifecycleService = actorAccessLifecycleService;
    }

    public TechnicianService(
            TechnicianRepository technicianRepository,
            PhoneNumberNormalizer phoneNumberNormalizer) {
        this(technicianRepository, phoneNumberNormalizer, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<TechnicianSummaryResponse> list(
            String search,
            String phone,
            String specialization,
            Boolean active,
            Boolean telegramLinked,
            Pageable pageable) {
        String normalizedPhone = phone == null || phone.isBlank() ? null : phoneNumberNormalizer.normalize(phone);
        String normalizedSearchPhone = normalizeSearchPhone(search);
        return PageResponse.from(technicianRepository.findAll(filters(
                blankToNull(search),
                normalizedSearchPhone,
                normalizedPhone,
                blankToNull(specialization),
                active,
                telegramLinked), pageable).map(TechnicianMapper::summary));
    }

    @Transactional(readOnly = true)
    public TechnicianDetailResponse get(Long id) {
        return TechnicianMapper.details(find(id));
    }

    @Transactional
    public TechnicianDetailResponse create(TechnicianCreateRequest request) {
        Technician technician = new Technician(
                request.fullName().trim(),
                phoneNumberNormalizer.normalize(request.phone()),
                blankToNull(request.specialization()),
                blankToNull(request.notes()),
                request.maximumConcurrentRequests(),
                request.preferredLanguage(),
                request.active(),
                now());
        validateMaximum(technician.getMaximumConcurrentRequests());
        try {
            Technician saved = technicianRepository.saveAndFlush(technician);
            LOGGER.info("Technician event operation=technician_created result=success technicianId={}", saved.getId());
            return TechnicianMapper.details(saved);
        } catch (DataIntegrityViolationException exception) {
            throw technicianConflict(exception);
        }
    }

    @Transactional
    public TechnicianDetailResponse update(Long id, TechnicianUpdateRequest request) {
        Technician technician = technicianRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
        validateMaximum(request.maximumConcurrentRequests());
        technician.updateProfile(
                request.fullName().trim(),
                phoneNumberNormalizer.normalize(request.phone()),
                blankToNull(request.specialization()),
                blankToNull(request.notes()),
                request.maximumConcurrentRequests(),
                request.preferredLanguage(),
                now());
        try {
            return TechnicianMapper.details(technicianRepository.saveAndFlush(technician));
        } catch (DataIntegrityViolationException exception) {
            throw technicianConflict(exception);
        }
    }

    @Transactional
    public TechnicianDetailResponse changeActivation(Long id, boolean active, String reason) {
        Technician technician = technicianRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
        boolean deactivated = technician.isActive() && !active;
        technician.setActive(active, now());
        if (deactivated && actorAccessLifecycleService != null) {
            actorAccessLifecycleService.onTechnicianDeactivated(id);
        }
        LOGGER.info(
                "Technician event operation=technician_activation_changed result=success technicianId={} active={} reason={}",
                id,
                active,
                reason == null ? "" : reason.trim());
        return TechnicianMapper.details(technician);
    }

    private Technician find(Long id) {
        return technicianRepository.findById(id).orElseThrow(this::notFound);
    }

    private void validateMaximum(int maximumConcurrentRequests) {
        if (maximumConcurrentRequests < 1) {
            throw new BusinessException(ErrorCode.INVALID_MAXIMUM_CONCURRENT_REQUESTS);
        }
    }

    private BusinessRuleException notFound() {
        return new BusinessRuleException(ErrorCode.TECHNICIAN_NOT_FOUND);
    }

    private BusinessRuleException technicianConflict(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause() != null ? exception.getMostSpecificCause().getMessage() : "";
        if (message != null && message.contains("telegram_user_id")) {
            return new BusinessRuleException(ErrorCode.TECHNICIAN_TELEGRAM_ID_ALREADY_EXISTS);
        }
        return new BusinessRuleException(ErrorCode.TECHNICIAN_PHONE_ALREADY_EXISTS);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeSearchPhone(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        try {
            return phoneNumberNormalizer.normalize(search);
        } catch (BusinessRuleException exception) {
            return null;
        }
    }

    private Specification<Technician> filters(
            String search,
            String normalizedSearchPhone,
            String phone,
            String specialization,
            Boolean active,
            Boolean telegramLinked) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("fullName")), pattern),
                        builder.like(root.get("phone"), "%" + search + "%"),
                        normalizedSearchPhone == null
                                ? builder.disjunction()
                                : builder.equal(root.get("phone"), normalizedSearchPhone),
                        builder.like(builder.lower(root.get("specialization")), pattern)));
            }
            if (phone != null) {
                predicate = builder.and(predicate, builder.equal(root.get("phone"), phone));
            }
            if (specialization != null) {
                String pattern = "%" + specialization.toLowerCase(Locale.ROOT) + "%";
                predicate = builder.and(predicate, builder.like(builder.lower(root.get("specialization")), pattern));
            }
            if (active != null) {
                predicate = builder.and(predicate, builder.equal(root.get("active"), active));
            }
            if (telegramLinked != null && telegramLinked) {
                predicate = builder.and(predicate, builder.isNotNull(root.get("telegramUserId")));
            }
            if (telegramLinked != null && !telegramLinked) {
                predicate = builder.and(predicate, builder.isNull(root.get("telegramUserId")));
            }
            return predicate;
        };
    }
}
