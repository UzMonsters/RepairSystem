package com.example.darks.repair_auto.telegram.core.infrastructure;

import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

class HttpTelegramBotClient implements TelegramBotClient {

    private final TelegramProperties properties;
    private final RestClient restClient;

    HttpTelegramBotClient(TelegramProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public void sendMessage(Long chatId, String text, String replyMarkupJson) {
        Map<String, Object> body = replyMarkupJson == null || replyMarkupJson.isBlank()
                ? Map.of("chat_id", chatId, "text", text)
                : Map.of("chat_id", chatId, "text", text, "reply_markup", replyMarkupJson);
        try {
            TelegramResponse<?> response = restClient.post()
                    .uri(apiUri("sendMessage"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(TelegramResponse.class);
            validate(response);
        } catch (RestClientException exception) {
            throw new TelegramApiException("Telegram API request failed.", exception);
        }
    }

    @Override
    public void answerCallback(String callbackQueryId, String text) {
        try {
            TelegramResponse<?> response = restClient.post()
                    .uri(apiUri("answerCallbackQuery"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("callback_query_id", callbackQueryId, "text", text == null ? "" : text))
                    .retrieve()
                    .body(TelegramResponse.class);
            validate(response);
        } catch (RestClientException exception) {
            throw new TelegramApiException("Telegram API request failed.", exception);
        }
    }

    @Override
    public TelegramFileMetadata getFile(String fileId) {
        try {
            TelegramFileResponse response = restClient.post()
                    .uri(apiUri("getFile"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("file_id", fileId))
                    .retrieve()
                    .body(TelegramFileResponse.class);
            validateFile(response);
            TelegramFileResult result = response.result();
            return new TelegramFileMetadata(
                    result.fileId(),
                    result.filePath(),
                    result.fileSize() == null ? 0 : result.fileSize());
        } catch (RestClientException exception) {
            throw new TelegramApiException("Telegram API request failed.", exception);
        }
    }

    @Override
    public InputStream downloadFile(String filePath, long maxSizeBytes) {
        try {
            Resource resource = restClient.get()
                    .uri(fileUri(filePath))
                    .retrieve()
                    .body(Resource.class);
            if (resource == null) {
                throw new TelegramApiException("Telegram file download failed.");
            }
            return new BoundedInputStream(resource.getInputStream(), maxSizeBytes);
        } catch (IOException | RestClientException exception) {
            throw new TelegramApiException("Telegram file download failed.", exception);
        }
    }

    private URI apiUri(String method) {
        return UriComponentsBuilder.fromUri(properties.getApiBaseUrl())
                .pathSegment("bot" + properties.getBotToken(), method)
                .build()
                .toUri();
    }

    private URI fileUri(String filePath) {
        return UriComponentsBuilder.fromUri(properties.getFileBaseUrl())
                .pathSegment("file", "bot" + properties.getBotToken())
                .path(filePath.startsWith("/") ? filePath : "/" + filePath)
                .build()
                .toUri();
    }

    private void validate(TelegramResponse<?> response) {
        if (response == null || !response.ok()) {
            throw new TelegramApiException("Telegram API request failed.");
        }
    }

    private void validateFile(TelegramFileResponse response) {
        if (response == null || !response.ok() || response.result() == null) {
            throw new TelegramApiException("Telegram API request failed.");
        }
    }

    private record TelegramResponse<T>(boolean ok, T result) {
    }

    private record TelegramFileResponse(boolean ok, TelegramFileResult result) {
    }

    private record TelegramFileResult(
            @JsonProperty("file_id") String fileId,
            @JsonProperty("file_path") String filePath,
            @JsonProperty("file_size") Long fileSize) {
    }

    private static final class BoundedInputStream extends FilterInputStream {

        private final long maxBytes;
        private long bytesRead;

        private BoundedInputStream(InputStream inputStream, long maxBytes) {
            super(inputStream);
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = super.read(buffer, offset, length);
            if (count > 0) {
                count(count);
            }
            return count;
        }

        private void count(long increment) {
            bytesRead += increment;
            if (bytesRead > maxBytes) {
                throw new TelegramApiException("Telegram file exceeds allowed size.");
            }
        }
    }
}
