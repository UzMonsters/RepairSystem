package com.example.darks.repair_auto.telegram.core.infrastructure;

import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import com.example.darks.repair_auto.telegram.core.application.TelegramMediaPhoto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

class HttpTelegramBotClient implements TelegramBotClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpTelegramBotClient.class);

    private final TelegramProperties properties;
    private final TelegramProperties.Bot bot;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    HttpTelegramBotClient(TelegramProperties properties, TelegramProperties.Bot bot, RestClient restClient) {
        this.properties = properties;
        this.bot = bot;
        this.restClient = restClient;
    }

    @Override
    public Long sendMessage(Long chatId, String text, String replyMarkupJson) {
        Map<String, Object> body = replyMarkupJson == null || replyMarkupJson.isBlank()
                ? Map.of("chat_id", chatId, "text", text)
                : Map.of("chat_id", chatId, "text", text, "reply_markup", replyMarkupJson);
        try {
            TelegramMessageResponse response = restClient.post()
                    .uri(apiUri("sendMessage"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(TelegramMessageResponse.class);
            validateMessage(response);
            return response.result() != null ? response.result().messageId() : null;
        } catch (RestClientException exception) {
            throw new TelegramApiException("Telegram API request failed.", exception);
        }
    }

    @Override
    public Long editMessage(Long chatId, Long messageId, String text, String replyMarkupJson) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("message_id", messageId);
        body.put("text", text);
        if (replyMarkupJson != null && !replyMarkupJson.isBlank()) {
            body.put("reply_markup", replyMarkupJson);
        }
        try {
            TelegramMessageResponse response = restClient.post()
                    .uri(apiUri("editMessageText"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(TelegramMessageResponse.class);
            validateMessage(response);
            return response.result() != null ? response.result().messageId() : messageId;
        } catch (RestClientResponseException exception) {
            String responseBody = exception.getResponseBodyAsString();
            if (responseBody != null && responseBody.toLowerCase(Locale.ROOT).contains("message is not modified")) {
                return messageId;
            }
            throw new TelegramApiException("Telegram API editMessage failed.", exception);
        } catch (RestClientException exception) {
            if (exception.getMessage() != null && exception.getMessage().toLowerCase(Locale.ROOT).contains("message is not modified")) {
                return messageId;
            }
            throw new TelegramApiException("Telegram API editMessage failed.", exception);
        }
    }

    @Override
    public void answerCallback(String callbackQueryId, String text) {
        answerCallback(callbackQueryId, text, false);
    }

    @Override
    public void answerCallback(String callbackQueryId, String text, boolean showAlert) {
        if (callbackQueryId == null || callbackQueryId.isBlank()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("callback_query_id", callbackQueryId);
        body.put("text", text == null ? "" : text);
        if (showAlert) {
            body.put("show_alert", true);
        }
        try {
            TelegramResponse<?> response = restClient.post()
                    .uri(apiUri("answerCallbackQuery"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(TelegramResponse.class);
            validate(response);
        } catch (RestClientException exception) {
            LOGGER.debug("Telegram answerCallbackQuery failed for id={}", callbackQueryId, exception);
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

    @Override
    public void sendPhoto(Long chatId, String filename, byte[] photoBytes, String caption) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);
        final String fname = (filename != null && !filename.isBlank()) ? filename : "photo.jpg";
        body.add("photo", new ByteArrayResource(photoBytes) {
            @Override
            public String getFilename() {
                return fname;
            }
        });
        if (caption != null && !caption.isBlank()) {
            body.add("caption", caption);
        }
        try {
            TelegramResponse<?> response = restClient.post()
                    .uri(apiUri("sendPhoto"))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(TelegramResponse.class);
            validate(response);
        } catch (RestClientException exception) {
            throw new TelegramApiException("Telegram API request failed.", exception);
        }
    }

    @Override
    public void sendMediaGroup(Long chatId, List<TelegramMediaPhoto> photos) {
        if (photos == null || photos.isEmpty()) {
            return;
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("chat_id", chatId);

        List<Map<String, Object>> mediaList = new ArrayList<>();
        for (int i = 0; i < photos.size(); i++) {
            TelegramMediaPhoto photo = photos.get(i);
            String attachName = "photo_" + i;
            Map<String, Object> mediaItem = new LinkedHashMap<>();
            mediaItem.put("type", "photo");
            mediaItem.put("media", "attach://" + attachName);
            if (photo.caption() != null && !photo.caption().isBlank()) {
                mediaItem.put("caption", photo.caption());
            }
            mediaList.add(mediaItem);

            final String fname = (photo.filename() != null && !photo.filename().isBlank())
                    ? photo.filename()
                    : "photo_" + i + ".jpg";
            body.add(attachName, new ByteArrayResource(photo.bytes()) {
                @Override
                public String getFilename() {
                    return fname;
                }
            });
        }

        try {
            String mediaJson = objectMapper.writeValueAsString(mediaList);
            body.add("media", mediaJson);

            TelegramResponse<?> response = restClient.post()
                    .uri(apiUri("sendMediaGroup"))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(TelegramResponse.class);
            validate(response);
        } catch (JsonProcessingException | RestClientException exception) {
            throw new TelegramApiException("Telegram API request failed.", exception);
        }
    }

    @Override
    public void sendLocation(Long chatId, double latitude, double longitude) {
        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "latitude", latitude,
                "longitude", longitude
        );
        try {
            TelegramResponse<?> response = restClient.post()
                    .uri(apiUri("sendLocation"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(TelegramResponse.class);
            validate(response);
        } catch (RestClientException exception) {
            throw new TelegramApiException("Telegram API request failed.", exception);
        }
    }

    private URI apiUri(String method) {
        return UriComponentsBuilder.fromUri(properties.getApiBaseUrl())
                .pathSegment("bot" + bot.getBotToken(), method)
                .build()
                .toUri();
    }

    private URI fileUri(String filePath) {
        return UriComponentsBuilder.fromUri(properties.getFileBaseUrl())
                .pathSegment("file", "bot" + bot.getBotToken())
                .path(filePath.startsWith("/") ? filePath : "/" + filePath)
                .build()
                .toUri();
    }

    private void validate(TelegramResponse<?> response) {
        if (response == null || !response.ok()) {
            throw new TelegramApiException("Telegram API request failed.");
        }
    }

    private void validateMessage(TelegramMessageResponse response) {
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

    private record TelegramMessageResponse(boolean ok, TelegramMessageResult result) {
    }

    private record TelegramMessageResult(@JsonProperty("message_id") Long messageId) {
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
