package com.example.darks.repair_auto.shared.error;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String traceId,
        List<FieldErrorResponse> fieldErrors
) {
    public ApiErrorResponse(
            OffsetDateTime timestamp,
            int status,
            String code,
            String message,
            String path,
            String traceId,
            List<FieldErrorResponse> fieldErrors) {
        this(
                timestamp,
                status,
                resolveReasonPhrase(status),
                code,
                message,
                path,
                traceId,
                fieldErrors != null ? fieldErrors : List.of()
        );
    }

    private static String resolveReasonPhrase(int status) {
        try {
            return HttpStatus.valueOf(status).getReasonPhrase();
        } catch (Exception e) {
            return "Error";
        }
    }
}
