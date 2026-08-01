package com.example.darks.repair_auto.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.observability.TraceIdFilter;
import com.example.darks.repair_auto.config.AppProperties;
import com.example.darks.repair_auto.observability.TraceIdService;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class SecurityErrorHandlerTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void givenAccessDeniedWhenHandledThenStandardErrorIsWritten() throws Exception {
        SecurityErrorHandler handler = new SecurityErrorHandler(properties(), new TraceIdService());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/protected");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put(TraceIdFilter.MDC_KEY, "security-trace-123");

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("\"code\":\"ACCESS_DENIED\"");
        assertThat(response.getContentAsString()).contains("\"traceId\":\"security-trace-123\"");
    }

    private AppProperties properties() {
        return new AppProperties(
                new AppProperties.Cors(List.of(), List.of(), List.of(), List.of(), false),
                new AppProperties.Trace("X-Trace-Id"),
                new AppProperties.Jwt("test-secret-that-is-at-least-32-characters", "repair-auto", Duration.ofMinutes(15)),
                Duration.ofDays(30),
                new AppProperties.BootstrapAdmin(false, "", "", ""));
    }
}
