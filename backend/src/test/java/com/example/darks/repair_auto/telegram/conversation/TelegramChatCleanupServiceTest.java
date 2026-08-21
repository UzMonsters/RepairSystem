package com.example.darks.repair_auto.telegram.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import com.example.darks.repair_auto.telegram.core.application.TelegramMediaPhoto;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TelegramChatCleanupServiceTest {

    private final TelegramChatCleanupService cleanupService = new TelegramChatCleanupService();

    @Test
    void deleteFailureDoesNotPropagate() {
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        botClient.failDeletes = true;

        assertThatCode(() -> cleanupService.deleteQuietly(botClient, 10L, 20L, "test"))
                .doesNotThrowAnyException();
        assertThat(botClient.deletedMessages).contains(20L);
    }

    @Test
    void batchDeleteAndRemoveKeyboardAreRecorded() {
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();

        cleanupService.deleteAllQuietly(botClient, 10L, List.of(21L, 22L), "test");
        cleanupService.removeKeyboardQuietly(botClient, 10L, 23L, "test");

        assertThat(botClient.deletedMessages).containsExactly(21L, 22L);
        assertThat(botClient.replyMarkupEdits).containsExactly(23L);
    }

    private static final class RecordingTelegramBotClient implements TelegramBotClient {

        private final List<Long> deletedMessages = new ArrayList<>();
        private final List<Long> replyMarkupEdits = new ArrayList<>();
        private boolean failDeletes;

        @Override
        public Long sendMessage(Long chatId, String text, String replyMarkupJson) {
            return null;
        }

        @Override
        public void answerCallback(String callbackQueryId, String text) {
        }

        @Override
        public void deleteMessage(Long chatId, Long messageId) {
            deletedMessages.add(messageId);
            if (failDeletes) {
                throw new TelegramApiException("Already deleted.");
            }
        }

        @Override
        public void editMessageText(Long chatId, Long messageId, String text, String replyMarkupJson) {
        }

        @Override
        public void editMessageReplyMarkup(Long chatId, Long messageId, String replyMarkupJson) {
            replyMarkupEdits.add(messageId);
        }

        @Override
        public TelegramFileMetadata getFile(String fileId) {
            throw new TelegramApiException("Unsupported.");
        }

        @Override
        public InputStream downloadFile(String filePath, long maxSizeBytes) {
            throw new TelegramApiException("Unsupported.");
        }

        @Override
        public void sendPhoto(Long chatId, String filename, byte[] photoBytes, String caption) {
        }

        @Override
        public void sendMediaGroup(Long chatId, List<TelegramMediaPhoto> photos) {
        }

        @Override
        public void sendLocation(Long chatId, double latitude, double longitude) {
        }
    }
}
