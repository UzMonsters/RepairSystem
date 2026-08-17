package com.example.darks.repair_auto.shared.error;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] arguments;

    public BusinessException(ErrorCode errorCode, Object... arguments) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.arguments = arguments != null ? arguments : new Object[0];
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public String code() {
        return errorCode.name();
    }

    public int status() {
        return errorCode.getStatus().value();
    }
}
