package com.example.darks.repair_auto.shared.error;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] arguments;
    private final boolean explicitMessage;

    public BusinessException(ErrorCode errorCode, Object... arguments) {
        this(errorCode, defaultMessage(errorCode), false, arguments);
    }

    public BusinessException(ErrorCode errorCode, String message, Object... arguments) {
        this(errorCode, message != null ? message : defaultMessage(errorCode), message != null, arguments);
    }

    private BusinessException(ErrorCode errorCode, String message, boolean explicitMessage, Object... arguments) {
        super(message);
        this.errorCode = errorCode;
        this.explicitMessage = explicitMessage;
        this.arguments = arguments != null ? arguments : new Object[0];
    }

    public boolean hasExplicitMessage() {
        return explicitMessage;
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

    private static String defaultMessage(ErrorCode errorCode) {
        if (errorCode == null) {
            return "Business rule violation occurred.";
        }
        return switch (errorCode) {
            case USER_NOT_FOUND -> "User was not found.";
            case INVALID_CURRENT_PASSWORD -> "Current password is invalid.";
            case PASSWORD_CONFIRMATION_MISMATCH -> "Password and confirmation do not match.";
            case PASSWORD_REUSE_NOT_ALLOWED, NEW_PASSWORD_SAME_AS_CURRENT -> "New password must differ from the current password.";
            case INVALID_CREDENTIALS -> "Username or password is incorrect.";
            case CUSTOMER_NOT_FOUND -> "Customer was not found.";
            case TECHNICIAN_NOT_FOUND -> "Technician was not found.";
            case CATEGORY_NOT_FOUND -> "Repair category was not found.";
            case REPAIR_REQUEST_NOT_FOUND -> "Repair request was not found.";
            case REPAIR_EXECUTION_NOT_FOUND -> "Repair execution was not found.";
            case NOTIFICATION_NOT_FOUND -> "Notification was not found.";
            case REVIEW_NOT_FOUND -> "Review was not found.";
            case ACCESS_DENIED -> "Access denied.";
            case AUTHENTICATION_REQUIRED -> "Authentication is required.";
            default -> errorCode.name();
        };
    }
}
