package com.example.darks.repair_auto.identity.mobile.telegram.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.telegram.TelegramMobileAuthService;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileActorSummary;
import com.example.darks.repair_auto.identity.mobile.telegram.dto.MobileAuthResponse;
import com.example.darks.repair_auto.shared.error.ApiErrorResponseFactory;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.error.GlobalExceptionHandler;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TelegramMobileAuthControllerTest {

    private MockMvc mockMvc;
    private TelegramMobileAuthService telegramMobileAuthService;
    private LocalizationService localizationService;

    @BeforeEach
    void setUp() {
        telegramMobileAuthService = mock(TelegramMobileAuthService.class);
        localizationService = mock(LocalizationService.class);
        TraceIdService traceIdService = new TraceIdService();
        ApiErrorResponseFactory errorResponseFactory = new ApiErrorResponseFactory(traceIdService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(localizationService, errorResponseFactory);

        TelegramMobileAuthController controller = new TelegramMobileAuthController(telegramMobileAuthService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        when(localizationService.get(any())).thenReturn("Localized message");
        when(localizationService.get(any(), any(Object[].class))).thenReturn("Localized message");
        when(localizationService.get(any(String.class), any(jakarta.servlet.http.HttpServletRequest.class), any(Object[].class)))
                .thenReturn("Localized message");
    }

    @Test
    void givenValidCustomerPayloadWhenLoginCustomerThenReturns200WithTokens() throws Exception {
        MobileAuthResponse response = new MobileAuthResponse(
                "Bearer",
                "customer-access-token",
                "customer-refresh-token",
                900L,
                2592000L,
                new MobileActorSummary(ActorType.CUSTOMER, 12L, "Customer Test", "+998901234567", "uz"));

        when(telegramMobileAuthService.loginCustomer("valid-id-token")).thenReturn(response);

        mockMvc.perform(post("/api/v1/mobile/auth/telegram/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "valid-id-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("customer-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("customer-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshExpiresIn").value(2592000))
                .andExpect(jsonPath("$.actor.type").value("CUSTOMER"))
                .andExpect(jsonPath("$.actor.id").value(12))
                .andExpect(jsonPath("$.actor.fullName").value("Customer Test"))
                .andExpect(jsonPath("$.actor.phone").value("+998901234567"))
                .andExpect(jsonPath("$.actor.preferredLanguage").value("uz"));
    }

    @Test
    void givenValidTechnicianPayloadWhenLoginTechnicianThenReturns200WithTokens() throws Exception {
        MobileAuthResponse response = new MobileAuthResponse(
                "Bearer",
                "technician-access-token",
                "technician-refresh-token",
                900L,
                2592000L,
                new MobileActorSummary(ActorType.TECHNICIAN, 34L, "Technician Test", "+998909876543", "ru"));

        when(telegramMobileAuthService.loginTechnician("valid-tech-id-token")).thenReturn(response);

        mockMvc.perform(post("/api/v1/mobile/auth/telegram/technician")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idToken": "valid-tech-id-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("technician-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("technician-refresh-token"))
                .andExpect(jsonPath("$.actor.type").value("TECHNICIAN"))
                .andExpect(jsonPath("$.actor.id").value(34));
    }

    @Test
    void givenValidRefreshPayloadWhenRefreshThenReturns200WithNewTokens() throws Exception {
        MobileAuthResponse response = new MobileAuthResponse(
                "Bearer",
                "new-access-token",
                "new-refresh-token",
                900L,
                2592000L,
                new MobileActorSummary(ActorType.CUSTOMER, 12L, "Customer Test", "+998901234567", "uz"));

        when(telegramMobileAuthService.refresh("valid-refresh-token")).thenReturn(response);

        mockMvc.perform(post("/api/v1/mobile/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "valid-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void givenBlankRefreshTokenWhenRefreshThenReturns400BadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void givenReusedRefreshTokenWhenRefreshThenReturns401Unauthorized() throws Exception {
        when(telegramMobileAuthService.refresh("reused-token"))
                .thenThrow(new BusinessException(ErrorCode.MOBILE_REFRESH_TOKEN_REUSED));

        mockMvc.perform(post("/api/v1/mobile/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "reused-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MOBILE_REFRESH_TOKEN_REUSED"));
    }

    @Test
    void givenExpiredRefreshTokenWhenRefreshThenReturns401Unauthorized() throws Exception {
        when(telegramMobileAuthService.refresh("expired-token"))
                .thenThrow(new BusinessException(ErrorCode.MOBILE_REFRESH_TOKEN_EXPIRED));

        mockMvc.perform(post("/api/v1/mobile/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "expired-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("MOBILE_REFRESH_TOKEN_EXPIRED"));
    }

    @Test
    void givenValidLogoutRequestWhenLogoutThenReturns204NoContent() throws Exception {
        mockMvc.perform(post("/api/v1/mobile/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "token-to-logout"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(telegramMobileAuthService).logout("token-to-logout");
    }

    @Test
    void givenCustomerPrincipalWhenLogoutAllThenReturns204NoContent() throws Exception {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.CUSTOMER, 12L, "+998901234567", true);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                actor,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            mockMvc.perform(post("/api/v1/mobile/auth/logout-all")
                            .principal(auth))
                    .andExpect(status().isNoContent());

            verify(telegramMobileAuthService).logoutAll(actor);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void givenTechnicianPrincipalWhenLogoutAllThenReturns204NoContent() throws Exception {
        AuthenticatedMobileActor actor = new AuthenticatedMobileActor(ActorType.TECHNICIAN, 34L, "+998909876543", true);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                actor,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_TECHNICIAN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            mockMvc.perform(post("/api/v1/mobile/auth/logout-all")
                            .principal(auth))
                    .andExpect(status().isNoContent());

            verify(telegramMobileAuthService).logoutAll(actor);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
