package com.example.darks.repair_auto.notification.push.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointRegisterRequest;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointResponse;
import com.example.darks.repair_auto.notification.push.api.dto.PushEndpointUnregisterRequest;
import com.example.darks.repair_auto.notification.push.application.PushEndpointService;
import com.example.darks.repair_auto.notification.push.domain.PushClientType;
import com.example.darks.repair_auto.notification.push.domain.PushFirebaseApp;
import com.example.darks.repair_auto.notification.push.domain.PushPlatform;
import com.example.darks.repair_auto.shared.error.ApiErrorResponseFactory;
import com.example.darks.repair_auto.shared.error.GlobalExceptionHandler;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class MobilePushEndpointControllerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private PushEndpointService service;
    private LocalizationService localizationService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private AuthenticatedMobileActor currentActor;

    @BeforeEach
    void setUp() {
        service = mock(PushEndpointService.class);
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
        MobilePushEndpointController controller = new MobilePushEndpointController(service);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(localizationService, errorResponseFactory);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void givenCustomerAndroidPayload_whenPutRegister_thenReturns200AndEndpointResponse() throws Exception {
        PushEndpointRegisterRequest request = new PushEndpointRegisterRequest(
                "fid-cust-android-123",
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                "1.0.0");

        PushEndpointResponse response = new PushEndpointResponse(
                61L,
                PushClientType.CUSTOMER_MOBILE,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                true,
                NOW);

        when(service.registerForMobile(eq(currentActor), any(PushEndpointRegisterRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/mobile/me/push-endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(61))
                .andExpect(jsonPath("$.clientType").value("CUSTOMER_MOBILE"))
                .andExpect(jsonPath("$.platform").value("ANDROID"))
                .andExpect(jsonPath("$.firebaseAppKey").value("CUSTOMER_ANDROID"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void givenInvalidMobilePayload_whenPutRegister_thenReturns400() throws Exception {
        PushEndpointRegisterRequest request = new PushEndpointRegisterRequest(
                " ",
                null,
                PushPlatform.ANDROID,
                PushFirebaseApp.CUSTOMER_ANDROID,
                null);

        mockMvc.perform(put("/api/v1/mobile/me/push-endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenValidUnregisterPayload_whenDeleteUnregister_thenReturns204() throws Exception {
        PushEndpointUnregisterRequest request = new PushEndpointUnregisterRequest(
                "fid-cust-android-123",
                PushFirebaseApp.CUSTOMER_ANDROID);

        mockMvc.perform(delete("/api/v1/mobile/me/push-endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(service).unregisterForMobile(eq(currentActor), any(PushEndpointUnregisterRequest.class));
    }
}
