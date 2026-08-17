package com.example.darks.repair_auto.shared.error;

import com.example.darks.repair_auto.shared.observability.TraceIdFilter;
import com.example.darks.repair_auto.shared.observability.TraceIdService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorResponseFactory {

    private final TraceIdService traceIdService;

    public ApiErrorResponseFactory(TraceIdService traceIdService) {
        this.traceIdService = traceIdService;
    }

    public ApiErrorResponse create(
            ErrorCode errorCode,
            String localizedMessage,
            HttpServletRequest request) {
        return create(errorCode.getStatus(), errorCode.name(), localizedMessage, request, List.of());
    }

    public ApiErrorResponse create(
            ErrorCode errorCode,
            String localizedMessage,
            HttpServletRequest request,
            List<FieldErrorResponse> fieldErrors) {
        return create(errorCode.getStatus(), errorCode.name(), localizedMessage, request, fieldErrors);
    }

    public ApiErrorResponse create(
            HttpStatus status,
            String code,
            String localizedMessage,
            HttpServletRequest request,
            List<FieldErrorResponse> fieldErrors) {
        String path = request != null ? request.getRequestURI() : "";
        String traceId = resolveTraceId();
        return new ApiErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                code,
                localizedMessage,
                path,
                traceId,
                fieldErrors != null ? fieldErrors : List.of()
        );
    }

    public String resolveTraceId() {
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        if (traceIdService != null && traceIdService.isValid(traceId)) {
            return traceId;
        }
        if (traceIdService != null) {
            return traceIdService.resolve(null);
        }
        return traceId != null ? traceId : "unknown-trace";
    }
}
