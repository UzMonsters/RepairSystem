package com.example.darks.repair_auto.shared.error;

public record FieldErrorResponse(
        String field,
        String code,
        String message
) {
}
