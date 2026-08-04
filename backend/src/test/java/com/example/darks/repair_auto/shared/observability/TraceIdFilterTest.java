package com.example.darks.repair_auto.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.shared.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    @Test
    void givenValidIncomingTraceIdWhenRequestIsFilteredThenTraceIdIsPreserved()
            throws ServletException, IOException {
        TraceIdFilter filter = new TraceIdFilter(properties(), new TraceIdService());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Trace-Id", "incoming-trace-123");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("incoming-trace-123");
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void givenInvalidIncomingTraceIdWhenRequestIsFilteredThenNewTraceIdIsGenerated()
            throws ServletException, IOException {
        TraceIdFilter filter = new TraceIdFilter(properties(), new TraceIdService());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Trace-Id", "bad id with spaces");

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Trace-Id"))
                .isNotBlank()
                .isNotEqualTo("bad id with spaces");
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void givenNoIncomingTraceIdWhenRequestIsFilteredThenTraceIdIsGenerated()
            throws ServletException, IOException {
        TraceIdFilter filter = new TraceIdFilter(properties(), new TraceIdService());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Trace-Id")).matches(TraceIdService.TRACE_ID_PATTERN);
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void givenUnsafeIncomingTraceIdsWhenRequestIsFilteredThenValuesAreReplaced()
            throws ServletException, IOException {
        assertTraceIdIsReplaced("");
        assertTraceIdIsReplaced(" ".repeat(8));
        assertTraceIdIsReplaced("a".repeat(TraceIdService.MAX_TRACE_ID_LENGTH + 1));
        assertTraceIdIsReplaced("bad\ntrace");
        assertTraceIdIsReplaced("bad\u0001trace");
        assertTraceIdIsReplaced("bad@trace");
    }

    @Test
    void givenFilterChainThrowsWhenRequestIsFilteredThenMdcIsCleared() {
        TraceIdFilter filter = new TraceIdFilter(properties(), new TraceIdService());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "exception-trace");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            throw new ServletException("boom");
        };

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(ServletException.class);
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("exception-trace");
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void givenPreviousRequestTraceIdWhenNextRequestRunsThenTraceIdDoesNotLeak()
            throws ServletException, IOException {
        TraceIdFilter filter = new TraceIdFilter(properties(), new TraceIdService());
        MockHttpServletRequest firstRequest = new MockHttpServletRequest();
        firstRequest.addHeader("X-Trace-Id", "first-trace");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = new MockHttpServletRequest();
        secondRequest.addHeader("X-Trace-Id", "second-trace");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertThat(firstResponse.getHeader("X-Trace-Id")).isEqualTo("first-trace");
        assertThat(secondResponse.getHeader("X-Trace-Id")).isEqualTo("second-trace");
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }

    private void assertTraceIdIsReplaced(String incomingTraceId) throws ServletException, IOException {
        TraceIdFilter filter = new TraceIdFilter(properties(), new TraceIdService());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", incomingTraceId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("X-Trace-Id"))
                .isNotBlank()
                .isNotEqualTo(incomingTraceId)
                .matches(TraceIdService.TRACE_ID_PATTERN);
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
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
