package com.example.darks.repair_auto.shared.error;

import java.util.List;

public class InvalidRequestParameterException extends RuntimeException {

    private final List<FieldErrorResponse> fieldErrors;

    public InvalidRequestParameterException(String field, String message) {
        super("Request parameter has an invalid value.");
        this.fieldErrors = List.of(new FieldErrorResponse(field, "InvalidRequestParameter", message));
    }

    public List<FieldErrorResponse> fieldErrors() {
        return fieldErrors;
    }
}
