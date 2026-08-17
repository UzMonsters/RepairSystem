package com.example.darks.repair_auto.shared.error;

import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final LocalizationService localizationService;
    private final ApiErrorResponseFactory responseFactory;

    public GlobalExceptionHandler(LocalizationService localizationService, ApiErrorResponseFactory responseFactory) {
        this.localizationService = localizationService;
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = exception.getErrorCode();
        String message = localizationService.get(errorCode.getMessageKey(), request, exception.getArguments());
        ApiErrorResponse response = responseFactory.create(errorCode, message, request);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> toFieldError(fieldError, request))
                .toList();

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request, fieldErrors);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations()
                .stream()
                .map(violation -> {
                    String fieldPath = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "";
                    String fieldName = fieldPath.contains(".") ? fieldPath.substring(fieldPath.lastIndexOf('.') + 1) : fieldPath;
                    String messageKey = violation.getMessage();
                    String localizedMsg = localizationService.get(messageKey, request);
                    String constraintCode = violation.getConstraintDescriptor() != null && violation.getConstraintDescriptor().getAnnotation() != null
                            ? violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName().toUpperCase()
                            : "INVALID";
                    return new FieldErrorResponse(fieldName, constraintCode, localizedMsg);
                })
                .toList();

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request, fieldErrors);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        Throwable cause = exception.getCause();
        if (cause instanceof InvalidFormatException invalidFormat) {
            String fieldName = invalidFormat.getPath().isEmpty() ? "field" : invalidFormat.getPath().get(invalidFormat.getPath().size() - 1).getFieldName();
            ErrorCode errorCode = ErrorCode.INVALID_ENUM_VALUE;
            String message = localizationService.get(errorCode.getMessageKey(), request, fieldName);
            ApiErrorResponse response = responseFactory.create(errorCode, message, request);
            return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
        }

        String msg = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        if (msg.contains("request body is missing")) {
            ErrorCode errorCode = ErrorCode.REQUEST_BODY_REQUIRED;
            String message = localizationService.get(errorCode.getMessageKey(), request);
            ApiErrorResponse response = responseFactory.create(errorCode, message, request);
            return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
        }

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_BODY;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_PARAMETER_TYPE;
        String paramName = exception.getName();
        String message = localizationService.get(errorCode.getMessageKey(), request, paramName);
        FieldErrorResponse fieldError = new FieldErrorResponse(paramName, "TYPE_MISMATCH", message);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request, List.of(fieldError));
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(InvalidRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequestParameter(
            InvalidRequestParameterException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request, exception.fieldErrors());
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.MISSING_REQUEST_PARAMETER;
        String paramName = exception.getParameterName();
        String message = localizationService.get(errorCode.getMessageKey(), request, paramName);
        FieldErrorResponse fieldError = new FieldErrorResponse(paramName, "MISSING_PARAMETER", message);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request, List.of(fieldError));
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(
            MissingServletRequestPartException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.PART_REQUIRED;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.ATTACHMENT_FILE_TOO_LARGE;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMultipart(
            MultipartException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_BODY;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            Exception exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.RESOURCE_VERSION_CONFLICT;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = mapDatabaseConstraintToErrorCode(exception);
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(TelegramApiException.class)
    public ResponseEntity<ApiErrorResponse> handleTelegramApi(
            TelegramApiException exception,
            HttpServletRequest request) {
        ErrorCode errorCode = ErrorCode.TELEGRAM_API_UNAVAILABLE;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request);
        return ResponseEntity.status(errorCode.getStatus()).contentType(org.springframework.http.MediaType.parseMediaType("application/json;charset=UTF-8")).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        String traceId = responseFactory.resolveTraceId();
        LOGGER.error("Unexpected request failure. traceId={}", traceId, exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        ApiErrorResponse response = responseFactory.create(errorCode, message, request);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    private FieldErrorResponse toFieldError(FieldError error, HttpServletRequest request) {
        String field = error.getField();
        String messageKey = error.getDefaultMessage();
        String localizedMsg = localizationService.get(messageKey, request);
        String code = error.getCode() != null ? error.getCode().toUpperCase() : "INVALID";
        return new FieldErrorResponse(field, code, localizedMsg);
    }

    private ErrorCode mapDatabaseConstraintToErrorCode(DataIntegrityViolationException exception) {
        String causeMessage = exception.getMostSpecificCause() != null ? exception.getMostSpecificCause().getMessage() : "";
        if (causeMessage != null) {
            String lower = causeMessage.toLowerCase();
            if (lower.contains("users_email") || lower.contains("email")) {
                return ErrorCode.USER_EMAIL_ALREADY_EXISTS;
            }
            if (lower.contains("customers_phone") || lower.contains("customer_phone")) {
                return ErrorCode.CUSTOMER_PHONE_ALREADY_EXISTS;
            }
            if (lower.contains("technicians_phone") || lower.contains("technician_phone")) {
                return ErrorCode.TECHNICIAN_PHONE_ALREADY_EXISTS;
            }
            if (lower.contains("telegram_chat_id") || lower.contains("telegram_user_id")) {
                return ErrorCode.TELEGRAM_ALREADY_LINKED;
            }
        }
        return ErrorCode.DATA_INTEGRITY_VIOLATION;
    }
}
