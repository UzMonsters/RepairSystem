package com.example.darks.repair_auto.telegram.technician.domain;

import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "telegram_technician_sessions")
public class TelegramTechnicianSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_user_id", nullable = false, unique = true)
    private Long telegramUserId;

    @Column(name = "telegram_chat_id", nullable = false, unique = true)
    private Long telegramChatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private LanguageCode language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TelegramTechnicianSessionState state;

    @Column(name = "pending_token_hash", length = 64)
    private String pendingTokenHash;

    @Column(name = "selected_request_id")
    private Long selectedRequestId;

    @Column(name = "draft_text", length = 4000)
    private String draftText;

    @Column(name = "last_interaction_at", nullable = false)
    private OffsetDateTime lastInteractionAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TelegramTechnicianSession() {
    }

    public TelegramTechnicianSession(Long telegramUserId, Long telegramChatId, OffsetDateTime now) {
        this.telegramUserId = telegramUserId;
        this.telegramChatId = telegramChatId;
        this.language = LanguageCode.UZ;
        this.state = TelegramTechnicianSessionState.MAIN_MENU;
        this.lastInteractionAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public Technician getTechnician() {
        return technician;
    }

    public Long getTechnicianId() {
        return technician == null ? null : technician.getId();
    }

    public LanguageCode getLanguage() {
        return language;
    }

    public TelegramTechnicianSessionState getState() {
        return state;
    }

    public String getPendingTokenHash() {
        return pendingTokenHash;
    }

    public Long getSelectedRequestId() {
        return selectedRequestId;
    }

    public String getDraftText() {
        return draftText;
    }

    public void touch(Long chatId, OffsetDateTime now) {
        this.telegramChatId = chatId;
        this.lastInteractionAt = now;
        this.updatedAt = now;
    }

    public void pendingLink(String tokenHash, OffsetDateTime now) {
        this.pendingTokenHash = tokenHash;
        this.state = TelegramTechnicianSessionState.LANGUAGE_SELECTION;
        this.updatedAt = now;
    }

    public void link(Technician technician, LanguageCode language, OffsetDateTime now) {
        this.technician = technician;
        this.language = language;
        this.pendingTokenHash = null;
        this.state = TelegramTechnicianSessionState.MAIN_MENU;
        this.selectedRequestId = null;
        this.draftText = null;
        this.updatedAt = now;
    }

    public void language(LanguageCode language, OffsetDateTime now) {
        this.language = language;
        this.updatedAt = now;
    }

    public void state(TelegramTechnicianSessionState state, OffsetDateTime now) {
        this.state = state;
        this.updatedAt = now;
    }

    public void selectRequest(Long requestId, OffsetDateTime now) {
        this.selectedRequestId = requestId;
        this.draftText = null;
        this.updatedAt = now;
    }

    public void draftText(String text, OffsetDateTime now) {
        this.draftText = text;
        this.updatedAt = now;
    }

    public void clearDraft(OffsetDateTime now) {
        this.selectedRequestId = null;
        this.draftText = null;
        this.updatedAt = now;
    }

    public void unlink(OffsetDateTime now) {
        this.technician = null;
        this.pendingTokenHash = null;
        this.state = TelegramTechnicianSessionState.MAIN_MENU;
        this.selectedRequestId = null;
        this.draftText = null;
        this.updatedAt = now;
    }
}
