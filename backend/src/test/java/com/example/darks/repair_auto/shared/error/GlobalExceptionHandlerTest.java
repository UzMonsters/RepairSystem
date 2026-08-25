package com.example.darks.repair_auto.shared.error;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AppProperties properties = new AppProperties(
                new AppProperties.Cors(List.of(), List.of(), List.of(), List.of(), false),
                new AppProperties.Trace("X-Trace-Id"),
                new AppProperties.Jwt("test-secret-that-is-at-least-32-characters", "repair-auto", Duration.ofMinutes(15)),
                Duration.ofDays(1),
                Duration.ofDays(30),
                new AppProperties.BootstrapAdmin(false, "", "", ""));

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

        org.springframework.web.filter.CharacterEncodingFilter encodingFilter = new org.springframework.web.filter.CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler(localizationService, responseFactory))
                .setValidator(validator)
                .addFilter(encodingFilter)
                .addFilter(new TraceIdFilter(properties, traceIdService))
                .build();
    }

    @Test
    void givenInvalidRequestBodyWhenControllerValidatesThenStandardErrorIncludesTraceId()
            throws Exception {
        mockMvc.perform(post("/test/validation")
                        .header("X-Trace-Id", "test-trace-123")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Trace-Id", "test-trace-123"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.traceId").value("test-trace-123"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("This field is required."));
    }

    @Test
    void givenAcceptLanguageRuWhenValidationFailsThenRussianMessageIsReturned() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .header("Accept-Language", "ru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("\u041E\u0448\u0438\u0431\u043A\u0430 \u043F\u0440\u043E\u0432\u0435\u0440\u043A\u0438 \u0434\u0430\u043D\u043D\u044B\u0445 \u0437\u0430\u043F\u0440\u043E\u0441\u0430."))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("\u042D\u0442\u043E \u043F\u043E\u043B\u0435 \u043E\u0431\u044F\u0437\u0430\u0442\u0435\u043B\u044C\u043D\u043E \u0434\u043B\u044F \u0437\u0430\u043F\u043E\u043B\u043D\u0435\u043D\u0438\u044F."));
    }

    @Test
    void givenAcceptLanguageUzWhenValidationFailsThenUzbekMessageIsReturned() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .header("Accept-Language", "uz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("So'rov ma'lumotlarini tekshirishda xatolik."))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Ushbu maydon to'ldirilishi majburiy."));
    }

    @Test
    void givenMalformedJsonWhenBodyCannotBeReadThenInvalidBodyErrorIsReturned() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").value("Request body is malformed or invalid JSON."))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void givenInvalidParameterTypeWhenParameterIsBoundThenInvalidParameterErrorIsReturned()
            throws Exception {
        mockMvc.perform(get("/test/parameter").param("number", "abc").header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("number"));
    }

    @Test
    void givenMissingRequiredParameterWhenHandledThenTraceHeaderMatchesBody() throws Exception {
        mockMvc.perform(get("/test/missing").header("X-Trace-Id", "missing-param-trace").header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Trace-Id", "missing-param-trace"))
                .andExpect(jsonPath("$.code").value("MISSING_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.traceId").value("missing-param-trace"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("required"));
    }

    @Test
    void givenUnsupportedMediaTypeWhenHandledThenUnsupportedMediaTypeErrorIsReturned()
            throws Exception {
        mockMvc.perform(post("/test/validation")
                        .header("X-Trace-Id", "media-type-trace")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("X-Trace-Id", "media-type-trace"))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.traceId").value("media-type-trace"));
    }

    @Test
    void givenUnexpectedExceptionWhenHandledThenSafeInternalErrorIsReturned() throws Exception {
        mockMvc.perform(get("/test/unexpected").header("X-Trace-Id", "unexpected-trace").header("Accept-Language", "en"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Trace-Id", "unexpected-trace"))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.traceId").value("unexpected-trace"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("database password leaked"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("IllegalStateException"))));
    }

    @Test
    void givenBusinessExceptionWhenHandledThenErrorCodeAndLocalizedMessageReturned() throws Exception {
        mockMvc.perform(get("/test/business").header("Accept-Language", "ru"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPAIR_REQUEST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("\u0417\u0430\u044F\u0432\u043A\u0430 \u043D\u0430 \u0440\u0435\u043C\u043E\u043D\u0442 \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D\u0430."));
    }

    @Test
    void givenBusinessExceptionWithArgumentsWhenHandledThenArgumentsAreFormatted() throws Exception {
        mockMvc.perform(get("/test/status-invalid").header("Accept-Language", "en"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_REPAIR_REQUEST_STATUS"))
                .andExpect(jsonPath("$.message").value("This action cannot be performed while the repair request is in status COMPLETED."));

        mockMvc.perform(get("/test/status-invalid").header("Accept-Language", "ru"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_REPAIR_REQUEST_STATUS"))
                .andExpect(jsonPath("$.message").value("\u041D\u0435\u0432\u043E\u0437\u043C\u043E\u0436\u043D\u043E \u0432\u044B\u043F\u043E\u043B\u043D\u0438\u0442\u044C \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435, \u043F\u043E\u043A\u0430 \u0437\u0430\u044F\u0432\u043A\u0430 \u043D\u0430\u0445\u043E\u0434\u0438\u0442\u0441\u044F \u0432 \u0441\u0442\u0430\u0442\u0443\u0441\u0435 COMPLETED."));

        mockMvc.perform(get("/test/status-invalid").header("Accept-Language", "uz"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_REPAIR_REQUEST_STATUS"))
                .andExpect(jsonPath("$.message").value("Ta'mirlash arizasi COMPLETED holatida bo'lganda ushbu amalni bajarib bo'lmaydi."));
    }

    @Test
    void givenChatCustomBusinessRuleExceptionWhenHandledThenCustomCodeAndLocalizedMessageReturned() throws Exception {
        mockMvc.perform(get("/test/chat-closed").header("Accept-Language", "en"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONVERSATION_CLOSED"))
                .andExpect(jsonPath("$.message").value("Conversation is closed."));

        mockMvc.perform(get("/test/chat-closed").header("Accept-Language", "ru"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONVERSATION_CLOSED"))
                .andExpect(jsonPath("$.message").value("\u0411\u0435\u0441\u0435\u0434\u0430 \u0437\u0430\u043A\u0440\u044B\u0442\u0430."));

        mockMvc.perform(get("/test/chat-closed").header("Accept-Language", "uz"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONVERSATION_CLOSED"))
                .andExpect(jsonPath("$.message").value("Suhbat yopilgan."));
    }

    @Test
    void givenDataIntegrityViolationWhenHandledThenSanitizedMappedErrorCodeAndLocalizedMessageReturned() throws Exception {
        mockMvc.perform(get("/test/data-integrity").header("Accept-Language", "en"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("User with this email already exists."))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("duplicate key value violates unique constraint"))));
    }

    @Test
    void givenResourceNotFoundExceptionWhenHandledThenNotFoundErrorIsReturned() throws Exception {
        mockMvc.perform(get("/test/not-found").header("Accept-Language", "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Requested resource was not found."));
    }

    @Test
    void givenUnsupportedHttpMethodWhenHandledThenMethodNotAllowedErrorIsReturned()
            throws Exception {
        mockMvc.perform(post("/test/parameter").header("Accept-Language", "en"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @RestController
    private static class TestController {

        @PostMapping(value = "/test/validation", consumes = MediaType.APPLICATION_JSON_VALUE)
        String validate(@Valid @RequestBody TestRequest request) {
            return request.name();
        }

        @GetMapping("/test/parameter")
        String parameter(@RequestParam Integer number) {
            return number.toString();
        }

        @GetMapping("/test/missing")
        String missing(@RequestParam String required) {
            return required;
        }

        @GetMapping("/test/business")
        String business() {
            throw new BusinessException(ErrorCode.REPAIR_REQUEST_NOT_FOUND);
        }

        @GetMapping("/test/status-invalid")
        String statusInvalid() {
            throw new BusinessException(ErrorCode.INVALID_REPAIR_REQUEST_STATUS, "COMPLETED");
        }

        @GetMapping("/test/chat-closed")
        String chatClosed() {
            throw new BusinessRuleException("CONVERSATION_CLOSED", "Conversation is closed.", 409);
        }

        @GetMapping("/test/data-integrity")
        String dataIntegrity() {
            throw new DataIntegrityViolationException(
                    "could not execute statement",
                    new RuntimeException("duplicate key value violates unique constraint \"users_email_key\""));
        }

        @GetMapping("/test/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Missing test resource.");
        }

        @GetMapping("/test/unexpected")
        String unexpected() {
            throw new IllegalStateException("database password leaked");
        }
    }

    private record TestRequest(@NotBlank String name) {
    }
}
