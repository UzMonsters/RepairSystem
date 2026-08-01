package com.example.darks.repair_auto.common.error;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId,
        List<FieldErrorResponse> fieldErrors
) {
}
