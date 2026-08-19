package com.example.darks.repair_auto.repair.request.mobile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.access.application.RepairResourceAccessPolicy;
import com.example.darks.repair_auto.repair.action.application.RepairActionCapabilityService;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.execution.domain.RepairRequestStatusHistory;
import com.example.darks.repair_auto.repair.execution.infrastructure.RepairRequestStatusHistoryRepository;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

class CustomerRepairRequestFacadeTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private RepairRequestService repairRequestService;
    private RepairResourceAccessPolicy accessPolicy;
    private RepairRequestRepository repairRequestRepository;
    private RepairAssignmentRepository repairAssignmentRepository;
    private RepairRequestStatusHistoryRepository statusHistoryRepository;
    private RepairActionCapabilityService actionCapabilityService;
    private RepairReviewService repairReviewService;
    private RepairReviewRepository repairReviewRepository;
    private RequestLocaleResolver requestLocaleResolver;
    private LocalizationService localizationService;
    private CustomerRepairRequestFacade facade;

    private Customer customer;
    private RepairCategory category;
    private User admin;
    private Technician technician;

    @BeforeEach
    void setUp() {
        repairRequestService = mock(RepairRequestService.class);
        accessPolicy = mock(RepairResourceAccessPolicy.class);
        repairRequestRepository = mock(RepairRequestRepository.class);
        repairAssignmentRepository = mock(RepairAssignmentRepository.class);
        statusHistoryRepository = mock(RepairRequestStatusHistoryRepository.class);
        actionCapabilityService = mock(RepairActionCapabilityService.class);
        repairReviewService = mock(RepairReviewService.class);
        repairReviewRepository = mock(RepairReviewRepository.class);
        requestLocaleResolver = mock(RequestLocaleResolver.class);
        localizationService = mock(LocalizationService.class);

        facade = new CustomerRepairRequestFacade(
                repairRequestService,
                accessPolicy,
                repairRequestRepository,
                repairAssignmentRepository,
                statusHistoryRepository,
                actionCapabilityService,
                repairReviewService,
                repairReviewRepository,
                requestLocaleResolver,
                localizationService);

        customer = new Customer("Customer Ali", "+998901234567", LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(customer, "id", 42L);

        category = mock(RepairCategory.class);
        when(category.getId()).thenReturn(4L);
        when(category.getNameUz()).thenReturn("Konditsioner");
        when(category.getNameRu()).thenReturn("Кондиционер");
        when(category.getNameEn()).thenReturn("Air Conditioner");

        admin = new User("Admin", "admin@example.com", "hash", UserRole.ADMIN, true, NOW);
        ReflectionTestUtils.setField(admin, "id", 1L);

        technician = new Technician("Aziz Karimov", "+998901112233", "Cooling Master", "Notes", 5, LanguageCode.RU, true, NOW);
        ReflectionTestUtils.setField(technician, "id", 17L);

        when(requestLocaleResolver.resolveLanguage()).thenReturn(SupportedLanguage.UZ);
        when(localizationService.get(any(), any())).thenReturn("Localized Label");
        when(actionCapabilityService.resolveCustomerActions(any())).thenReturn(List.of());
    }

    @Test
    void givenCustomerActor_whenCreateRequestWithIdempotencyKey_thenDelegatesToServiceAndReturnsDetail() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        CustomerRepairRequestCreateRequest request = new CustomerRepairRequestCreateRequest(
                4L,
                "Air conditioner is making noise",
                null,
                "Chilanzar 9, Tashkent",
                new BigDecimal("41.275412"),
                new BigDecimal("69.204511"));

        RepairRequest saved = new RepairRequest(
                "REQ-2026-000042",
                customer,
                category,
                "Air conditioner is making noise",
                "Chilanzar 9, Tashkent",
                new BigDecimal("41.275412"),
                new BigDecimal("69.204511"),
                com.example.darks.repair_auto.repair.request.domain.RequestLocationSource.DEVICE_GPS,
                RepairRequestPriority.NORMAL,
                null,
                null,
                admin,
                NOW);
        ReflectionTestUtils.setField(saved, "id", 1001L);

        when(repairRequestService.mobileCreate(
                eq(42L),
                eq(4L),
                eq("Air conditioner is making noise"),
                eq(request.resolvedLocation()),
                eq("mobile:customer:42:k-123"))).thenReturn(saved);

        CustomerRepairRequestDetailResponse response = facade.createRequest(actor, "k-123", request);

        assertThat(response.id()).isEqualTo(1001L);
        assertThat(response.requestNumber()).isEqualTo("REQ-2026-000042");
        assertThat(response.category().name()).isEqualTo("Konditsioner");
        assertThat(response.location().address()).isEqualTo("Chilanzar 9, Tashkent");
        assertThat(response.availableActions()).isEmpty();
    }

    @Test
    void givenCustomerActor_whenListRequests_thenReturnsPaginatedSummaries() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        RepairRequest req = new RepairRequest(
                "REQ-2026-000042",
                customer,
                category,
                "Problem description",
                "Tashkent",
                null,
                null,
                null,
                RepairRequestPriority.NORMAL,
                null,
                null,
                admin,
                NOW);
        ReflectionTestUtils.setField(req, "id", 1001L);

        when(repairRequestRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(req), PageRequest.of(0, 20), 1));

        PageResponse<CustomerRepairRequestSummaryResponse> result = facade.listRequests(actor, null, null, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).id()).isEqualTo(1001L);
        assertThat(result.content().get(0).requestNumber()).isEqualTo("REQ-2026-000042");
        assertThat(result.content().get(0).category().name()).isEqualTo("Konditsioner");
    }

    @Test
    void givenCustomerActor_whenGetRequestDetail_thenReturnsCustomerSafeDetail() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        RepairRequest req = new RepairRequest(
                "REQ-2026-000042",
                customer,
                category,
                "Problem description",
                "Tashkent",
                new BigDecimal("41.2"),
                new BigDecimal("69.2"),
                com.example.darks.repair_auto.repair.request.domain.RequestLocationSource.DEVICE_GPS,
                RepairRequestPriority.NORMAL,
                null,
                null,
                admin,
                NOW);
        req.markInProgress(NOW);
        ReflectionTestUtils.setField(req, "id", 1001L);

        RepairAssignment assignment = new RepairAssignment(req, technician, NOW.plusDays(1), admin, NOW);
        assignment.accept(NOW);

        when(accessPolicy.requireCurrentCustomerCanReadRequest(actor, 1001L)).thenReturn(req);
        when(repairRequestRepository.findWithRelationsById(1001L)).thenReturn(Optional.of(req));
        when(repairAssignmentRepository.findActiveByRequestId(1001L, RepairResourceAccessPolicy.ACTIVE_ASSIGNMENT_STATUSES))
                .thenReturn(Optional.of(assignment));

        CustomerRepairRequestDetailResponse detail = facade.getRequestDetail(actor, 1001L);

        assertThat(detail.id()).isEqualTo(1001L);
        assertThat(detail.status()).isEqualTo(RepairRequestStatus.IN_PROGRESS);
        assertThat(detail.technician()).isNotNull();
        assertThat(detail.technician().fullName()).isEqualTo("Aziz Karimov");
        assertThat(detail.technician().specialization()).isEqualTo("Cooling Master");
        assertThat(detail.schedule()).isNotNull();
        assertThat(detail.review()).isNull();
    }

    @Test
    void givenCompletedRequestWithReview_whenGetRequestDetail_thenReturnsReviewInfo() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        RepairRequest req = new RepairRequest(
                "REQ-2026-000042",
                customer,
                category,
                "Problem description",
                "Tashkent",
                null,
                null,
                null,
                RepairRequestPriority.NORMAL,
                null,
                null,
                admin,
                NOW);
        req.markCompleted(NOW);
        ReflectionTestUtils.setField(req, "id", 1001L);

        RepairReview review = new RepairReview(
                req, customer, technician, 5, "Excellent", ReviewSource.MOBILE, LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(review, "id", 71L);

        when(accessPolicy.requireCurrentCustomerCanReadRequest(actor, 1001L)).thenReturn(req);
        when(repairRequestRepository.findWithRelationsById(1001L)).thenReturn(Optional.of(req));
        when(repairReviewRepository.findByRepairRequestId(1001L)).thenReturn(Optional.of(review));

        CustomerRepairRequestDetailResponse detail = facade.getRequestDetail(actor, 1001L);

        assertThat(detail.review()).isNotNull();
        assertThat(detail.review().id()).isEqualTo(71L);
        assertThat(detail.review().rating()).isEqualTo(5);
        assertThat(detail.review().comment()).isEqualTo("Excellent");
    }

    @Test
    void givenCustomerActor_whenSubmitReview_thenDelegatesToReviewService() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        RepairRequest req = new RepairRequest(
                "REQ-2026-000042", customer, category, "Problem", "Tashkent", null, null, null, RepairRequestPriority.NORMAL, null, null, admin, NOW);
        req.markCompleted(NOW);
        ReflectionTestUtils.setField(req, "id", 1001L);

        RepairReview review = new RepairReview(
                req, customer, technician, 5, "Fast and clean", ReviewSource.MOBILE, LanguageCode.UZ, NOW);
        ReflectionTestUtils.setField(review, "id", 71L);

        when(repairReviewService.submitReview(
                eq(42L), eq(1001L), eq(5), eq("Fast and clean"), eq(ReviewSource.MOBILE), eq(LanguageCode.UZ)))
                .thenReturn(review);

        CustomerReviewResponse response = facade.submitReview(
                actor, 1001L, new CustomerReviewCreateRequest(5, "Fast and clean"));

        assertThat(response.id()).isEqualTo(71L);
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Fast and clean");
        verify(repairReviewService).submitReview(42L, 1001L, 5, "Fast and clean", ReviewSource.MOBILE, LanguageCode.UZ);
    }

    @Test
    void givenCustomerActor_whenGetRequestTimeline_thenReturnsChronologicalTimeline() {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);
        RepairRequest req = new RepairRequest(
                "REQ-2026-000042",
                customer,
                category,
                "Problem description",
                "Tashkent",
                null,
                null,
                null,
                RepairRequestPriority.NORMAL,
                null,
                null,
                admin,
                NOW);
        ReflectionTestUtils.setField(req, "id", 1001L);

        RepairRequestStatusHistory h1 = new RepairRequestStatusHistory(req, null, RepairRequestStatus.NEW, "Initial", (User) null, NOW);
        RepairRequestStatusHistory h2 = new RepairRequestStatusHistory(req, RepairRequestStatus.NEW, RepairRequestStatus.IN_PROGRESS, "Started", (User) null, NOW.plusHours(1));

        when(accessPolicy.requireCurrentCustomerCanReadRequest(actor, 1001L)).thenReturn(req);
        when(statusHistoryRepository.findByRepairRequestIdOrderByChangedAtAscIdAsc(1001L)).thenReturn(List.of(h1, h2));

        List<CustomerRepairRequestTimelineItemResponse> timeline = facade.getRequestTimeline(actor, 1001L);

        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(0).status()).isEqualTo(RepairRequestStatus.NEW);
        assertThat(timeline.get(1).status()).isEqualTo(RepairRequestStatus.IN_PROGRESS);
    }

    @Test
    void givenTechnicianActor_whenCallingCustomerFacadeMethods_thenThrowsAccessDenied() {
        AuthenticatedMobileActor techActor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);

        BusinessException exception = catchThrowableOfType(
                () -> facade.getRequestDetail(techActor, 1001L),
                BusinessException.class);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }
}
