package com.example.darks.repair_auto.repair.technician.mobile.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRejectionRequest;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.execution.api.dto.CompleteRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.DiagnosisRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.WaitForPartsRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianJobDetailResponse;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianJobSummaryResponse;
import com.example.darks.repair_auto.repair.technician.mobile.api.dto.TechnicianScheduleItemResponse;
import com.example.darks.repair_auto.repair.technician.mobile.application.TechnicianJobFacade;
import com.example.darks.repair_auto.shared.error.ApiErrorResponseFactory;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.error.GlobalExceptionHandler;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class TechnicianJobControllerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private TechnicianJobFacade facade;
    private LocalizationService localizationService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private AuthenticatedMobileActor currentActor;

    @BeforeEach
    void setUp() {
        facade = mock(TechnicianJobFacade.class);
        localizationService = mock(LocalizationService.class);
        when(localizationService.get(any())).thenReturn("Localized message");
        objectMapper = new ObjectMapper();

        currentActor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 17L, "+998901112233", true);

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
        TechnicianJobController controller = new TechnicianJobController(facade);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(localizationService, errorResponseFactory);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(principalResolver, new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void givenTechnicianAuth_whenGetJobs_thenReturns200Page() throws Exception {
        TechnicianJobSummaryResponse summary = new TechnicianJobSummaryResponse(
                42L,
                19L,
                "REQ-2026-000042",
                RepairRequestStatus.ASSIGNED,
                "Biriktirilgan",
                AssignmentStatus.PENDING,
                "Kutilmoqda",
                new TechnicianJobSummaryResponse.CategorySummary(4L, "Konditsioner"),
                new TechnicianJobSummaryResponse.CustomerSummary("Ali Valiyev"),
                "Chilanzar 9, Tashkent",
                NOW.plusDays(1),
                NOW);

        when(facade.listJobs(eq(currentActor), any(), any()))
                .thenReturn(PageResponse.from(new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1)));

        mockMvc.perform(get("/api/v1/mobile/me/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].requestId").value(42))
                .andExpect(jsonPath("$.content[0].assignmentId").value(19))
                .andExpect(jsonPath("$.content[0].requestNumber").value("REQ-2026-000042"));
    }

    @Test
    void givenTechnicianAuth_whenGetJobDetail_thenReturns200Detail() throws Exception {
        TechnicianJobDetailResponse detail = new TechnicianJobDetailResponse(
                42L,
                19L,
                "REQ-2026-000042",
                RepairRequestStatus.IN_PROGRESS,
                "Ta'mirlash jarayonida",
                AssignmentStatus.ACCEPTED,
                "Qabul qilingan",
                new TechnicianJobDetailResponse.CategorySummary(4L, "Konditsioner"),
                "Air conditioner problem",
                new TechnicianJobDetailResponse.CustomerInfo(101L, "Ali Valiyev", "+998901234567"),
                new TechnicianJobDetailResponse.LocationInfo("Chilanzar 9", new BigDecimal("41.2"), new BigDecimal("69.2")),
                new TechnicianJobDetailResponse.ScheduleInfo(NOW.plusDays(1)),
                new TechnicianJobDetailResponse.ExecutionInfo("Capacitor failed", null, null, null),
                List.of(com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction.UPDATE_DIAGNOSIS, com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction.WAIT_FOR_PARTS),
                NOW,
                NOW);

        when(facade.getJobDetail(eq(currentActor), eq(42L))).thenReturn(detail);

        mockMvc.perform(get("/api/v1/mobile/me/jobs/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(42))
                .andExpect(jsonPath("$.customer.phone").value("+998901234567"))
                .andExpect(jsonPath("$.execution.diagnosis").value("Capacitor failed"))
                .andExpect(jsonPath("$.availableActions[0]").value("UPDATE_DIAGNOSIS"))
                .andExpect(jsonPath("$.availableActions[1]").value("WAIT_FOR_PARTS"));
    }

    @Test
    void givenNonExistentOrRejected_whenGetJobDetail_thenReturns404() throws Exception {
        when(facade.getJobDetail(eq(currentActor), eq(999L)))
                .thenThrow(new BusinessException(ErrorCode.REPAIR_REQUEST_NOT_FOUND));

        mockMvc.perform(get("/api/v1/mobile/me/jobs/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenTechnicianAuth_whenGetSchedule_thenReturns200List() throws Exception {
        TechnicianScheduleItemResponse item = new TechnicianScheduleItemResponse(
                42L,
                19L,
                "REQ-2026-000042",
                RepairRequestStatus.ASSIGNED,
                "Biriktirilgan",
                AssignmentStatus.PENDING,
                "Kutilmoqda",
                new TechnicianScheduleItemResponse.CategorySummary(4L, "Konditsioner"),
                new TechnicianScheduleItemResponse.CustomerSummary("Ali Valiyev", "+998901234567"),
                "Chilanzar 9",
                NOW.plusDays(1));

        when(facade.getSchedule(eq(currentActor), eq(LocalDate.of(2026, 8, 18)), eq(LocalDate.of(2026, 8, 25))))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/mobile/me/schedule")
                        .param("from", "2026-08-18")
                        .param("to", "2026-08-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(42))
                .andExpect(jsonPath("$[0].customer.phone").value("+998901234567"));
    }

    @Test
    void givenTechnicianAuth_whenPostAccept_thenReturns200Detail() throws Exception {
        TechnicianJobDetailResponse detail = new TechnicianJobDetailResponse(
                42L, 19L, "REQ-2026-000042", RepairRequestStatus.ASSIGNED, "Biriktirilgan",
                AssignmentStatus.ACCEPTED, "Qabul qilingan",
                new TechnicianJobDetailResponse.CategorySummary(4L, "Konditsioner"),
                "Problem", new TechnicianJobDetailResponse.CustomerInfo(101L, "Ali", "+998901234567"),
                new TechnicianJobDetailResponse.LocationInfo("Tashkent", null, null), null, null,
                List.of(com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction.START_REPAIR), NOW, NOW);

        when(facade.acceptAssignment(eq(currentActor), eq(42L))).thenReturn(detail);

        mockMvc.perform(post("/api/v1/mobile/me/jobs/42/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignmentStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.availableActions[0]").value("START_REPAIR"));
    }

    @Test
    void givenTechnicianAuth_whenPostReject_thenReturns204NoContent() throws Exception {
        AssignmentRejectionRequest request = new AssignmentRejectionRequest("Location out of reach");

        mockMvc.perform(post("/api/v1/mobile/me/jobs/42/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(facade).rejectAssignment(eq(currentActor), eq(42L), any(AssignmentRejectionRequest.class));
    }

    @Test
    void givenTechnicianAuth_whenPostStart_thenReturns200Detail() throws Exception {
        TechnicianJobDetailResponse detail = new TechnicianJobDetailResponse(
                42L, 19L, "REQ-2026-000042", RepairRequestStatus.IN_PROGRESS, "Ta'mirlash jarayonida",
                AssignmentStatus.ACCEPTED, "Qabul qilingan",
                new TechnicianJobDetailResponse.CategorySummary(4L, "Konditsioner"),
                "Problem", new TechnicianJobDetailResponse.CustomerInfo(101L, "Ali", "+998901234567"),
                new TechnicianJobDetailResponse.LocationInfo("Tashkent", null, null), null, null,
                List.of(com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction.UPDATE_DIAGNOSIS, com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction.WAIT_FOR_PARTS),
                NOW, NOW);

        when(facade.startRepair(eq(currentActor), eq(42L))).thenReturn(detail);

        mockMvc.perform(post("/api/v1/mobile/me/jobs/42/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.availableActions[0]").value("UPDATE_DIAGNOSIS"));
    }

    @Test
    void givenTechnicianAuth_whenPatchDiagnosis_thenReturns200Detail() throws Exception {
        DiagnosisRequest request = new DiagnosisRequest("Compressor capacitor dead");
        TechnicianJobDetailResponse detail = new TechnicianJobDetailResponse(
                42L, 19L, "REQ-2026-000042", RepairRequestStatus.IN_PROGRESS, "Ta'mirlash jarayonida",
                AssignmentStatus.ACCEPTED, "Qabul qilingan",
                new TechnicianJobDetailResponse.CategorySummary(4L, "Konditsioner"),
                "Problem", new TechnicianJobDetailResponse.CustomerInfo(101L, "Ali", "+998901234567"),
                new TechnicianJobDetailResponse.LocationInfo("Tashkent", null, null), null,
                new TechnicianJobDetailResponse.ExecutionInfo("Compressor capacitor dead", null, null, null),
                List.of(com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction.UPDATE_DIAGNOSIS, com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction.WAIT_FOR_PARTS),
                NOW, NOW);

        when(facade.updateDiagnosis(eq(currentActor), eq(42L), any(DiagnosisRequest.class))).thenReturn(detail);

        mockMvc.perform(patch("/api/v1/mobile/me/jobs/42/diagnosis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.execution.diagnosis").value("Compressor capacitor dead"))
                .andExpect(jsonPath("$.availableActions[0]").value("UPDATE_DIAGNOSIS"));
    }

    @Test
    void givenTechnicianAuth_whenPostWaitForParts_thenReturns200Detail() throws Exception {
        WaitForPartsRequest request = new WaitForPartsRequest("Need 45uF capacitor");
        TechnicianJobDetailResponse detail = new TechnicianJobDetailResponse(
                42L, 19L, "REQ-2026-000042", RepairRequestStatus.WAITING_FOR_PARTS, "Ehtiyot qismlar kutilmoqda",
                AssignmentStatus.ACCEPTED, "Qabul qilingan",
                new TechnicianJobDetailResponse.CategorySummary(4L, "Konditsioner"),
                "Problem", new TechnicianJobDetailResponse.CustomerInfo(101L, "Ali", "+998901234567"),
                new TechnicianJobDetailResponse.LocationInfo("Tashkent", null, null), null,
                new TechnicianJobDetailResponse.ExecutionInfo("Capacitor dead", null, null, "Need 45uF capacitor"),
                List.of(com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction.UPDATE_DIAGNOSIS, com.example.darks.repair_auto.repair.action.domain.RepairAvailableAction.RESUME_REPAIR),
                NOW, NOW);

        when(facade.waitForParts(eq(currentActor), eq(42L), any(WaitForPartsRequest.class))).thenReturn(detail);

        mockMvc.perform(post("/api/v1/mobile/me/jobs/42/wait-for-parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value("WAITING_FOR_PARTS"))
                .andExpect(jsonPath("$.availableActions[0]").value("UPDATE_DIAGNOSIS"))
                .andExpect(jsonPath("$.availableActions[1]").value("RESUME_REPAIR"));
    }

    @Test
    void givenTechnicianAuth_whenPostComplete_thenReturns200Detail() throws Exception {
        CompleteRepairRequest request = new CompleteRepairRequest("Replaced capacitor", "All tests ok");
        TechnicianJobDetailResponse detail = new TechnicianJobDetailResponse(
                42L, 19L, "REQ-2026-000042", RepairRequestStatus.COMPLETED, "Bajarildi",
                AssignmentStatus.COMPLETED, "Bajarilgan",
                new TechnicianJobDetailResponse.CategorySummary(4L, "Konditsioner"),
                "Problem", new TechnicianJobDetailResponse.CustomerInfo(101L, "Ali", "+998901234567"),
                new TechnicianJobDetailResponse.LocationInfo("Tashkent", null, null), null,
                new TechnicianJobDetailResponse.ExecutionInfo("Capacitor dead", "Replaced capacitor", "All tests ok", null),
                List.of(),
                NOW, NOW);

        when(facade.completeRepair(eq(currentActor), eq(42L), any(CompleteRepairRequest.class))).thenReturn(detail);

        mockMvc.perform(post("/api/v1/mobile/me/jobs/42/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.assignmentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.availableActions").isArray())
                .andExpect(jsonPath("$.availableActions").isEmpty());
    }
}
