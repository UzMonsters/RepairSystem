package com.example.darks.repair_auto.shared.error;

public class BusinessRuleException extends BusinessException {

    private final String customCode;
    private final Integer customStatus;

    public BusinessRuleException(ErrorCode errorCode) {
        super(errorCode);
        this.customCode = errorCode.name();
        this.customStatus = errorCode.getStatus().value();
    }

    public BusinessRuleException(ErrorCode errorCode, Object arg1) {
        super(errorCode, arg1);
        this.customCode = errorCode.name();
        this.customStatus = errorCode.getStatus().value();
    }

    public BusinessRuleException(ErrorCode errorCode, Object[] arguments) {
        super(errorCode, arguments);
        this.customCode = errorCode.name();
        this.customStatus = errorCode.getStatus().value();
    }

    public BusinessRuleException(String code, String message) {
        super(resolveErrorCode(code), message, true);
        this.customCode = code;
        this.customStatus = resolveErrorCode(code).getStatus().value();
    }

    public BusinessRuleException(String code, String message, int status) {
        super(resolveErrorCode(code), message, true);
        this.customCode = code;
        this.customStatus = status;
    }

    @Override
    public String code() {
        return customCode != null ? customCode : super.code();
    }

    @Override
    public int status() {
        return customStatus != null ? customStatus : super.status();
    }

    private static ErrorCode resolveErrorCode(String code) {
        if (code != null) {
            try {
                return ErrorCode.valueOf(code.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ErrorCode.BUSINESS_RULE_VIOLATION;
    }
}
