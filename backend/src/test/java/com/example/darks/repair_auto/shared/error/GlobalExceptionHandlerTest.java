package com.example.darks.repair_auto.shared.error;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.observability.TraceIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .addFilter(new TraceIdFilter(properties, new com.example.darks.repair_auto.shared.observability.TraceIdService()))
                .build();
    }

    @Test
    void givenInvalidRequestBodyWhenControllerValidatesThenStandardErrorIncludesTraceId()
            throws Exception {
        mockMvc.perform(post("/test/validation")
                        .header("X-Trace-Id", "test-trace-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Trace-Id", "test-trace-123"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.traceId").value("test-trace-123"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("NotBlank"));
    }

    @Test
    void givenMalformedJsonWhenBodyCannotBeReadThenInvalidBodyErrorIsReturned() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void givenInvalidParameterTypeWhenParameterIsBoundThenInvalidParameterErrorIsReturned()
            throws Exception {
        mockMvc.perform(get("/test/parameter").param("number", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("number"));
    }

    @Test
    void givenMissingRequiredParameterWhenHandledThenTraceHeaderMatchesBody() throws Exception {
        mockMvc.perform(get("/test/missing").header("X-Trace-Id", "missing-param-trace"))
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
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("X-Trace-Id", "media-type-trace"))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.traceId").value("media-type-trace"));
    }

    @Test
    void givenUnexpectedExceptionWhenHandledThenSafeInternalErrorIsReturned() throws Exception {
        mockMvc.perform(get("/test/unexpected").header("X-Trace-Id", "unexpected-trace"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Trace-Id", "unexpected-trace"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected internal error."))
                .andExpect(jsonPath("$.traceId").value("unexpected-trace"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("database password leaked"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("IllegalStateException"))));
    }

    @Test
    void givenBusinessRuleExceptionWhenHandledThenBusinessCodeIsPreserved() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHASE0_TEST_RULE"))
                .andExpect(jsonPath("$.message").value("Business rule failed."));
    }

    @Test
    void givenResourceNotFoundExceptionWhenHandledThenNotFoundErrorIsReturned() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void givenUnsupportedHttpMethodWhenHandledThenMethodNotAllowedErrorIsReturned()
            throws Exception {
        mockMvc.perform(post("/test/parameter"))
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
            throw new BusinessRuleException("PHASE0_TEST_RULE", "Business rule failed.");
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
