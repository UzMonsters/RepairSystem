package com.example.darks.repair_auto.telegram.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "telegram_user_contexts")
public class TelegramUserContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_user_id", nullable = false, unique = true)
    private Long telegramUserId;

    @Column(name = "telegram_chat_id", nullable = false, unique = true)
    private Long telegramChatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "active_mode", nullable = false, length = 20)
    private TelegramUserMode activeMode;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TelegramUserContext() {
    }

    public TelegramUserContext(
            Long telegramUserId,
            Long telegramChatId,
            TelegramUserMode activeMode,
            OffsetDateTime now) {
        this.telegramUserId = telegramUserId;
        this.telegramChatId = telegramChatId;
        this.activeMode = activeMode;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public TelegramUserMode getActiveMode() {
        return activeMode;
    }

    public void switchMode(TelegramUserMode mode, Long chatId, OffsetDateTime now) {
        this.activeMode = mode;
        this.telegramChatId = chatId;
        this.updatedAt = now;
    }
}
