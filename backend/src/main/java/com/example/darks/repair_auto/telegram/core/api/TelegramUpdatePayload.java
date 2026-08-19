package com.example.darks.repair_auto.telegram.core.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUpdatePayload(
        @JsonProperty("update_id") Long updateId,
        TelegramMessage message,
        @JsonProperty("callback_query") TelegramCallbackQuery callbackQuery) {

    public String updateType() {
        if (message != null) {
            return "MESSAGE";
        }
        if (callbackQuery != null) {
            return "CALLBACK_QUERY";
        }
        return "UNSUPPORTED";
    }

    public TelegramUser sender() {
        if (message != null) {
            return message.from();
        }
        return callbackQuery == null ? null : callbackQuery.from();
    }

    public TelegramChat chat() {
        if (message != null) {
            return message.chat();
        }
        return callbackQuery == null || callbackQuery.message() == null ? null : callbackQuery.message().chat();
    }

    public String text() {
        return message == null ? null : message.text();
    }

    public TelegramContact contact() {
        return message == null ? null : message.contact();
    }

    public TelegramLocation location() {
        if (message == null) {
            return null;
        }
        if (message.location() != null) {
            return message.location();
        }
        if (message.venue() != null) {
            return message.venue().location();
        }
        return null;
    }

    public List<TelegramPhotoSize> photo() {
        return message == null ? List.of() : nullToEmpty(message.photo());
    }

    private List<TelegramPhotoSize> nullToEmpty(List<TelegramPhotoSize> value) {
        return value == null ? List.of() : value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramMessage(
            @JsonProperty("message_id") Long messageId,
            TelegramUser from,
            TelegramChat chat,
            String text,
            TelegramContact contact,
            TelegramLocation location,
            TelegramVenue venue,
            List<TelegramPhotoSize> photo) {

        public TelegramMessage(
                Long messageId,
                TelegramUser from,
                TelegramChat chat,
                String text,
                TelegramContact contact,
                TelegramLocation location,
                List<TelegramPhotoSize> photo) {
            this(messageId, from, chat, text, contact, location, null, photo);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramVenue(
            TelegramLocation location,
            String title,
            String address) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramCallbackQuery(
            String id,
            TelegramUser from,
            TelegramMessage message,
            String data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramUser(
            Long id,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramChat(
            Long id,
            String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramContact(
            @JsonProperty("phone_number") String phoneNumber,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            @JsonProperty("user_id") Long userId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramLocation(
            BigDecimal latitude,
            BigDecimal longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramPhotoSize(
            @JsonProperty("file_id") String fileId,
            @JsonProperty("file_unique_id") String fileUniqueId,
            Integer width,
            Integer height,
            @JsonProperty("file_size") Long fileSize) {
    }
}
