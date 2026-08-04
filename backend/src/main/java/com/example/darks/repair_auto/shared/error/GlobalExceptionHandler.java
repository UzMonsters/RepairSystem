package com.example.darks.repair_auto.shared.error;

import com.example.darks.repair_auto.shared.observability.TraceIdFilter;
import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldErrorResponse> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_FAILED.name(),
                "Request validation failed.",
                request,
                fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<FieldErrorResponse> fieldErrors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new FieldErrorResponse(
                        violation.getPropertyPath().toString(),
                        violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
                        violation.getMessage()))
                .toList();
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_FAILED.name(),
                "Request validation failed.",
                request,
                fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_REQUEST_BODY.name(),
                "Request body is missing or malformed.",
                request,
                List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_REQUEST_PARAMETER.name(),
                "Request parameter has an invalid value.",
                request,
                List.of(new FieldErrorResponse(
                        exception.getName(),
                        "TypeMismatch",
                        "Invalid value for parameter '" + exception.getName() + "'.")));
    }

    @ExceptionHandler(InvalidRequestParameterException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidRequestParameter(
            InvalidRequestParameterException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_REQUEST_PARAMETER.name(),
                exception.getMessage(),
                request,
                exception.fieldErrors());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.MISSING_REQUEST_PARAMETER.name(),
                "Required request parameter is missing.",
                request,
                List.of(new FieldErrorResponse(
                        exception.getParameterName(),
                        "MissingParameter",
                        "Required parameter '" + exception.getParameterName() + "' is missing.")));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.METHOD_NOT_ALLOWED,
                ApiErrorCode.METHOD_NOT_ALLOWED.name(),
                "HTTP method is not supported for this endpoint.",
                request,
                List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ApiErrorCode.UNSUPPORTED_MEDIA_TYPE.name(),
                "Request media type is not supported.",
                request,
                List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.ATTACHMENT_FILE_TOO_LARGE.name(),
                "Attachment file exceeds the configured limit.",
                request,
                List.of());
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ApiErrorResponse> handleMultipart(
            MultipartException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_REQUEST_BODY.name(),
                "Multipart request is missing or malformed.",
                request,
                List.of());
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ApiErrorResponse> handleNotFound(Exception exception, HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.RESOURCE_NOT_FOUND.name(),
                "Requested resource was not found.",
                request,
                List.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiErrorResponse> handleBusinessRule(
            BusinessRuleException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.valueOf(exception.status()),
                exception.code(),
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiErrorResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.CONFLICT,
                ApiErrorCode.OPTIMISTIC_LOCK_CONFLICT.name(),
                "The record was changed by another request. Reload and try again.",
                request,
                List.of());
    }

    @ExceptionHandler(TelegramApiException.class)
    ResponseEntity<ApiErrorResponse> handleTelegramApi(
            TelegramApiException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.SERVICE_UNAVAILABLE,
                "TELEGRAM_API_UNAVAILABLE",
                "Telegram service is temporarily unavailable.",
                request,
                List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected request failure. traceId={}", traceId(), exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_ERROR.name(),
                "Unexpected internal error.",
                request,
                List.of());
    }

    private FieldErrorResponse toFieldError(FieldError error) {
        return new FieldErrorResponse(
                error.getField(),
                error.getCode(),
                error.getDefaultMessage());
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<FieldErrorResponse> fieldErrors) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        OffsetDateTime.now(ZoneOffset.UTC),
                        status.value(),
                        code,
                        message,
                        request.getRequestURI(),
                        traceId(),
                        fieldErrors));
    }

    private String traceId() {
        return MDC.get(TraceIdFilter.MDC_KEY);
    }
}
