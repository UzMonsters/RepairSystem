package com.example.darks.repair_auto.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.settings.infrastructure.SystemSettingsRepository;
import com.example.darks.repair_auto.settings.infrastructure.UserSettingsRepository;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.i18n.JsonTranslationCatalog;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.shared.i18n.LocalizationServiceImpl;
import com.example.darks.repair_auto.shared.i18n.RequestLocaleResolver;
import com.example.darks.repair_auto.shared.observability.TraceIdFilter;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

class SecurityErrorHandlerTest {

    private SecurityErrorHandler handler;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonTranslationCatalog catalog = new JsonTranslationCatalog(objectMapper);
        catalog.init();

        UserSettingsRepository userSettingsRepository = Mockito.mock(UserSettingsRepository.class);
        SystemSettingsRepository systemSettingsRepository = Mockito.mock(SystemSettingsRepository.class);
        Mockito.when(userSettingsRepository.findByUserId(Mockito.any())).thenReturn(Optional.empty());
        Mockito.when(systemSettingsRepository.findById(Mockito.any())).thenReturn(Optional.empty());

        RequestLocaleResolver localeResolver = new RequestLocaleResolver(userSettingsRepository, systemSettingsRepository);
        LocalizationService localizationService = new LocalizationServiceImpl(catalog, localeResolver);
        TraceIdService traceIdService = new TraceIdService();
        ApiErrorResponseFactory responseFactory = new ApiErrorResponseFactory(traceIdService);

        handler = new SecurityErrorHandler(properties(), localizationService, responseFactory, objectMapper);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void givenAccessDeniedInEnglishWhenHandledThenStandardLocalizedErrorIsWritten() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/protected");
        request.addHeader("Accept-Language", "en");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put(TraceIdFilter.MDC_KEY, "security-trace-123");

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("\"code\":\"ACCESS_DENIED\"");
        assertThat(response.getContentAsString()).contains("\"message\":\"You do not have permission to perform this action.\"");
        assertThat(response.getContentAsString()).contains("\"traceId\":\"security-trace-123\"");
    }

    @Test
    void givenAccessDeniedInUzbekWhenHandledThenUzbekLocalizedErrorIsWritten() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/protected");
        request.addHeader("Accept-Language", "uz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put(TraceIdFilter.MDC_KEY, "security-trace-456");

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":\"ACCESS_DENIED\"");
        assertThat(response.getContentAsString()).contains("\"message\":\"Sizda ushbu amalni bajarish uchun ruxsat yo'q.\"");
        assertThat(response.getContentAsString()).contains("\"traceId\":\"security-trace-456\"");
    }

    @Test
    void givenAuthenticationRequiredInRussianWhenCommencedThenRussianLocalizedErrorIsWritten() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/protected");
        request.addHeader("Accept-Language", "ru");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(request, response, Mockito.mock(AuthenticationException.class));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":\"AUTHENTICATION_REQUIRED\"");
        assertThat(response.getContentAsString()).contains("\"message\":\"\u0422\u0440\u0435\u0431\u0443\u0435\u0442\u0441\u044F \u0430\u0443\u0442\u0435\u043D\u0442\u0438\u0444\u0438\u043A\u0430\u0446\u0438\u044F.\"");
    }

    private AppProperties properties() {
        return new AppProperties(
                new AppProperties.Cors(List.of(), List.of(), List.of(), List.of(), false),
                new AppProperties.Trace("X-Trace-Id"),
                new AppProperties.Jwt("test-secret-that-is-at-least-32-characters", "repair-auto", Duration.ofMinutes(15)),
                Duration.ofDays(1),
                Duration.ofDays(30),
                new AppProperties.BootstrapAdmin(false, "", "", ""));
    }
}
