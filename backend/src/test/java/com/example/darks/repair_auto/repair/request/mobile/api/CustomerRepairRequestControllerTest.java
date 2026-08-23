package com.example.darks.repair_auto.repair.request.mobile.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestCreateRequest;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestDetailResponse;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestSummaryResponse;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerRepairRequestTimelineItemResponse;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerReviewCreateRequest;
import com.example.darks.repair_auto.repair.request.mobile.api.dto.CustomerReviewResponse;
import com.example.darks.repair_auto.repair.request.mobile.application.CustomerRepairRequestFacade;
import com.example.darks.repair_auto.shared.error.ApiErrorResponseFactory;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.error.GlobalExceptionHandler;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class CustomerRepairRequestControllerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private CustomerRepairRequestFacade facade;
    private LocalizationService localizationService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private AuthenticatedMobileActor currentActor;

    @BeforeEach
    void setUp() {
        facade = mock(CustomerRepairRequestFacade.class);
        localizationService = mock(LocalizationService.class);
        when(localizationService.get(any())).thenReturn("Localized message");
        objectMapper = new ObjectMapper();

        currentActor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 42L, "+998901234567", true);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().equals(AuthenticatedMobileActor.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory) {
                return currentActor;
            }
        };

        TraceIdService traceIdService = new TraceIdService();
        ApiErrorResponseFactory errorResponseFactory = new ApiErrorResponseFactory(traceIdService);
        CustomerRepairRequestController controller = new CustomerRepairRequestController(facade);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(localizationService, errorResponseFactory);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(principalResolver, new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void givenCustomerAuth_whenPostCreateRequest_thenReturns201Created() throws Exception {
        CustomerRepairRequestCreateRequest request = new CustomerRepairRequestCreateRequest(
                4L,
                "Air conditioner is not cooling properly",
                "Chilanzar 9, Tashkent",
                new BigDecimal("41.275412"),
                new BigDecimal("69.204511"));

        CustomerRepairRequestDetailResponse response = new CustomerRepairRequestDetailResponse(
                1001L,
                "REQ-2026-000042",
                RepairRequestStatus.NEW,
                "Yangi",
                new CustomerRepairRequestDetailResponse.CategorySummary(4L, "Konditsioner"),
                "Air conditioner is not cooling properly",
                new CustomerRepairRequestDetailResponse.LocationInfo("Chilanzar 9, Tashkent", new BigDecimal("41.275412"), new BigDecimal("69.204511")),
                null,
                null,
                null,
                List.of(),
                NOW,
                NOW);

        when(facade.createRequest(eq(currentActor), eq("k-12345"), any(CustomerRepairRequestCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/mobile/me/repair-requests")
                        .header("Idempotency-Key", "k-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1001))
                .andExpect(jsonPath("$.requestNumber").value("REQ-2026-000042"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.category.name").value("Konditsioner"))
                .andExpect(jsonPath("$.location.address").value("Chilanzar 9, Tashkent"))
                .andExpect(jsonPath("$.availableActions").isArray());
    }

    @Test
    void givenInvalidPayload_whenPostCreateRequest_thenReturns400() throws Exception {
        CustomerRepairRequestCreateRequest request = new CustomerRepairRequestCreateRequest(
                null,
                "short",
                "Tashkent",
                null,
                null);

        mockMvc.perform(post("/api/v1/mobile/me/repair-requests")
                        .header("Idempotency-Key", "k-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenCustomerAuth_whenGetList_thenReturns200Page() throws Exception {
        CustomerRepairRequestSummaryResponse summary = new CustomerRepairRequestSummaryResponse(
                1001L,
                "REQ-2026-000042",
                RepairRequestStatus.NEW,
                "Yangi",
                new CustomerRepairRequestSummaryResponse.CategorySummary(4L, "Konditsioner"),
                "Air conditioner problem",
                null,
                NOW);

        when(facade.listRequests(eq(currentActor), any(), any(), any()))
                .thenReturn(PageResponse.from(new org.springframework.data.domain.PageImpl<>(List.of(summary))));

        mockMvc.perform(get("/api/v1/mobile/me/repair-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1001))
                .andExpect(jsonPath("$.content[0].requestNumber").value("REQ-2026-000042"));
    }

    @Test
    void givenCustomerAuth_whenGetDetail_thenReturns200Detail() throws Exception {
        CustomerRepairRequestDetailResponse response = new CustomerRepairRequestDetailResponse(
                1001L,
                "REQ-2026-000042",
                RepairRequestStatus.NEW,
                "Yangi",
                new CustomerRepairRequestDetailResponse.CategorySummary(4L, "Konditsioner"),
                "Air conditioner problem",
                new CustomerRepairRequestDetailResponse.LocationInfo("Chilanzar", null, null),
                null,
                null,
                null,
                List.of(),
                NOW,
                NOW);

        when(facade.getRequestDetail(eq(currentActor), eq(1001L))).thenReturn(response);

        mockMvc.perform(get("/api/v1/mobile/me/repair-requests/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1001))
                .andExpect(jsonPath("$.requestNumber").value("REQ-2026-000042"))
                .andExpect(jsonPath("$.availableActions").isArray());
    }

    @Test
    void givenNonExistentOrCrossCustomer_whenGetDetail_thenReturns404NotFound() throws Exception {
        when(facade.getRequestDetail(eq(currentActor), eq(9999L)))
                .thenThrow(new BusinessException(ErrorCode.REPAIR_REQUEST_NOT_FOUND));

        mockMvc.perform(get("/api/v1/mobile/me/repair-requests/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenCustomerAuth_whenGetTimeline_thenReturns200Timeline() throws Exception {
        CustomerRepairRequestTimelineItemResponse item1 = new CustomerRepairRequestTimelineItemResponse(
                RepairRequestStatus.NEW, "Yangi", NOW);
        CustomerRepairRequestTimelineItemResponse item2 = new CustomerRepairRequestTimelineItemResponse(
                RepairRequestStatus.ASSIGNED, "Usta biriktirildi", NOW.plusHours(1));

        when(facade.getRequestTimeline(eq(currentActor), eq(1001L))).thenReturn(List.of(item1, item2));

        mockMvc.perform(get("/api/v1/mobile/me/repair-requests/1001/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andExpect(jsonPath("$[0].label").value("Yangi"))
                .andExpect(jsonPath("$[1].status").value("ASSIGNED"))
                .andExpect(jsonPath("$[1].label").value("Usta biriktirildi"));
    }

    @Test
    void givenCrossCustomer_whenGetTimeline_thenReturns404NotFound() throws Exception {
        when(facade.getRequestTimeline(eq(currentActor), eq(9999L)))
                .thenThrow(new BusinessException(ErrorCode.REPAIR_REQUEST_NOT_FOUND));

        mockMvc.perform(get("/api/v1/mobile/me/repair-requests/9999/timeline"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenCustomerAuth_whenPostReview_thenReturns201Created() throws Exception {
        CustomerReviewCreateRequest request = new CustomerReviewCreateRequest(5, "Fast and quality repair!");
        CustomerReviewResponse response = new CustomerReviewResponse(71L, 5, "Fast and quality repair!", NOW);

        when(facade.submitReview(eq(currentActor), eq(1001L), any(CustomerReviewCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/mobile/me/repair-requests/1001/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(71))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Fast and quality repair!"))
                .andExpect(jsonPath("$.submittedAt").exists());
    }

    @Test
    void givenInvalidRating_whenPostReview_thenReturns400BadRequest() throws Exception {
        CustomerReviewCreateRequest request = new CustomerReviewCreateRequest(6, "Invalid score");

        mockMvc.perform(post("/api/v1/mobile/me/repair-requests/1001/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenNonExistentOrCrossCustomer_whenPostReview_thenReturns404NotFound() throws Exception {
        CustomerReviewCreateRequest request = new CustomerReviewCreateRequest(5, "Nice");

        when(facade.submitReview(eq(currentActor), eq(9999L), any(CustomerReviewCreateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.REPAIR_REQUEST_NOT_FOUND));

        mockMvc.perform(post("/api/v1/mobile/me/repair-requests/9999/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
