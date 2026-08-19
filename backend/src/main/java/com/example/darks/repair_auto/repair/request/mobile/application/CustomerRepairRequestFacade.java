package com.example.darks.repair_auto.repair.request.mobile.application;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.access.application.RepairResourceAccessPolicy;
import com.example.darks.repair_auto.repair.action.application.RepairActionCapabilityService;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.execution.domain.RepairRequestStatusHistory;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairRequestStatusHistoryRepository;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestDetailResponse;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestSummaryResponse;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestTimelineItemResponse;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerReviewCreateRequest;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerReviewResponse;
import com.example.darks.repair_auto.review.application.RepairReviewService;
import com.example.darks.repair_auto.review.domain.RepairReview;
import com.example.darks.repair_auto.review.domain.ReviewSource;
import com.example.darks.repair_auto.review.infrastructure.RepairReviewRepository;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.i18n.RequestLocaleResolver;
import com.example.darks.repair_auto.shared.i18n.SupportedLanguage;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.example.darks.repair_auto.technician.domain.Technician;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerRepairRequestFacade {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 120;

    private final RepairRequestService repairRequestService;
    private final RepairResourceAccessPolicy accessPolicy;
    private final RepairRequestRepository repairRequestRepository;
    private final RepairAssignmentRepository repairAssignmentRepository;
    private final RepairRequestStatusHistoryRepository statusHistoryRepository;
    private final RepairActionCapabilityService actionCapabilityService;
    private final RepairReviewService repairReviewService;
    private final RepairReviewRepository repairReviewRepository;
    private final RequestLocaleResolver requestLocaleResolver;
    private final LocalizationService localizationService;

    public CustomerRepairRequestFacade(
            RepairRequestService repairRequestService,
            RepairResourceAccessPolicy accessPolicy,
            RepairRequestRepository repairRequestRepository,
            RepairAssignmentRepository repairAssignmentRepository,
            RepairRequestStatusHistoryRepository statusHistoryRepository,
            RepairActionCapabilityService actionCapabilityService,
            RepairReviewService repairReviewService,
            RepairReviewRepository repairReviewRepository,
            RequestLocaleResolver requestLocaleResolver,
            LocalizationService localizationService) {
        this.repairRequestService = repairRequestService;
        this.accessPolicy = accessPolicy;
        this.repairRequestRepository = repairRequestRepository;
        this.repairAssignmentRepository = repairAssignmentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.actionCapabilityService = actionCapabilityService;
        this.repairReviewService = repairReviewService;
        this.repairReviewRepository = repairReviewRepository;
        this.requestLocaleResolver = requestLocaleResolver;
        this.localizationService = localizationService;
    }

    @Transactional
    public CustomerRepairRequestDetailResponse createRequest(
            AuthenticatedMobileActor actor,
            String idempotencyKey,
            CustomerRepairRequestCreateRequest request) {
        requireCustomer(actor);

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        String trimmedKey = idempotencyKey.trim();
        if (trimmedKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        String sourceReference = "mobile:customer:%d:%s".formatted(actor.actorId(), trimmedKey);
        RepairRequest created = repairRequestService.mobileCreate(
                actor.actorId(),
                request.categoryId(),
                request.description(),
                request.resolvedLocation(),
                sourceReference);

        RepairAssignment assignment = repairAssignmentRepository
                .findActiveByRequestId(created.getId(), RepairResourceAccessPolicy.ACTIVE_ASSIGNMENT_STATUSES)
                .orElse(null);

        return toDetailResponse(created, assignment);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerRepairRequestSummaryResponse> listRequests(
            AuthenticatedMobileActor actor,
            RepairRequestStatus status,
            Long categoryId,
            Pageable pageable) {
        requireCustomer(actor);

        Specification<RepairRequest> spec = (root, query, cb) -> {
            var predicate = cb.and(
                    cb.equal(root.get("customer").get("id"), actor.actorId()),
                    cb.isNull(root.get("deletedAt")));
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category").get("id"), categoryId));
            }
            return predicate;
        };

        Page<RepairRequest> page = repairRequestRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(this::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    public CustomerRepairRequestDetailResponse getRequestDetail(
            AuthenticatedMobileActor actor,
            Long requestId) {
        requireCustomer(actor);
        accessPolicy.requireCurrentCustomerCanReadRequest(actor, requestId);

        RepairRequest request = repairRequestRepository.findWithRelationsById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPAIR_REQUEST_NOT_FOUND));

        RepairAssignment activeAssignment = repairAssignmentRepository
                .findActiveByRequestId(requestId, RepairResourceAccessPolicy.ACTIVE_ASSIGNMENT_STATUSES)
                .orElse(null);

        return toDetailResponse(request, activeAssignment);
    }

    @Transactional(readOnly = true)
    public List<CustomerRepairRequestTimelineItemResponse> getRequestTimeline(
            AuthenticatedMobileActor actor,
            Long requestId) {
        requireCustomer(actor);
        accessPolicy.requireCurrentCustomerCanReadRequest(actor, requestId);

        SupportedLanguage language = requestLocaleResolver.resolveLanguage();
        List<RepairRequestStatusHistory> history = statusHistoryRepository
                .findByRepairRequestIdOrderByChangedAtAscIdAsc(requestId);

        return history.stream()
                .map(h -> new CustomerRepairRequestTimelineItemResponse(
                        h.getToStatus(),
                        localizeStatus(h.getToStatus(), language),
                        h.getChangedAt()))
                .toList();
    }

    public CustomerReviewResponse submitReview(
            AuthenticatedMobileActor actor,
            Long requestId,
            CustomerReviewCreateRequest request) {
        requireCustomer(actor);
        SupportedLanguage language = requestLocaleResolver.resolveLanguage();
        LanguageCode languageCode = switch (language) {
            case EN -> LanguageCode.EN;
            case RU -> LanguageCode.RU;
            case UZ -> LanguageCode.UZ;
        };
        RepairReview review = repairReviewService.submitReview(
                actor.actorId(),
                requestId,
                request.rating(),
                request.comment(),
                ReviewSource.MOBILE,
                languageCode);
        return new CustomerReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getSubmittedAt());
    }

    private CustomerRepairRequestSummaryResponse toSummaryResponse(RepairRequest request) {
        SupportedLanguage language = requestLocaleResolver.resolveLanguage();
        RepairAssignment activeAssignment = repairAssignmentRepository
                .findActiveByRequestId(request.getId(), RepairResourceAccessPolicy.ACTIVE_ASSIGNMENT_STATUSES)
                .orElse(null);

        OffsetDateTime scheduledVisitAt = activeAssignment != null ? activeAssignment.getScheduledVisitAt() : null;

        return new CustomerRepairRequestSummaryResponse(
                request.getId(),
                request.getRequestNumber(),
                request.getStatus(),
                localizeStatus(request.getStatus(), language),
                new CustomerRepairRequestSummaryResponse.CategorySummary(
                        request.getCategory().getId(),
                        localizeCategory(request.getCategory(), language)),
                request.getDescription(),
                scheduledVisitAt,
                request.getCreatedAt());
    }

    private CustomerRepairRequestDetailResponse toDetailResponse(
            RepairRequest request,
            RepairAssignment assignment) {
        SupportedLanguage language = requestLocaleResolver.resolveLanguage();

        CustomerRepairRequestDetailResponse.TechnicianSummary technicianSummary = null;
        CustomerRepairRequestDetailResponse.ScheduleInfo scheduleInfo = null;

        if (assignment != null) {
            Technician tech = assignment.getTechnician();
            technicianSummary = new CustomerRepairRequestDetailResponse.TechnicianSummary(
                    tech.getId(),
                    tech.getFullName(),
                    tech.getPhone(),
                    tech.getSpecialization());

            if (assignment.getScheduledVisitAt() != null) {
                scheduleInfo = new CustomerRepairRequestDetailResponse.ScheduleInfo(assignment.getScheduledVisitAt());
            }
        }

        CustomerRepairRequestDetailResponse.ReviewInfo reviewInfo = repairReviewRepository
                .findByRepairRequestId(request.getId())
                .map(r -> new CustomerRepairRequestDetailResponse.ReviewInfo(
                        r.getId(),
                        r.getRating(),
                        r.getComment(),
                        r.getSubmittedAt()))
                .orElse(null);

        return new CustomerRepairRequestDetailResponse(
                request.getId(),
                request.getRequestNumber(),
                request.getStatus(),
                localizeStatus(request.getStatus(), language),
                new CustomerRepairRequestDetailResponse.CategorySummary(
                        request.getCategory().getId(),
                        localizeCategory(request.getCategory(), language)),
                request.getDescription(),
                (request.getLocationAddress() != null || request.getLocationLatitude() != null || request.getLocationLongitude() != null || request.getLocationSource() != null)
                        ? new CustomerRepairRequestDetailResponse.LocationInfo(
                                request.getLocationAddress(),
                                request.getLocationLatitude(),
                                request.getLocationLongitude(),
                                request.getLocationSource())
                        : null,
                technicianSummary,
                scheduleInfo,
                reviewInfo,
                actionCapabilityService.resolveCustomerActions(request),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    private String localizeCategory(RepairCategory category, SupportedLanguage language) {
        if (category == null) {
            return "";
        }
        return switch (language) {
            case EN -> category.getNameEn();
            case RU -> category.getNameRu();
            case UZ -> category.getNameUz();
        };
    }

    private String localizeStatus(RepairRequestStatus status, SupportedLanguage language) {
        if (status == null) {
            return "";
        }
        String key = "repair.status." + status.name().toLowerCase(Locale.ROOT).replace('_', '-');
        return localizationService.get(key, language);
    }

    private void requireCustomer(AuthenticatedMobileActor actor) {
        if (actor == null || !actor.isCustomer()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!actor.active()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
        }
    }
}
