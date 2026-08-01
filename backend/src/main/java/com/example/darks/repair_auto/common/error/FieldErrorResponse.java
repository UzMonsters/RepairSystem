package com.example.darks.repair_auto.common.error;

public record FieldErrorResponse(
        String field,
        String code,
        String message
) {
}
