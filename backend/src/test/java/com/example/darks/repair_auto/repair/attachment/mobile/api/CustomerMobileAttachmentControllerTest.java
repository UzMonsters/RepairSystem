package com.example.darks.repair_auto.repair.attachment.mobile.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.mobile.api.dto.MobileAttachmentResponse;
import com.example.darks.repair_auto.repair.attachment.mobile.application.MobileAttachmentFacade;
import com.example.darks.repair_auto.shared.error.ApiErrorResponseFactory;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.error.GlobalExceptionHandler;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;

class CustomerMobileAttachmentControllerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private MobileAttachmentFacade facade;
    private LocalizationService localizationService;
    private MockMvc mockMvc;
    private AuthenticatedMobileActor currentActor;

    @BeforeEach
    void setUp() {
        facade = mock(MobileAttachmentFacade.class);
        localizationService = mock(LocalizationService.class);
        when(localizationService.get(any())).thenReturn("Localized error");

        currentActor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 101L, "+998901234567", true);

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
        CustomerMobileAttachmentController controller = new CustomerMobileAttachmentController(facade);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(localizationService, errorResponseFactory);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void givenCustomerAuth_whenPostAttachment_thenReturns201Created() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "problem.jpg", "image/jpeg", new byte[]{1, 2, 3});

        MobileAttachmentResponse response = new MobileAttachmentResponse(
                501L,
                42L,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                "problem.jpg",
                "image/jpeg",
                3L,
                AttachmentStatus.AVAILABLE,
                "/api/v1/mobile/me/attachments/501/download",
                true,
                NOW);

        when(facade.uploadCustomerAttachment(eq(currentActor), eq(42L), any(), any(MultipartFile.class)))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/v1/mobile/me/repair-requests/42/attachments")
                        .file(file)
                        .param("attachmentType", "CUSTOMER_PROBLEM_PHOTO"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(501))
                .andExpect(jsonPath("$.type").value("CUSTOMER_PROBLEM_PHOTO"))
                .andExpect(jsonPath("$.originalFileName").value("problem.jpg"))
                .andExpect(jsonPath("$.downloadUrl").value("/api/v1/mobile/me/attachments/501/download"))
                .andExpect(jsonPath("$.imagePreview").value(true))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void givenCustomerAuth_whenGetAttachments_thenReturns200List() throws Exception {
        MobileAttachmentResponse response = new MobileAttachmentResponse(
                501L,
                42L,
                AttachmentType.CUSTOMER_PROBLEM_PHOTO,
                "problem.jpg",
                "image/jpeg",
                3L,
                AttachmentStatus.AVAILABLE,
                NOW);

        when(facade.listCustomerAttachments(eq(currentActor), eq(42L)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/mobile/me/repair-requests/42/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(501))
                .andExpect(jsonPath("$[0].type").value("CUSTOMER_PROBLEM_PHOTO"));
    }

    @Test
    void givenCrossCustomerOrNonExistent_whenGetAttachments_thenReturns404() throws Exception {
        when(facade.listCustomerAttachments(eq(currentActor), eq(999L)))
                .thenThrow(new BusinessException(ErrorCode.REPAIR_REQUEST_NOT_FOUND));

        mockMvc.perform(get("/api/v1/mobile/me/repair-requests/999/attachments"))
                .andExpect(status().isNotFound());
    }
}
