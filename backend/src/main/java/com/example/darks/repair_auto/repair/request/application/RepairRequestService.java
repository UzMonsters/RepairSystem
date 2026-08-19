package com.example.darks.repair_auto.repair.request.application;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.notification.application.NotificationEventFactory;
import com.example.darks.repair_auto.notification.application.NotificationOutboxService;
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.execution.application.RepairStatusHistoryService;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairExecutionRepository;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCreateResponse;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestDetailResponse;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestMapper;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestSummaryResponse;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestUpdateRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestNumberGenerator;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.error.InvalidRequestParameterException;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.example.darks.repair_auto.shared.phone.PhoneNumberNormalizer;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.localization.infrastructure.EffectiveLanguageResolver;
import com.example.darks.repair_auto.settings.domain.Language;

@Service
public class RepairRequestService {

    private static final int MIN_DESCRIPTION_LENGTH = 10;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final int MAX_ADDRESS_LENGTH = 500;
    private static final int MAX_INTERNAL_NOTE_LENGTH = 2000;
    private static final int MAX_SEARCH_LENGTH = 120;

    private final RepairRequestRepository repairRequestRepository;
    private final CustomerRepository customerRepository;
    private final RepairCategoryRepository repairCategoryRepository;
    private final UserRepository userRepository;
    private final RepairAssignmentRepository repairAssignmentRepository;
    private final RepairExecutionRepository repairExecutionRepository;
    private final RepairStatusHistoryService statusHistoryService;
    private final RepairRequestNumberGenerator requestNumberGenerator;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final NotificationEventFactory notificationEventFactory;
    private final NotificationOutboxService notificationOutboxService;
    private final EffectiveLanguageResolver effectiveLanguageResolver;
    private final LocalizedValueResolver localizedValueResolver;
    private final Clock clock;

    @Autowired
    public RepairRequestService(
            RepairRequestRepository repairRequestRepository,
            CustomerRepository customerRepository,
            RepairCategoryRepository repairCategoryRepository,
            UserRepository userRepository,
            RepairAssignmentRepository repairAssignmentRepository,
            RepairExecutionRepository repairExecutionRepository,
            RepairStatusHistoryService statusHistoryService,
            RepairRequestNumberGenerator requestNumberGenerator,
            PhoneNumberNormalizer phoneNumberNormalizer,
            NotificationEventFactory notificationEventFactory,
            NotificationOutboxService notificationOutboxService,
            EffectiveLanguageResolver effectiveLanguageResolver,
            LocalizedValueResolver localizedValueResolver) {
        this(
                repairRequestRepository,
                customerRepository,
                repairCategoryRepository,
                userRepository,
                repairAssignmentRepository,
                repairExecutionRepository,
                statusHistoryService,
                requestNumberGenerator,
                phoneNumberNormalizer,
                notificationEventFactory,
                notificationOutboxService,
                effectiveLanguageResolver,
                localizedValueResolver,
                Clock.systemUTC());
    }

    RepairRequestService(
            RepairRequestRepository repairRequestRepository,
            CustomerRepository customerRepository,
            RepairCategoryRepository repairCategoryRepository,
            UserRepository userRepository,
            RepairAssignmentRepository repairAssignmentRepository,
            RepairExecutionRepository repairExecutionRepository,
            RepairStatusHistoryService statusHistoryService,
            RepairRequestNumberGenerator requestNumberGenerator,
            PhoneNumberNormalizer phoneNumberNormalizer,
            NotificationEventFactory notificationEventFactory,
            NotificationOutboxService notificationOutboxService,
            EffectiveLanguageResolver effectiveLanguageResolver,
            LocalizedValueResolver localizedValueResolver,
            Clock clock) {
        this.repairRequestRepository = repairRequestRepository;
        this.customerRepository = customerRepository;
        this.repairCategoryRepository = repairCategoryRepository;
        this.userRepository = userRepository;
        this.repairAssignmentRepository = repairAssignmentRepository;
        this.repairExecutionRepository = repairExecutionRepository;
        this.statusHistoryService = statusHistoryService;
        this.requestNumberGenerator = requestNumberGenerator;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.notificationEventFactory = notificationEventFactory;
        this.notificationOutboxService = notificationOutboxService;
        this.effectiveLanguageResolver = effectiveLanguageResolver;
        this.localizedValueResolver = localizedValueResolver;
        this.clock = clock;
    }

    @Transactional
    public RepairRequestCreateResponse create(RepairRequestCreateRequest request, AuthenticatedUser creator) {
        OffsetDateTime now = now();
        Customer customer = activeCustomerForUpdate(request.customerId());
        RepairCategory category = activeCategoryForUpdate(request.categoryId());
        User createdBy = userRepository.findById(creator.id()).orElseThrow(this::creatorNotFound);
        RepairRequest repairRequest = new RepairRequest(
                requestNumberGenerator.nextRequestNumber(),
                customer,
                category,
                validateDescription(request.description()),
                validateAddress(request.address()),
                validateLatitude(request.latitude()),
                validateLongitude(request.longitude()),
                request.priority() == null ? RepairRequestPriority.NORMAL : request.priority(),
                validatePreferredVisit(request.customerPreferredVisitAt(), now),
                validateInternalNote(request.internalNote()),
                createdBy,
                now);
        validateLocation(repairRequest.getAddress(), repairRequest.getLatitude(), repairRequest.getLongitude());
        RepairRequest saved = repairRequestRepository.saveAndFlush(repairRequest);
        var history = statusHistoryService.recordInitial(saved, "Request created.", createdBy, now);
        notificationOutboxService.enqueue(notificationEventFactory.customer(
                NotificationType.REQUEST_CREATED,
                saved,
                NotificationEventFactory.statusEventKeyPart(history.getId(), saved.getStatus())));
        return RepairRequestMapper.created(saved);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RepairRequest telegramCreate(
            Long customerId,
            Long categoryId,
            String description,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String sourceReference) {
        String reference = validateSourceReference(sourceReference);
        Optional<RepairRequest> existing = repairRequestRepository.findBySourceReference(reference);
        if (existing.isPresent()) {
            return existing.get();
        }
        OffsetDateTime now = now();
        Customer customer = activeCustomerForUpdate(customerId);
        RepairCategory category = activeCategoryForUpdate(categoryId);
        String validAddress = validateAddress(address);
        BigDecimal validLatitude = validateLatitude(latitude);
        BigDecimal validLongitude = validateLongitude(longitude);
        validateLocation(validAddress, validLatitude, validLongitude);
        RepairRequest repairRequest = RepairRequest.telegram(
                requestNumberGenerator.nextRequestNumber(),
                customer,
                category,
                validateDescription(description),
                validAddress,
                validLatitude,
                validLongitude,
                RepairRequestPriority.NORMAL,
                null,
                reference,
                now);
        try {
            RepairRequest saved = repairRequestRepository.saveAndFlush(repairRequest);
            statusHistoryService.recordInitial(saved, "Telegram request created.", null, now);
            return saved;
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            return repairRequestRepository.findBySourceReference(reference)
                    .orElseThrow(() -> new BusinessRuleException(
                            "TELEGRAM_REQUEST_SUBMISSION_CONFLICT",
                            "Repair request submission conflicted. Please try again.",
                            409));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RepairRequest mobileCreate(
            Long customerId,
            Long categoryId,
            String description,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String sourceReference) {
        String reference = validateSourceReference(sourceReference);
        Optional<RepairRequest> existing = repairRequestRepository.findBySourceReference(reference);
        if (existing.isPresent()) {
            RepairRequest existingRequest = existing.get();
            if (!existingRequest.getCustomer().getId().equals(customerId)) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
            return existingRequest;
        }
        OffsetDateTime now = now();
        Customer customer = activeCustomerForUpdate(customerId);
        RepairCategory category = activeCategoryForUpdate(categoryId);
        String validAddress = validateAddress(address);
        BigDecimal validLatitude = validateLatitude(latitude);
        BigDecimal validLongitude = validateLongitude(longitude);
        validateLocation(validAddress, validLatitude, validLongitude);
        RepairRequest repairRequest = RepairRequest.mobile(
                requestNumberGenerator.nextRequestNumber(),
                customer,
                category,
                validateDescription(description),
                validAddress,
                validLatitude,
                validLongitude,
                RepairRequestPriority.NORMAL,
                null,
                reference,
                now);
        try {
            RepairRequest saved = repairRequestRepository.saveAndFlush(repairRequest);
            var history = statusHistoryService.recordInitial(saved, "Mobile request created.", null, now);
            notificationOutboxService.enqueue(notificationEventFactory.customer(
                    NotificationType.REQUEST_CREATED,
                    saved,
                    NotificationEventFactory.statusEventKeyPart(history.getId(), saved.getStatus())));
            return saved;
        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            return repairRequestRepository.findBySourceReference(reference)
                    .orElseThrow(() -> new BusinessRuleException(
                            "REPAIR_REQUEST_SUBMISSION_CONFLICT",
                            "Repair request submission conflicted. Please try again.",
                            409));
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<RepairRequestSummaryResponse> list(
            RepairRequestQuery query,
            Pageable pageable) {
        validateDateRange(query.createdFrom(), query.createdTo(), "created");
        validateDateRange(query.preferredVisitFrom(), query.preferredVisitTo(), "preferredVisit");
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        return PageResponse.from(repairRequestRepository.findAll(filters(query, null), pageable)
                .map(request -> RepairRequestMapper.summary(request, lang, localizedValueResolver)));
    }

    @Transactional(readOnly = true)
    public RepairRequestDetailResponse get(Long id) {
        RepairRequest repairRequest = repairRequestRepository.findWithRelationsById(id).orElseThrow(this::notFound);
        var currentAssignment = repairAssignmentRepository
                .findActiveByRequestId(id, RepairAssignmentRepository.ACTIVE_STATUSES)
                .orElse(null);
        var execution = repairExecutionRepository.findByRepairRequestId(id).orElse(null);
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        return RepairRequestMapper.details(repairRequest, currentAssignment, execution, lang, localizedValueResolver);
    }

    @Transactional
    public RepairRequestDetailResponse update(Long id, RepairRequestUpdateRequest request) {
        RepairRequest repairRequest = repairRequestRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
        if (repairRequest.getStatus() != RepairRequestStatus.NEW) {
            throw new BusinessRuleException(
                    "REPAIR_REQUEST_NOT_EDITABLE",
                    "Repair request intake can be updated only while the request is NEW.",
                    409);
        }
        OffsetDateTime now = now();
        Customer customer = activeCustomerForUpdate(request.customerId());
        RepairCategory category = activeCategoryForUpdate(request.categoryId());
        String address = validateAddress(request.address());
        BigDecimal latitude = validateLatitude(request.latitude());
        BigDecimal longitude = validateLongitude(request.longitude());
        validateLocation(address, latitude, longitude);
        repairRequest.updateIntake(
                customer,
                category,
                validateDescription(request.description()),
                address,
                latitude,
                longitude,
                request.priority(),
                validatePreferredVisit(request.customerPreferredVisitAt(), now),
                validateInternalNote(request.internalNote()),
                now);
        var execution = repairExecutionRepository.findByRepairRequestId(id).orElse(null);
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        return RepairRequestMapper.details(repairRequestRepository.saveAndFlush(repairRequest), null, execution, lang, localizedValueResolver);
    }

    @Transactional(readOnly = true)
    public PageResponse<RepairRequestSummaryResponse> customerHistory(
            Long customerId,
            RepairRequestQuery query,
            Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            throw customerNotFound();
        }
        validateDateRange(query.createdFrom(), query.createdTo(), "created");
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        return PageResponse.from(repairRequestRepository.findAll(filters(query, customerId), pageable)
                .map(request -> RepairRequestMapper.summary(request, lang, localizedValueResolver)));
    }

    private Specification<RepairRequest> filters(RepairRequestQuery query, Long forcedCustomerId) {
        return (root, criteriaQuery, builder) -> {
            var predicate = builder.conjunction();
            var customer = root.join("customer");
            var category = root.join("category");
            String search = blankToNull(query.search());
            if (search != null) {
                if (search.length() > MAX_SEARCH_LENGTH) {
                    throw new InvalidRequestParameterException("search", "Search must be at most 120 characters.");
                }
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                String normalizedSearchPhone = normalizeSearchPhone(search);
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("requestNumber")), pattern),
                        builder.like(builder.lower(customer.get("fullName")), pattern),
                        builder.like(customer.get("phone"), "%" + search + "%"),
                        normalizedSearchPhone == null
                                ? builder.disjunction()
                                : builder.equal(customer.get("phone"), normalizedSearchPhone),
                        builder.like(builder.lower(root.get("description")), pattern),
                        builder.like(builder.lower(root.get("address")), pattern),
                        builder.like(builder.lower(category.get("nameEn")), pattern),
                        builder.like(builder.lower(category.get("nameRu")), pattern),
                        builder.like(builder.lower(category.get("nameUz")), pattern)));
            }
            if (blankToNull(query.requestNumber()) != null) {
                predicate = builder.and(predicate, builder.equal(
                        root.get("requestNumber"),
                        query.requestNumber().trim()));
            }
            Long customerId = forcedCustomerId == null ? query.customerId() : forcedCustomerId;
            if (customerId != null) {
                predicate = builder.and(predicate, builder.equal(customer.get("id"), customerId));
            }
            if (query.categoryId() != null) {
                predicate = builder.and(predicate, builder.equal(category.get("id"), query.categoryId()));
            }
            if (query.status() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), query.status()));
            }
            if (query.priority() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("priority"), query.priority()));
            }
            if (query.source() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("source"), query.source()));
            }
            if (query.createdFrom() != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("createdAt"), query.createdFrom()));
            }
            if (query.createdTo() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("createdAt"), query.createdTo()));
            }
            if (query.preferredVisitFrom() != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(
                        root.get("customerPreferredVisitAt"),
                        query.preferredVisitFrom()));
            }
            if (query.preferredVisitTo() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(
                        root.get("customerPreferredVisitAt"),
                        query.preferredVisitTo()));
            }
            return predicate;
        };
    }

    private Customer activeCustomerForUpdate(Long id) {
        Customer customer = customerRepository.findByIdForUpdate(id).orElseThrow(this::customerNotFound);
        if (!customer.isActive()) {
            throw new BusinessRuleException(
                    "REPAIR_REQUEST_CUSTOMER_INACTIVE",
                    "Inactive customer cannot be selected for a new repair request.",
                    409);
        }
        return customer;
    }

    private RepairCategory activeCategoryForUpdate(Long id) {
        RepairCategory category = repairCategoryRepository.findByIdForUpdate(id).orElseThrow(this::categoryNotFound);
        if (!category.isActive()) {
            throw new BusinessRuleException(
                    "REPAIR_REQUEST_CATEGORY_INACTIVE",
                    "Inactive repair category cannot be selected for a new repair request.",
                    409);
        }
        return category;
    }

    private String validateDescription(String value) {
        String description = blankToNull(value);
        if (description == null
                || description.length() < MIN_DESCRIPTION_LENGTH
                || description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessRuleException(
                    "INVALID_REPAIR_REQUEST_DESCRIPTION",
                    "Description must be between 10 and 2000 characters.",
                    400);
        }
        return description;
    }

    private String validateAddress(String value) {
        String address = blankToNull(value);
        if (address != null && address.length() > MAX_ADDRESS_LENGTH) {
            throw invalidLocation("Address must be at most 500 characters.");
        }
        return address;
    }

    private BigDecimal validateLatitude(BigDecimal value) {
        if (value != null && (value.compareTo(BigDecimal.valueOf(-90)) < 0
                || value.compareTo(BigDecimal.valueOf(90)) > 0)) {
            throw invalidLocation("Latitude must be between -90 and 90.");
        }
        return value;
    }

    private BigDecimal validateLongitude(BigDecimal value) {
        if (value != null && (value.compareTo(BigDecimal.valueOf(-180)) < 0
                || value.compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw invalidLocation("Longitude must be between -180 and 180.");
        }
        return value;
    }

    private void validateLocation(String address, BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw invalidLocation("Latitude and longitude must be provided together.");
        }
        if (address == null && latitude == null) {
            throw invalidLocation("Address or a latitude/longitude pair is required.");
        }
    }

    private OffsetDateTime validatePreferredVisit(OffsetDateTime value, OffsetDateTime now) {
        if (value != null && value.isBefore(now.minusMinutes(5))) {
            throw new BusinessRuleException(
                    "INVALID_PREFERRED_VISIT_TIME",
                    "Preferred visit time must not be in the past.",
                    400);
        }
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC);
    }

    private String validateInternalNote(String value) {
        String note = blankToNull(value);
        if (note != null && note.length() > MAX_INTERNAL_NOTE_LENGTH) {
            throw new BusinessRuleException(
                    "VALIDATION_FAILED",
                    "Internal note must be at most 2000 characters.",
                    400);
        }
        return note;
    }

    private String validateSourceReference(String value) {
        String reference = blankToNull(value);
        if (reference == null || reference.length() > 120) {
            throw new BusinessRuleException(
                    "TELEGRAM_REQUEST_SUBMISSION_CONFLICT",
                    "Repair request submission is invalid.",
                    409);
        }
        return reference;
    }

    private void validateDateRange(OffsetDateTime from, OffsetDateTime to, String fieldPrefix) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException(
                    "INVALID_REQUEST_DATE_RANGE",
                    fieldPrefix + "From must be before or equal to " + fieldPrefix + "To.",
                    400);
        }
    }

    private String normalizeSearchPhone(String search) {
        try {
            return phoneNumberNormalizer.normalize(search);
        } catch (BusinessRuleException exception) {
            return null;
        }
    }

    private BusinessRuleException invalidLocation(String message) {
        return new BusinessRuleException("INVALID_REPAIR_REQUEST_LOCATION", message, 400);
    }

    private BusinessRuleException notFound() {
        return new BusinessRuleException("REPAIR_REQUEST_NOT_FOUND", "Repair request was not found.", 404);
    }

    private BusinessRuleException customerNotFound() {
        return new BusinessRuleException("CUSTOMER_NOT_FOUND", "Customer was not found.", 404);
    }

    private BusinessRuleException categoryNotFound() {
        return new BusinessRuleException("CATEGORY_NOT_FOUND", "Repair category was not found.", 404);
    }

    private BusinessRuleException creatorNotFound() {
        return new BusinessRuleException("USER_NOT_FOUND", "Authenticated user was not found.", 404);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
