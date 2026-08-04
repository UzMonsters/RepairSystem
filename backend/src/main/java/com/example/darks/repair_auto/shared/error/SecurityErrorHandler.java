package com.example.darks.repair_auto.shared.error;

import com.example.darks.repair_auto.shared.observability.TraceIdFilter;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final String traceHeaderName;
    private final TraceIdService traceIdService;

    public SecurityErrorHandler(AppProperties properties, TraceIdService traceIdService) {
        this.traceHeaderName = properties.trace().headerName();
        this.traceIdService = traceIdService;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        write(response, request, HttpStatus.UNAUTHORIZED, ApiErrorCode.AUTHENTICATION_REQUIRED.name(),
                "Authentication is required.");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        write(response, request, HttpStatus.FORBIDDEN, ApiErrorCode.ACCESS_DENIED.name(),
                "Access is denied.");
    }

    public void writeUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            String code,
            String message) throws IOException {
        write(response, request, HttpStatus.UNAUTHORIZED, code, message);
    }

    private void write(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message) throws IOException {
        String traceId = traceId();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(traceHeaderName, traceId);
        response.getWriter().write("""
                {"timestamp":"%s","status":%d,"code":"%s","message":"%s","path":"%s","traceId":"%s","fieldErrors":[]}
                """.formatted(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                json(code),
                json(message),
                json(request.getRequestURI()),
                json(traceId)));
    }

    private String traceId() {
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        if (traceIdService.isValid(traceId)) {
            return traceId;
        }
        return traceIdService.resolve(null);
    }

    private String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
