package com.example.darks.repair_auto.review.application;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.review.api.dto.ReviewMapper;
import com.example.darks.repair_auto.review.api.dto.ReviewResponse;
import com.example.darks.repair_auto.review.api.dto.ReviewSummaryResponse;
import com.example.darks.repair_auto.review.domain.RepairReview;
import com.example.darks.repair_auto.review.domain.ReviewSource;
import com.example.darks.repair_auto.review.infrastructure.RepairReviewRepository;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.localization.infrastructure.EffectiveLanguageResolver;
import com.example.darks.repair_auto.settings.domain.Language;

@Service
public class RepairReviewService {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    private final RepairReviewRepository reviewRepository;
    private final RepairRequestRepository requestRepository;
    private final RepairAssignmentRepository assignmentRepository;
    private final CustomerRepository customerRepository;
    private final EffectiveLanguageResolver effectiveLanguageResolver;
    private final LocalizedValueResolver localizedValueResolver;
    private final Clock clock;

    @Autowired
    public RepairReviewService(
            RepairReviewRepository reviewRepository,
            RepairRequestRepository requestRepository,
            RepairAssignmentRepository assignmentRepository,
            CustomerRepository customerRepository,
            EffectiveLanguageResolver effectiveLanguageResolver,
            LocalizedValueResolver localizedValueResolver) {
        this(reviewRepository, requestRepository, assignmentRepository, customerRepository, effectiveLanguageResolver, localizedValueResolver, Clock.systemUTC());
    }

    RepairReviewService(
            RepairReviewRepository reviewRepository,
            RepairRequestRepository requestRepository,
            RepairAssignmentRepository assignmentRepository,
            CustomerRepository customerRepository,
            EffectiveLanguageResolver effectiveLanguageResolver,
            LocalizedValueResolver localizedValueResolver,
            Clock clock) {
        this.reviewRepository = reviewRepository;
        this.requestRepository = requestRepository;
        this.assignmentRepository = assignmentRepository;
        this.customerRepository = customerRepository;
        this.effectiveLanguageResolver = effectiveLanguageResolver;
        this.localizedValueResolver = localizedValueResolver;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReviewResponse submitFromTelegram(
            Long telegramUserId,
            Long telegramChatId,
            Long repairRequestId,
            int rating,
            String comment,
            LanguageCode submittedLanguage) {
        Customer customer = customerRepository.findByTelegramUserId(telegramUserId)
                .filter(found -> found.isActive()
                        && found.getTelegramChatId() != null
                        && found.getTelegramChatId().equals(telegramChatId))
                .orElseThrow(() -> new BusinessRuleException(
                        "REVIEW_CUSTOMER_INACTIVE",
                        "Customer cannot submit reviews.",
                        403));
        RepairReview review = submitReview(
                customer.getId(),
                repairRequestId,
                rating,
                comment,
                ReviewSource.TELEGRAM,
                submittedLanguage);
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        return ReviewMapper.response(review, lang, localizedValueResolver);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RepairReview submitReview(
            Long customerId,
            Long repairRequestId,
            int rating,
            String comment,
            ReviewSource source,
            LanguageCode submittedLanguage) {
        validateRating(rating);
        String safeComment = validateComment(comment);
        Customer customer = customerRepository.findById(customerId)
                .filter(Customer::isActive)
                .orElseThrow(() -> new BusinessRuleException(
                        "REVIEW_CUSTOMER_INACTIVE",
                        "Customer cannot submit reviews.",
                        403));
        RepairRequest request = requestRepository.findByIdForUpdate(repairRequestId)
                .orElseThrow(() -> new BusinessRuleException(
                        "REVIEW_NOT_ELIGIBLE",
                        "Review request is not eligible.",
                        404));
        if (!request.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessRuleException(
                    "REVIEW_REQUEST_NOT_OWNED",
                    "Review request does not belong to this customer.",
                    404);
        }
        if (request.getStatus() != RepairRequestStatus.COMPLETED) {
            throw new BusinessRuleException(
                    "REVIEW_REQUEST_NOT_COMPLETED",
                    "Only completed repair requests can be reviewed.",
                    409);
        }
        if (reviewRepository.existsByRepairRequestId(request.getId())) {
            throw alreadyExists();
        }
        RepairAssignment completedAssignment = assignmentRepository
                .findLatestCompletedByRequestId(request.getId(), AssignmentStatus.COMPLETED)
                .orElseThrow(() -> new BusinessRuleException(
                        "REVIEW_TECHNICIAN_NOT_RESOLVED",
                        "Completed technician assignment could not be resolved.",
                        409));
        try {
            return reviewRepository.saveAndFlush(new RepairReview(
                    request,
                    customer,
                    completedAssignment.getTechnician(),
                    rating,
                    safeComment,
                    source == null ? ReviewSource.MOBILE : source,
                    submittedLanguage == null ? LanguageCode.UZ : submittedLanguage,
                    now()));
        } catch (DataIntegrityViolationException exception) {
            throw alreadyExists();
        }
    }

    @Transactional(readOnly = true)
    public List<EligibleReviewRequest> eligibleRequests(Long customerId, Pageable pageable) {
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        return requestRepository.findCompletedUnreviewedForCustomer(customerId, pageable).stream()
                .map(request -> {
                    var cat = request.getCategory();
                    String name = localizedValueResolver.resolve(lang, cat.getNameUz(), cat.getNameRu(), cat.getNameEn());
                    return new EligibleReviewRequest(
                            request.getId(),
                            request.getRequestNumber(),
                            name,
                            cat.getNameEn(),
                            cat.getNameRu(),
                            cat.getNameUz());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean canReview(Long customerId, Long requestId) {
        return requestRepository.findWithRelationsById(requestId)
                .filter(request -> request.getCustomer().getId().equals(customerId))
                .filter(request -> request.getStatus() == RepairRequestStatus.COMPLETED)
                .filter(request -> !reviewRepository.existsByRepairRequestId(request.getId()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public CustomerReviewSummary customerReview(Long customerId, Long requestId) {
        return reviewRepository.findByRequestIdAndCustomerId(requestId, customerId)
                .map(review -> new CustomerReviewSummary(
                        review.getId(),
                        review.getRating(),
                        review.getComment()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> list(ReviewQuery query, Pageable pageable) {
        validateQuery(query);
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        return PageResponse.from(reviewRepository.findAll(filters(query), pageable)
                .map(review -> ReviewMapper.response(review, lang, localizedValueResolver)));
    }

    @Transactional(readOnly = true)
    public ReviewResponse get(Long id) {
        Language lang = effectiveLanguageResolver.resolveEffectiveLanguage();
        return reviewRepository.findDetailsById(id)
                .map(review -> ReviewMapper.response(review, lang, localizedValueResolver))
                .orElseThrow(() -> new BusinessRuleException("REVIEW_NOT_FOUND", "Review was not found.", 404));
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse summary(ReviewQuery query) {
        validateQuery(query);
        List<RepairReview> reviews = reviewRepository.findAll(filters(query));
        long total = reviews.size();
        long rating1 = countRating(reviews, 1);
        long rating2 = countRating(reviews, 2);
        long rating3 = countRating(reviews, 3);
        long rating4 = countRating(reviews, 4);
        long rating5 = countRating(reviews, 5);
        long withComment = reviews.stream().filter(review -> review.getComment() != null).count();
        BigDecimal average = null;
        if (total > 0) {
            int sum = reviews.stream().mapToInt(RepairReview::getRating).sum();
            average = BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        }
        return new ReviewSummaryResponse(total, average, rating1, rating2, rating3, rating4, rating5, withComment);
    }

    private Specification<RepairReview> filters(ReviewQuery query) {
        return (root, criteriaQuery, builder) -> {
            Class<?> resultType = criteriaQuery.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                root.fetch("repairRequest", JoinType.INNER).fetch("category", JoinType.INNER);
                root.fetch("customer", JoinType.INNER);
                root.fetch("technician", JoinType.INNER);
                criteriaQuery.distinct(true);
            }
            var predicate = builder.conjunction();
            if (query.rating() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("rating"), query.rating()));
            }
            if (query.technicianId() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("technician").get("id"), query.technicianId()));
            }
            if (query.customerId() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("customer").get("id"), query.customerId()));
            }
            if (query.categoryId() != null) {
                predicate = builder.and(
                        predicate,
                        builder.equal(root.get("repairRequest").get("category").get("id"), query.categoryId()));
            }
            if (query.repairRequestId() != null) {
                predicate = builder.and(
                        predicate,
                        builder.equal(root.get("repairRequest").get("id"), query.repairRequestId()));
            }
            if (query.requestNumber() != null && !query.requestNumber().isBlank()) {
                predicate = builder.and(
                        predicate,
                        builder.like(
                                builder.lower(root.get("repairRequest").get("requestNumber")),
                                "%" + query.requestNumber().trim().toLowerCase(java.util.Locale.ROOT) + "%"));
            }
            if (query.hasComment() != null) {
                if (query.hasComment()) {
                    predicate = builder.and(predicate, builder.isNotNull(root.get("comment")));
                } else {
                    predicate = builder.and(predicate, builder.isNull(root.get("comment")));
                }
            }
            if (query.submittedFrom() != null) {
                predicate = builder.and(
                        predicate,
                        builder.greaterThanOrEqualTo(root.get("submittedAt"), query.submittedFrom()));
            }
            if (query.submittedTo() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("submittedAt"), query.submittedTo()));
            }
            return predicate;
        };
    }

    private void validateQuery(ReviewQuery query) {
        if (query.rating() != null && (query.rating() < MIN_RATING || query.rating() > MAX_RATING)) {
            throw new BusinessRuleException("REVIEW_RATING_INVALID", "Rating must be between 1 and 5.", 400);
        }
        if (query.submittedFrom() != null && query.submittedTo() != null
                && query.submittedFrom().isAfter(query.submittedTo())) {
            throw new BusinessRuleException("INVALID_REVIEW_DATE_RANGE", "submittedFrom must be before submittedTo.", 400);
        }
    }

    private void validateRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new BusinessRuleException("REVIEW_RATING_INVALID", "Rating must be between 1 and 5.", 400);
        }
    }

    private String validateComment(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > RepairReview.MAX_COMMENT_LENGTH) {
            throw new BusinessRuleException("REVIEW_COMMENT_TOO_LONG", "Review comment is too long.", 400);
        }
        return trimmed;
    }

    private long countRating(List<RepairReview> reviews, int rating) {
        return reviews.stream().filter(review -> review.getRating() == rating).count();
    }

    private BusinessRuleException alreadyExists() {
        return new BusinessRuleException(
                "REVIEW_ALREADY_EXISTS",
                "This repair request has already been reviewed.",
                409);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
