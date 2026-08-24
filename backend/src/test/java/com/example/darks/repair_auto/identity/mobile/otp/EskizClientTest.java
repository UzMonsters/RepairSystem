package com.example.darks.repair_auto.identity.mobile.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EskizClientTest {

    private EskizProperties properties;
    private ObjectMapper objectMapper;
    private HttpClient httpClient;
    private EskizClient client;

    @BeforeEach
    void setUp() {
        properties = EskizProperties.of(
                "https://notify.eskiz.uz",
                "test@eskiz.uz",
                "secretPassword",
                "4546",
                Duration.ofSeconds(5),
                Duration.ofSeconds(10));
        objectMapper = new ObjectMapper();
        httpClient = mock(HttpClient.class);
        client = new EskizClient(properties, objectMapper, httpClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendSms_success_authenticatesAndSendsMessage() throws Exception {
        HttpResponse<String> authResponse = mock(HttpResponse.class);
        when(authResponse.statusCode()).thenReturn(200);
        when(authResponse.body()).thenReturn("""
                {"message": "token_generated", "data": {"token": "sample-eskiz-token"}}
                """);

        HttpResponse<String> sendResponse = mock(HttpResponse.class);
        when(sendResponse.statusCode()).thenReturn(200);
        when(sendResponse.body()).thenReturn("""
                {"id": 12345, "status": "waiting", "message": "Waiting for SMS provider"}
                """);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(authResponse, sendResponse);

        client.sendSms("+998 90 123-45-67", "Your OTP is 123456");

        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendSms_tokenCached_doesNotReAuthenticate() throws Exception {
        HttpResponse<String> authResponse = mock(HttpResponse.class);
        when(authResponse.statusCode()).thenReturn(200);
        when(authResponse.body()).thenReturn("""
                {"message": "token_generated", "data": {"token": "sample-eskiz-token"}}
                """);

        HttpResponse<String> sendResponse1 = mock(HttpResponse.class);
        when(sendResponse1.statusCode()).thenReturn(200);
        when(sendResponse1.body()).thenReturn("""
                {"id": 101, "status": "waiting"}
                """);

        HttpResponse<String> sendResponse2 = mock(HttpResponse.class);
        when(sendResponse2.statusCode()).thenReturn(200);
        when(sendResponse2.body()).thenReturn("""
                {"id": 102, "status": "waiting"}
                """);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(authResponse, sendResponse1, sendResponse2);

        client.sendSms("+998901112233", "Message 1");
        client.sendSms("+998904445566", "Message 2");

        // Total 3 requests: 1 auth + 2 sends
        verify(httpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendSms_whenUnauthorized_refreshesTokenAndRetries() throws Exception {
        HttpResponse<String> authResponse1 = mock(HttpResponse.class);
        when(authResponse1.statusCode()).thenReturn(200);
        when(authResponse1.body()).thenReturn("""
                {"message": "token_generated", "data": {"token": "expired-eskiz-token"}}
                """);

        HttpResponse<String> sendResponse401 = mock(HttpResponse.class);
        when(sendResponse401.statusCode()).thenReturn(401);

        HttpResponse<String> authResponse2 = mock(HttpResponse.class);
        when(authResponse2.statusCode()).thenReturn(200);
        when(authResponse2.body()).thenReturn("""
                {"message": "token_generated", "data": {"token": "fresh-eskiz-token"}}
                """);

        HttpResponse<String> sendResponse200 = mock(HttpResponse.class);
        when(sendResponse200.statusCode()).thenReturn(200);
        when(sendResponse200.body()).thenReturn("""
                {"id": 201, "status": "waiting"}
                """);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(authResponse1, sendResponse401, authResponse2, sendResponse200);

        client.sendSms("+998901234567", "Your code is 654321");

        verify(httpClient, times(4)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendSms_rateLimited_throwsSmsRateLimitedException() throws Exception {
        HttpResponse<String> authResponse = mock(HttpResponse.class);
        when(authResponse.statusCode()).thenReturn(200);
        when(authResponse.body()).thenReturn("""
                {"data": {"token": "valid-token"}}
                """);

        HttpResponse<String> sendResponse = mock(HttpResponse.class);
        when(sendResponse.statusCode()).thenReturn(429);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(authResponse, sendResponse);

        assertThatThrownBy(() -> client.sendSms("+998901234567", "Rate limited msg"))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.SMS_RATE_LIMITED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendSms_serverError_throwsSmsProviderUnavailableException() throws Exception {
        HttpResponse<String> authResponse = mock(HttpResponse.class);
        when(authResponse.statusCode()).thenReturn(200);
        when(authResponse.body()).thenReturn("""
                {"data": {"token": "valid-token"}}
                """);

        HttpResponse<String> sendResponse = mock(HttpResponse.class);
        when(sendResponse.statusCode()).thenReturn(503);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(authResponse, sendResponse);

        assertThatThrownBy(() -> client.sendSms("+998901234567", "Unavailable msg"))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.SMS_PROVIDER_UNAVAILABLE);
    }

    @Test
    void normalizePhone_stripsNonDigitsAndHandlesUzbekPrefix() {
        assertThat(client.normalizePhone("+998 90 123-45-67")).isEqualTo("998901234567");
        assertThat(client.normalizePhone("998901234567")).isEqualTo("998901234567");
        assertThat(client.normalizePhone("901234567")).isEqualTo("998901234567");
    }
}
