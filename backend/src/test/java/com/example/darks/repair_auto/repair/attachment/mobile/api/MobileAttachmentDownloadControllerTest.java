package com.example.darks.repair_auto.repair.attachment.mobile.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.repair.attachment.mobile.api.dto.MobileAttachmentDownloadUrlResponse;
import com.example.darks.repair_auto.repair.attachment.mobile.application.MobileAttachmentFacade;
import com.example.darks.repair_auto.shared.error.ApiErrorResponseFactory;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.error.GlobalExceptionHandler;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class MobileAttachmentDownloadControllerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private MobileAttachmentFacade facade;
    private LocalizationService localizationService;
    private MockMvc mockMvc;
    private AuthenticatedMobileActor currentActor;

    @BeforeEach
    void setUp() {
        facade = mock(MobileAttachmentFacade.class);
        localizationService = mock(LocalizationService.class);

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
        MobileAttachmentDownloadController controller = new MobileAttachmentDownloadController(facade);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(localizationService, errorResponseFactory);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void givenAuthorizedActor_whenGetDownloadUrl_thenReturns200WithUrlAndExpiry() throws Exception {
        MobileAttachmentDownloadUrlResponse response = new MobileAttachmentDownloadUrlResponse(
                501L,
                "https://s3.example.com/bucket/key?signature=abc",
                NOW.plusMinutes(15));

        when(facade.getDownloadUrl(eq(currentActor), eq(501L))).thenReturn(response);

        mockMvc.perform(get("/api/v1/mobile/me/attachments/501/download-url"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachmentId").value(501))
                .andExpect(jsonPath("$.url").value("https://s3.example.com/bucket/key?signature=abc"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void givenUnauthorizedOrNonExistent_whenGetDownloadUrl_thenReturns404() throws Exception {
        when(facade.getDownloadUrl(eq(currentActor), eq(999L)))
                .thenThrow(new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/mobile/me/attachments/999/download-url"))
                .andExpect(status().isNotFound());
    }
}
