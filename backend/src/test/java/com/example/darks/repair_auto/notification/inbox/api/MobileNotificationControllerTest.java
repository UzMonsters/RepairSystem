package com.example.darks.repair_auto.notification.inbox.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import com.example.darks.repair_auto.notification.domain.NotificationType;
import com.example.darks.repair_auto.notification.inbox.api.dto.UnreadNotificationCountResponse;
import com.example.darks.repair_auto.notification.inbox.api.dto.UserNotificationResponse;
import com.example.darks.repair_auto.notification.inbox.application.UserNotificationService;
import com.example.darks.repair_auto.shared.error.ApiErrorResponseFactory;
import com.example.darks.repair_auto.shared.error.GlobalExceptionHandler;
import com.example.darks.repair_auto.shared.error.ResourceNotFoundException;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class MobileNotificationControllerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-18T10:00:00Z");

    private UserNotificationService service;
    private LocalizationService localizationService;
    private MockMvc mockMvc;

    private AuthenticatedMobileActor currentActor;

    @BeforeEach
    void setUp() {
        service = mock(UserNotificationService.class);
        localizationService = mock(LocalizationService.class);
        when(localizationService.get(any())).thenReturn("Localized message");

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
        ApiErrorResponseFactory errorFactory = new ApiErrorResponseFactory(traceIdService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(localizationService, errorFactory);
        MobileNotificationController controller = new MobileNotificationController(service);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(principalResolver, new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void givenNotifications_whenListWithoutFilter_thenReturnsAllPaginated() throws Exception {
        UserNotificationResponse item = new UserNotificationResponse(
                501L,
                NotificationType.REPAIR_COMPLETED,
                "Ta'mirlash yakunlandi",
                "REQ-2026-000042 yakunlandi",
                false,
                null,
                "REPAIR_REQUEST_DETAILS",
                101L,
                "REQ-2026-000042",
                NOW);

        when(service.listForMobile(eq(currentActor), any(), eq(null)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/mobile/me/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(501))
                .andExpect(jsonPath("$.content[0].type").value("REPAIR_COMPLETED"))
                .andExpect(jsonPath("$.content[0].title").value("Ta'mirlash yakunlandi"))
                .andExpect(jsonPath("$.content[0].body").value("REQ-2026-000042 yakunlandi"))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andExpect(jsonPath("$.content[0].target").value("REPAIR_REQUEST_DETAILS"))
                .andExpect(jsonPath("$.content[0].targetId").value(101))
                .andExpect(jsonPath("$.content[0].requestNumber").value("REQ-2026-000042"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void givenUnreadFilter_whenList_thenPassesUnreadTrueToService() throws Exception {
        when(service.listForMobile(eq(currentActor), any(), eq(true)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/mobile/me/notifications?unread=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(service).listForMobile(eq(currentActor), any(), eq(true));
    }

    @Test
    void givenReadFilter_whenList_thenPassesUnreadFalseToService() throws Exception {
        when(service.listForMobile(eq(currentActor), any(), eq(false)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/mobile/me/notifications?unread=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(service).listForMobile(eq(currentActor), any(), eq(false));
    }

    @Test
    void givenUnreadCount_whenGetUnreadCount_thenReturnsCount() throws Exception {
        when(service.getUnreadCount(currentActor)).thenReturn(new UnreadNotificationCountResponse(5L));

        mockMvc.perform(get("/api/v1/mobile/me/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    void givenValidNotificationId_whenMarkAsRead_thenReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/mobile/me/notifications/501/read"))
                .andExpect(status().isNoContent());

        verify(service).markAsRead(currentActor, 501L);
    }

    @Test
    void givenNonExistentNotificationId_whenMarkAsRead_thenReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Notification was not found."))
                .when(service).markAsRead(currentActor, 999L);

        mockMvc.perform(patch("/api/v1/mobile/me/notifications/999/read"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenAuthenticatedActor_whenMarkAllAsRead_thenReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/me/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(service).markAllAsRead(currentActor);
    }
}
