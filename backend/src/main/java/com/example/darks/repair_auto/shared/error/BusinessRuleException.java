package com.example.darks.repair_auto.shared.error;

public class BusinessRuleException extends BusinessException {

    public BusinessRuleException(ErrorCode errorCode, Object... arguments) {
        super(errorCode, arguments);
    }

    public BusinessRuleException(String code, String message) {
        this(resolveErrorCode(code));
    }

    public BusinessRuleException(String code, String message, int status) {
        this(resolveErrorCode(code));
    }

    private static ErrorCode resolveErrorCode(String code) {
        if (code != null) {
            try {
                return ErrorCode.valueOf(code);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ErrorCode.BUSINESS_RULE_VIOLATION;
    }
}
