package com.example.darks.repair_auto.identity.mobile.otp;

import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EskizClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(EskizClient.class);
    private static final Duration TOKEN_VALIDITY_SAFETY_WINDOW = Duration.ofDays(25);

    private final EskizProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AtomicReference<CachedToken> cachedTokenRef = new AtomicReference<>();

    @org.springframework.beans.factory.annotation.Autowired
    public EskizClient(EskizProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build());
    }

    EskizClient(EskizProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public void sendSms(String recipientPhone, String messageText) {
        if (properties.getEmail() == null || properties.getEmail().isBlank()
                || properties.getPassword() == null || properties.getPassword().isBlank()) {
            LOGGER.error("Eskiz credentials (email or password) are not configured.");
            throw new BusinessException(ErrorCode.SMS_DELIVERY_FAILED);
        }

        String normalizedPhone = normalizePhone(recipientPhone);
        String maskedPhone = maskPhone(recipientPhone);

        LOGGER.info("Sending SMS via Eskiz provider to recipient={}", maskedPhone);

        String token = getOrRefreshToken();
        boolean success = doSendSms(token, normalizedPhone, messageText, maskedPhone);
        if (!success) {
            LOGGER.warn("Eskiz token expired or rejected. Attempting re-authentication and single retry.");
            cachedTokenRef.set(null);
            token = authenticate();
            boolean retrySuccess = doSendSms(token, normalizedPhone, messageText, maskedPhone);
            if (!retrySuccess) {
                LOGGER.error("Eskiz SMS dispatch failed after retry for recipient={}", maskedPhone);
                throw new BusinessException(ErrorCode.SMS_DELIVERY_FAILED);
            }
        }
        LOGGER.info("Eskiz SMS dispatched successfully to recipient={}", maskedPhone);
    }

    private boolean doSendSms(String token, String normalizedPhone, String messageText, String maskedPhone) {
        try {
            String url = properties.getBaseUrl() + "/api/message/sms/send";
            String requestJson = objectMapper.writeValueAsString(Map.of(
                    "mobile_phone", normalizedPhone,
                    "message", messageText,
                    "from", properties.getFromName()));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(properties.getReadTimeout())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 200 || statusCode == 201) {
                return true;
            }
            if (statusCode == 401) {
                return false;
            }
            if (statusCode == 429) {
                LOGGER.warn("Eskiz SMS provider rate limited request for recipient={}", maskedPhone);
                throw new BusinessException(ErrorCode.SMS_RATE_LIMITED);
            }
            if (statusCode >= 500) {
                LOGGER.error("Eskiz SMS provider server error status={} for recipient={}", statusCode, maskedPhone);
                throw new BusinessException(ErrorCode.SMS_PROVIDER_UNAVAILABLE);
            }

            LOGGER.error("Eskiz SMS provider rejected send with status={} for recipient={}", statusCode, maskedPhone);
            throw new BusinessException(ErrorCode.SMS_DELIVERY_FAILED);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            LOGGER.error("I/O failure communicating with Eskiz SMS provider: {}", exception.getMessage());
            throw new BusinessException(ErrorCode.SMS_PROVIDER_UNAVAILABLE);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.error("Thread interrupted during Eskiz SMS dispatch: {}", exception.getMessage());
            throw new BusinessException(ErrorCode.SMS_PROVIDER_UNAVAILABLE);
        } catch (Exception exception) {
            LOGGER.error("Unexpected error during Eskiz SMS dispatch: {}", exception.getMessage());
            throw new BusinessException(ErrorCode.SMS_DELIVERY_FAILED);
        }
    }

    private String getOrRefreshToken() {
        CachedToken cached = cachedTokenRef.get();
        if (cached != null && cached.isValid()) {
            return cached.token();
        }
        return authenticate();
    }

    private synchronized String authenticate() {
        CachedToken cached = cachedTokenRef.get();
        if (cached != null && cached.isValid()) {
            return cached.token();
        }

        try {
            String url = properties.getBaseUrl() + "/api/auth/login";
            String requestJson = objectMapper.writeValueAsString(Map.of(
                    "email", properties.getEmail(),
                    "password", properties.getPassword()));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(properties.getReadTimeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode != 200 && statusCode != 201) {
                LOGGER.error("Eskiz authentication failed with status code={}", statusCode);
                throw new BusinessException(statusCode >= 500 ? ErrorCode.SMS_PROVIDER_UNAVAILABLE : ErrorCode.SMS_DELIVERY_FAILED);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode dataNode = root.path("data");
            String token = dataNode.path("token").asText(null);

            if (token == null || token.isBlank()) {
                LOGGER.error("Eskiz authentication response missing token in payload.");
                throw new BusinessException(ErrorCode.SMS_DELIVERY_FAILED);
            }

            CachedToken newCached = new CachedToken(token, Instant.now().plus(TOKEN_VALIDITY_SAFETY_WINDOW));
            cachedTokenRef.set(newCached);
            return token;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            LOGGER.error("I/O error during Eskiz authentication: {}", exception.getMessage());
            throw new BusinessException(ErrorCode.SMS_PROVIDER_UNAVAILABLE);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.error("Interrupted during Eskiz authentication: {}", exception.getMessage());
            throw new BusinessException(ErrorCode.SMS_PROVIDER_UNAVAILABLE);
        } catch (Exception exception) {
            LOGGER.error("Failed to authenticate with Eskiz provider: {}", exception.getMessage());
            throw new BusinessException(ErrorCode.SMS_DELIVERY_FAILED);
        }
    }

    String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 9) {
            return "998" + digits;
        }
        return digits;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        String normalized = phone.trim();
        int len = normalized.length();
        return normalized.substring(0, Math.min(6, len)) + " *** ** " + normalized.substring(Math.max(0, len - 2));
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return token != null && !token.isBlank() && Instant.now().isBefore(expiresAt);
        }
    }
}
