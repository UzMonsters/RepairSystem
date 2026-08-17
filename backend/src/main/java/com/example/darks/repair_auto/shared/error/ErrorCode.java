package com.example.darks.repair_auto.shared.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common & System Errors
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "common.internal-error"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "common.internal-error"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "common.validation-failed"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "common.validation-failed"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "common.resource-not-found"),
    BUSINESS_RULE_VIOLATION(HttpStatus.CONFLICT, "common.business-rule-violation"),
    OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT, "common.optimistic-lock-conflict"),
    RESOURCE_VERSION_CONFLICT(HttpStatus.CONFLICT, "common.resource-version-conflict"),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "common.data-integrity-violation"),

    // Security & Auth Errors
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "security.authentication-required"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "security.access-denied"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "security.invalid-token"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "security.expired-token"),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "security.invalid-access-token"),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "security.access-token-expired"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "security.invalid-refresh-token"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "security.refresh-token-expired"),
    REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "security.refresh-token-revoked"),
    REFRESH_TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "security.refresh-token-reuse-detected"),
    SESSION_REVOKED(HttpStatus.UNAUTHORIZED, "security.session-revoked"),

    // Auth & Password Errors
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "auth.invalid-credentials"),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "auth.invalid-current-password"),
    PASSWORD_REUSE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "auth.password-reuse-not-allowed"),
    PASSWORD_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "auth.password-policy-violation"),
    PASSWORD_CONFIRMATION_MISMATCH(HttpStatus.BAD_REQUEST, "auth.password-confirmation-mismatch"),
    NEW_PASSWORD_SAME_AS_CURRENT(HttpStatus.BAD_REQUEST, "auth.new-password-same-as-current"),

    // User Management
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "user.not-found"),
    USER_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "user.email-already-exists"),
    USER_DISABLED(HttpStatus.FORBIDDEN, "user.disabled"),
    LAST_ACTIVE_ADMIN_REQUIRED(HttpStatus.CONFLICT, "user.last-active-admin-required"),
    SELF_DISABLE_NOT_ALLOWED(HttpStatus.CONFLICT, "user.self-disable-not-allowed"),

    // Customer
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "customer.not-found"),
    CUSTOMER_PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, "customer.phone-already-exists"),
    CUSTOMER_TELEGRAM_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "customer.telegram-id-already-exists"),
    CUSTOMER_INACTIVE(HttpStatus.BAD_REQUEST, "customer.inactive"),

    // Technician
    TECHNICIAN_NOT_FOUND(HttpStatus.NOT_FOUND, "technician.not-found"),
    TECHNICIAN_PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, "technician.phone-already-exists"),
    TECHNICIAN_TELEGRAM_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "technician.telegram-id-already-exists"),
    TECHNICIAN_INACTIVE(HttpStatus.BAD_REQUEST, "technician.inactive"),
    INVALID_MAXIMUM_CONCURRENT_REQUESTS(HttpStatus.BAD_REQUEST, "technician.invalid-maximum-concurrent-requests"),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "category.not-found"),
    CATEGORY_NAME_EN_ALREADY_EXISTS(HttpStatus.CONFLICT, "category.name-en-already-exists"),
    CATEGORY_NAME_UZ_ALREADY_EXISTS(HttpStatus.CONFLICT, "category.name-uz-already-exists"),
    CATEGORY_NAME_RU_ALREADY_EXISTS(HttpStatus.CONFLICT, "category.name-ru-already-exists"),
    CATEGORY_INACTIVE(HttpStatus.BAD_REQUEST, "category.inactive"),
    INVALID_CATEGORY_ORDER(HttpStatus.BAD_REQUEST, "category.invalid-order"),

    // Repair Requests
    REPAIR_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "repair.request.not-found"),
    REPAIR_REQUEST_NOT_EDITABLE(HttpStatus.CONFLICT, "repair.request.not-editable"),
    REPAIR_REQUEST_NOT_ASSIGNABLE(HttpStatus.CONFLICT, "repair.request.not-assignable"),
    REPAIR_REQUEST_CUSTOMER_INACTIVE(HttpStatus.BAD_REQUEST, "repair.request.customer-inactive"),
    REPAIR_REQUEST_CATEGORY_INACTIVE(HttpStatus.BAD_REQUEST, "repair.request.category-inactive"),
    INVALID_REPAIR_REQUEST_STATUS(HttpStatus.CONFLICT, "repair.request.invalid-status"),
    INVALID_REPAIR_REQUEST_DESCRIPTION(HttpStatus.BAD_REQUEST, "repair.request.invalid-description"),
    INVALID_REPAIR_REQUEST_LOCATION(HttpStatus.BAD_REQUEST, "repair.request.invalid-location"),
    INVALID_PREFERRED_VISIT_TIME(HttpStatus.BAD_REQUEST, "repair.request.invalid-preferred-visit-time"),
    INVALID_REQUEST_DATE_RANGE(HttpStatus.BAD_REQUEST, "repair.request.invalid-date-range"),

    // Assignments
    ACTIVE_ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "repair.assignment.active-not-found"),
    REPAIR_REQUEST_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "repair.assignment.technician-already-assigned"),
    TECHNICIAN_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "repair.assignment.technician-already-assigned"),
    TECHNICIAN_CAPACITY_EXCEEDED(HttpStatus.CONFLICT, "repair.assignment.technician-capacity-exceeded"),
    ASSIGNMENT_ALREADY_ACCEPTED(HttpStatus.CONFLICT, "repair.assignment.already-accepted"),
    ASSIGNMENT_ALREADY_REJECTED(HttpStatus.CONFLICT, "repair.assignment.already-rejected"),
    ASSIGNMENT_NOT_PENDING(HttpStatus.CONFLICT, "repair.assignment.not-pending"),
    INVALID_SCHEDULED_VISIT_TIME(HttpStatus.BAD_REQUEST, "repair.assignment.invalid-scheduled-visit-time"),
    ASSIGNMENT_CONFLICT(HttpStatus.CONFLICT, "repair.assignment.conflict"),
    REQUEST_NOT_SCHEDULABLE(HttpStatus.CONFLICT, "repair.assignment.not-schedulable"),

    // Executions
    REPAIR_EXECUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "repair.execution.not-found"),
    REPAIR_ALREADY_STARTED(HttpStatus.CONFLICT, "repair.execution.already-started"),
    REPAIR_NOT_STARTABLE(HttpStatus.CONFLICT, "repair.execution.not-startable"),
    REPAIR_NOT_IN_PROGRESS(HttpStatus.CONFLICT, "repair.execution.not-in-progress"),
    REPAIR_NOT_WAITING_FOR_PARTS(HttpStatus.CONFLICT, "repair.execution.not-waiting-for-parts"),
    REPAIR_ALREADY_COMPLETED(HttpStatus.CONFLICT, "repair.execution.already-completed"),
    REPAIR_ALREADY_CANCELLED(HttpStatus.CONFLICT, "repair.execution.already-cancelled"),
    DIAGNOSIS_REQUIRED(HttpStatus.BAD_REQUEST, "repair.execution.diagnosis-required"),
    INVALID_DIAGNOSIS(HttpStatus.BAD_REQUEST, "repair.execution.invalid-diagnosis"),
    WORK_PERFORMED_REQUIRED(HttpStatus.BAD_REQUEST, "repair.execution.work-performed-required"),
    INVALID_WAITING_REASON(HttpStatus.BAD_REQUEST, "repair.execution.invalid-waiting-reason"),
    INVALID_CANCELLATION_REASON(HttpStatus.BAD_REQUEST, "repair.execution.invalid-cancellation-reason"),
    ACTIVE_ACCEPTED_ASSIGNMENT_REQUIRED(HttpStatus.CONFLICT, "repair.execution.active-accepted-assignment-required"),
    REPAIR_EXECUTION_CONFLICT(HttpStatus.CONFLICT, "repair.execution.conflict"),
    INVALID_REPAIR_STATUS_TRANSITION(HttpStatus.CONFLICT, "repair.execution.invalid-status-transition"),

    // Attachments
    ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "attachment.not-found"),
    ATTACHMENT_EMPTY(HttpStatus.BAD_REQUEST, "attachment.empty"),
    ATTACHMENT_FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "attachment.too-large"),
    ATTACHMENT_CONTENT_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "attachment.content-type-not-allowed"),
    ATTACHMENT_CONTENT_MISMATCH(HttpStatus.BAD_REQUEST, "attachment.content-mismatch"),
    ATTACHMENT_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "attachment.type-not-allowed"),
    ATTACHMENT_UPLOAD_NOT_ALLOWED(HttpStatus.CONFLICT, "attachment.upload-not-allowed"),
    ATTACHMENT_DELETE_NOT_ALLOWED(HttpStatus.CONFLICT, "attachment.delete-not-allowed"),
    ATTACHMENT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "attachment.limit-exceeded"),
    ATTACHMENT_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "attachment.not-available"),
    ATTACHMENT_STORAGE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "attachment.storage-failed"),
    ATTACHMENT_CONFLICT(HttpStatus.CONFLICT, "attachment.conflict"),
    ATTACHMENT_ALREADY_DELETED(HttpStatus.CONFLICT, "attachment.already-deleted"),
    COMPLETION_PHOTO_REQUIRED(HttpStatus.BAD_REQUEST, "attachment.completion-photo-required"),
    PART_REQUIRED(HttpStatus.BAD_REQUEST, "attachment.part-required"),

    // Telegram & Security Aliases
    TELEGRAM_LINK_INVALID(HttpStatus.BAD_REQUEST, "telegram.link.invalid"),
    TELEGRAM_LINK_EXPIRED(HttpStatus.BAD_REQUEST, "telegram.link.expired"),
    TELEGRAM_ALREADY_LINKED(HttpStatus.CONFLICT, "telegram.already-linked"),
    TECHNICIAN_TELEGRAM_NOT_CONNECTED(HttpStatus.BAD_REQUEST, "telegram.technician-not-connected"),
    CUSTOMER_TELEGRAM_NOT_CONNECTED(HttpStatus.BAD_REQUEST, "telegram.customer-not-connected"),
    TELEGRAM_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "telegram.api-unavailable"),
    TECHNICIAN_ASSIGNMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "security.access-denied"),
    REPAIR_CATEGORY_INACTIVE(HttpStatus.BAD_REQUEST, "category.inactive"),
    REPAIR_REQUEST_DESCRIPTION_INVALID(HttpStatus.BAD_REQUEST, "repair.request.invalid-description"),
    REPAIR_REQUEST_LOCATION_REQUIRED(HttpStatus.BAD_REQUEST, "repair.request.invalid-location"),
    REPAIR_REQUEST_LOCATION_INVALID(HttpStatus.BAD_REQUEST, "repair.request.invalid-location"),
    TELEGRAM_CUSTOMER_LINK_CONFLICT(HttpStatus.CONFLICT, "customer.telegram-id-already-exists"),
    TELEGRAM_CUSTOMER_ARCHIVED(HttpStatus.BAD_REQUEST, "customer.inactive"),

    // Protocol & Request
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "request.body.invalid-json"),
    REQUEST_BODY_REQUIRED(HttpStatus.BAD_REQUEST, "request.body.required"),
    INVALID_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "request.parameter.invalid-value"),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "request.parameter.required"),
    INVALID_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "request.parameter.invalid-type"),
    INVALID_ENUM_VALUE(HttpStatus.BAD_REQUEST, "request.body.invalid-enum"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "request.method-not-supported"),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "request.media-type-not-supported"),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "validation.phone.invalid");

    private final HttpStatus status;
    private final String messageKey;

    ErrorCode(HttpStatus status, String messageKey) {
        this.status = status;
        this.messageKey = messageKey;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
