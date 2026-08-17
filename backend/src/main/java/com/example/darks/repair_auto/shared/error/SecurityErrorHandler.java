package com.example.darks.repair_auto.shared.error;

import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.shared.i18n.LocalizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final String traceHeaderName;
    private final LocalizationService localizationService;
    private final ApiErrorResponseFactory responseFactory;
    private final ObjectMapper objectMapper;

    public SecurityErrorHandler(
            AppProperties properties,
            LocalizationService localizationService,
            ApiErrorResponseFactory responseFactory,
            ObjectMapper objectMapper) {
        this.traceHeaderName = properties.trace().headerName();
        this.localizationService = localizationService;
        this.responseFactory = responseFactory;
        this.objectMapper = configureMapper(objectMapper);
    }

    private static ObjectMapper configureMapper(ObjectMapper base) {
        ObjectMapper mapper = base != null ? base.copy() : new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.AUTHENTICATION_REQUIRED;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        writeResponse(response, request, errorCode, message);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        String message = localizationService.get(errorCode.getMessageKey(), request);
        writeResponse(response, request, errorCode, message);
    }

    public void writeUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            String code,
            String messageKey) throws IOException {
        ErrorCode errorCode;
        try {
            errorCode = ErrorCode.valueOf(code);
        } catch (Exception e) {
            errorCode = ErrorCode.AUTHENTICATION_REQUIRED;
        }
        String localizedMessage = localizationService.get(messageKey != null ? messageKey : errorCode.getMessageKey(), request);
        writeResponse(response, request, errorCode, localizedMessage);
    }

    private void writeResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            ErrorCode errorCode,
            String message) throws IOException {
        ApiErrorResponse apiError = responseFactory.create(errorCode, message, request);
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(traceHeaderName, apiError.traceId());
        objectMapper.writeValue(response.getWriter(), apiError);
    }
}
