package com.example.darks.repair_auto.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.localization.application.LocalizedValueResolver;
import com.example.darks.repair_auto.localization.infrastructure.EffectiveLanguageResolver;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.review.api.dto.ReviewResponse;
import com.example.darks.repair_auto.review.domain.RepairReview;
import com.example.darks.repair_auto.review.domain.ReviewSource;
import com.example.darks.repair_auto.review.infrastructure.RepairReviewRepository;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class RepairReviewServiceUnitTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private RepairReviewRepository reviewRepository;
    private RepairRequestRepository requestRepository;
    private RepairAssignmentRepository assignmentRepository;
    private CustomerRepository customerRepository;
    private EffectiveLanguageResolver effectiveLanguageResolver;
    private LocalizedValueResolver localizedValueResolver;
    private Clock clock;
    private RepairReviewService service;

    private Customer customer;
    private Technician technician;
    private User admin;
    private RepairCategory category;
    private RepairRequest request;
    private RepairAssignment completedAssignment;

    @BeforeEach
    void setUp() {
        reviewRepository = mock(RepairReviewRepository.class);
        requestRepository = mock(RepairRequestRepository.class);
        assignmentRepository = mock(RepairAssignmentRepository.class);
        customerRepository = mock(CustomerRepository.class);
        effectiveLanguageResolver = mock(EffectiveLanguageResolver.class);
        localizedValueResolver = mock(LocalizedValueResolver.class);
        clock = Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC);

        service = new RepairReviewService(
                reviewRepository,
                requestRepository,
                assignmentRepository,
                customerRepository,
                effectiveLanguageResolver,
                localizedValueResolver,
                clock);

        customer = new Customer("Ali Valiyev", "+998901234567", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 101L);
        customer.linkTelegram(123456L, 654321L, LanguageCode.UZ, NOW);

        technician = new Technician("Aziz Karimov", "+998901112233", "Cooling Master", "Notes", 5, LanguageCode.RU, true, NOW);
        ReflectionTestUtils.setField(technician, "id", 17L);

        admin = new User("Admin", "admin@example.com", "hash", UserRole.ADMIN, true, NOW);
        ReflectionTestUtils.setField(admin, "id", 1L);

        category = mock(RepairCategory.class);
        when(category.getId()).thenReturn(4L);

        request = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(request, "id", 42L);
        request.markCompleted(NOW);

        completedAssignment = new RepairAssignment(request, technician, NOW.plusDays(1), admin, NOW);
        ReflectionTestUtils.setField(completedAssignment, "id", 19L);
        completedAssignment.complete(NOW);
    }

    @Test
    void givenValidCompletedRequest_whenSubmitMobileReview_thenSavesReviewWithMobileSource() {
        when(customerRepository.findById(101L)).thenReturn(Optional.of(customer));
        when(requestRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(request));
        when(reviewRepository.existsByRepairRequestId(42L)).thenReturn(false);
        when(assignmentRepository.findLatestCompletedByRequestId(42L, AssignmentStatus.COMPLETED))
                .thenReturn(Optional.of(completedAssignment));
        when(reviewRepository.saveAndFlush(any(RepairReview.class))).thenAnswer(invocation -> {
            RepairReview saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 71L);
            return saved;
        });

        RepairReview review = service.submitReview(
                101L,
                42L,
                5,
                "Great repair job!",
                ReviewSource.MOBILE,
                LanguageCode.EN);

        assertThat(review.getId()).isEqualTo(71L);
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getComment()).isEqualTo("Great repair job!");
        assertThat(review.getSource()).isEqualTo(ReviewSource.MOBILE);
        assertThat(review.getSubmittedLanguage()).isEqualTo(LanguageCode.EN);
        assertThat(review.getCustomer().getId()).isEqualTo(101L);
        assertThat(review.getTechnician().getId()).isEqualTo(17L);
    }

    @Test
    void givenTelegramUser_whenSubmitFromTelegram_thenDelegatesToSubmitReviewWithTelegramSource() {
        when(customerRepository.findByTelegramUserId(123456L)).thenReturn(Optional.of(customer));
        when(customerRepository.findById(101L)).thenReturn(Optional.of(customer));
        when(requestRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(request));
        when(reviewRepository.existsByRepairRequestId(42L)).thenReturn(false);
        when(assignmentRepository.findLatestCompletedByRequestId(42L, AssignmentStatus.COMPLETED))
                .thenReturn(Optional.of(completedAssignment));
        when(effectiveLanguageResolver.resolveEffectiveLanguage()).thenReturn(Language.UZ);
        when(localizedValueResolver.resolve(any(), any(), any(), any())).thenReturn("Konditsioner");
        when(reviewRepository.saveAndFlush(any(RepairReview.class))).thenAnswer(invocation -> {
            RepairReview saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 72L);
            return saved;
        });

        ReviewResponse response = service.submitFromTelegram(
                123456L,
                654321L,
                42L,
                4,
                "Good",
                LanguageCode.UZ);

        assertThat(response.reviewId()).isEqualTo(72L);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.source()).isEqualTo(ReviewSource.TELEGRAM);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1, 10})
    void givenInvalidRating_whenSubmitReview_thenThrowsRatingInvalid(int rating) {
        assertThatThrownBy(() -> service.submitReview(101L, 42L, rating, "Comment", ReviewSource.MOBILE, LanguageCode.EN))
                .isInstanceOf(BusinessRuleException.class)
                .matches(e -> ((BusinessRuleException) e).getErrorCode() == ErrorCode.REVIEW_RATING_INVALID);
    }

    @Test
    void givenOversizedComment_whenSubmitReview_thenThrowsCommentTooLong() {
        String longComment = "a".repeat(1001);
        assertThatThrownBy(() -> service.submitReview(101L, 42L, 5, longComment, ReviewSource.MOBILE, LanguageCode.EN))
                .isInstanceOf(BusinessRuleException.class)
                .matches(e -> ((BusinessRuleException) e).getErrorCode() == ErrorCode.REVIEW_COMMENT_TOO_LONG);
    }

    @Test
    void givenInactiveCustomer_whenSubmitReview_thenThrowsCustomerInactive() {
        ReflectionTestUtils.setField(customer, "active", false);
        when(customerRepository.findById(101L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> service.submitReview(101L, 42L, 5, "Nice", ReviewSource.MOBILE, LanguageCode.EN))
                .isInstanceOf(BusinessRuleException.class)
                .matches(e -> ((BusinessRuleException) e).getErrorCode() == ErrorCode.REVIEW_CUSTOMER_INACTIVE);
    }

    @Test
    void givenCrossCustomerRequest_whenSubmitReview_thenThrows404RequestNotOwned() {
        Customer otherCustomer = new Customer("Other", "+998909998877", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(otherCustomer, "id", 202L);

        RepairRequest otherRequest = new RepairRequest("REQ-2026-000099", otherCustomer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(otherRequest, "id", 99L);
        otherRequest.markCompleted(NOW);

        when(customerRepository.findById(101L)).thenReturn(Optional.of(customer));
        when(requestRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(otherRequest));

        assertThatThrownBy(() -> service.submitReview(101L, 99L, 5, "Nice", ReviewSource.MOBILE, LanguageCode.EN))
                .isInstanceOf(BusinessRuleException.class)
                .matches(e -> ((BusinessRuleException) e).getErrorCode() == ErrorCode.REVIEW_REQUEST_NOT_OWNED);
    }

    @Test
    void givenNonCompletedRequest_whenSubmitReview_thenThrowsNotCompleted() {
        RepairRequest inProgressRequest = new RepairRequest("REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        ReflectionTestUtils.setField(inProgressRequest, "id", 42L);
        inProgressRequest.markInProgress(NOW);

        when(customerRepository.findById(101L)).thenReturn(Optional.of(customer));
        when(requestRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(inProgressRequest));

        assertThatThrownBy(() -> service.submitReview(101L, 42L, 5, "Nice", ReviewSource.MOBILE, LanguageCode.EN))
                .isInstanceOf(BusinessRuleException.class)
                .matches(e -> ((BusinessRuleException) e).getErrorCode() == ErrorCode.REVIEW_REQUEST_NOT_COMPLETED);
    }

    @Test
    void givenAlreadyReviewedRequest_whenSubmitReview_thenThrowsAlreadyExists() {
        when(customerRepository.findById(101L)).thenReturn(Optional.of(customer));
        when(requestRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(request));
        when(reviewRepository.existsByRepairRequestId(42L)).thenReturn(true);

        assertThatThrownBy(() -> service.submitReview(101L, 42L, 5, "Nice", ReviewSource.MOBILE, LanguageCode.EN))
                .isInstanceOf(BusinessRuleException.class)
                .matches(e -> ((BusinessRuleException) e).getErrorCode() == ErrorCode.REVIEW_ALREADY_EXISTS);
    }
}
