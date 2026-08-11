package com.example.darks.repair_auto.telegram.technician.application;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramProperties;
import com.example.darks.repair_auto.telegram.core.domain.TelegramUserContext;
import com.example.darks.repair_auto.telegram.core.domain.TelegramUserMode;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramUserContextRepository;
import com.example.darks.repair_auto.telegram.technician.api.dto.TechnicianTelegramLinkResponse;
import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianLinkToken;
import com.example.darks.repair_auto.telegram.technician.infrastructure.TelegramTechnicianLinkTokenRepository;
import com.example.darks.repair_auto.telegram.technician.infrastructure.TelegramTechnicianSessionRepository;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class TechnicianTelegramLinkService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final TechnicianRepository technicianRepository;
    private final UserRepository userRepository;
    private final TelegramTechnicianLinkTokenRepository tokenRepository;
    private final TelegramTechnicianSessionRepository sessionRepository;
    private final TelegramUserContextRepository contextRepository;
    private final TelegramProperties properties;
    private final Clock clock;

    @Autowired
    public TechnicianTelegramLinkService(
            TechnicianRepository technicianRepository,
            UserRepository userRepository,
            TelegramTechnicianLinkTokenRepository tokenRepository,
            TelegramTechnicianSessionRepository sessionRepository,
            TelegramUserContextRepository contextRepository,
            TelegramProperties properties) {
        this(technicianRepository, userRepository, tokenRepository, sessionRepository, contextRepository, properties, Clock.systemUTC());
    }

    TechnicianTelegramLinkService(
            TechnicianRepository technicianRepository,
            UserRepository userRepository,
            TelegramTechnicianLinkTokenRepository tokenRepository,
            TelegramTechnicianSessionRepository sessionRepository,
            TelegramUserContextRepository contextRepository,
            TelegramProperties properties,
            Clock clock) {
        this.technicianRepository = technicianRepository;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.sessionRepository = sessionRepository;
        this.contextRepository = contextRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public TechnicianTelegramLinkResponse create(Long technicianId, AuthenticatedUser authenticatedUser) {
        OffsetDateTime now = now();
        Technician technician = technicianRepository.findByIdForUpdate(technicianId).orElseThrow(this::technicianNotFound);
        if (!technician.isActive()) {
            throw new BusinessRuleException("TECHNICIAN_INACTIVE", "Inactive technician cannot be linked.", 409);
        }
        tokenRepository.findFirstByTechnicianIdAndUsedAtIsNullAndRevokedAtIsNull(technicianId)
                .ifPresent(token -> {
                    token.revoked(now);
                    tokenRepository.flush();
                });
        User user = userRepository.findById(authenticatedUser.id()).orElseThrow(this::userNotFound);
        String rawToken = token();
        OffsetDateTime expiresAt = now.plusHours(24);
        tokenRepository.saveAndFlush(new TelegramTechnicianLinkToken(hash(rawToken), technician, user, expiresAt, now));
        return new TechnicianTelegramLinkResponse(
                "https://t.me/" + properties.getTechnician().getBotUsername() + "?start=tech_" + rawToken,
                expiresAt);
    }

    @Transactional
    public void unlink(Long technicianId) {
        OffsetDateTime now = now();
        Technician technician = technicianRepository.findByIdForUpdate(technicianId).orElseThrow(this::technicianNotFound);
        tokenRepository.findFirstByTechnicianIdAndUsedAtIsNullAndRevokedAtIsNull(technicianId)
                .ifPresent(token -> token.revoked(now));
        sessionRepository.findByTechnicianId(technicianId).forEach(session -> session.unlink(now));
        technician.unlinkTelegram(now);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public LinkResult consume(String tokenHash, Long telegramUserId, Long telegramChatId, LanguageCode language) {
        OffsetDateTime now = now();
        TelegramTechnicianLinkToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessRuleException("TELEGRAM_TECHNICIAN_LINK_INVALID", "Invalid link.", 400));
        if (!token.isUsable(now)) {
            throw new BusinessRuleException("TECHNICIAN_LINK_TOKEN_ALREADY_USED", "Invalid link.", 400);
        }
        Technician technician = technicianRepository.findByIdForUpdate(token.getTechnician().getId())
                .orElseThrow(this::technicianNotFound);
        if (!technician.isActive()) {
            throw new BusinessRuleException("TECHNICIAN_INACTIVE", "Inactive technician cannot be linked.", 409);
        }
        if (technician.getTelegramUserId() != null && !technician.getTelegramUserId().equals(telegramUserId)) {
            throw new BusinessRuleException(
                    "TECHNICIAN_TELEGRAM_ALREADY_LINKED",
                    "Technician profile is already linked.",
                    409);
        }
        technicianRepository.findByTelegramUserIdForUpdate(telegramUserId)
                .filter(existing -> !existing.getId().equals(technician.getId()))
                .ifPresent(existing -> {
                    throw new BusinessRuleException(
                            "TECHNICIAN_TELEGRAM_ACCESS_DENIED",
                            "Telegram account is already linked.",
                            409);
                });
        technician.updateTelegramLanguage(language, now);
        technician.linkTelegram(telegramUserId, telegramChatId, now);
        token.used(telegramUserId, now);
        contextRepository.findByTelegramUserId(telegramUserId)
                .ifPresentOrElse(
                        context -> context.switchMode(TelegramUserMode.TECHNICIAN, telegramChatId, now),
                        () -> contextRepository.saveAndFlush(new TelegramUserContext(
                                telegramUserId,
                                telegramChatId,
                                TelegramUserMode.TECHNICIAN,
                                now)));
        return new LinkResult(technician);
    }

    @Transactional(readOnly = true, noRollbackFor = BusinessRuleException.class)
    public void requireUsableToken(String tokenHash) {
        OffsetDateTime now = now();
        TelegramTechnicianLinkToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessRuleException("TELEGRAM_TECHNICIAN_LINK_INVALID", "Invalid link.", 400));
        if (!token.isUsable(now)) {
            throw new BusinessRuleException("TECHNICIAN_LINK_TOKEN_ALREADY_USED", "Invalid link.", 400);
        }
    }

    public String hash(String rawToken) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            builder.append("%02x".formatted(value));
        }
        return builder.toString();
    }

    private String token() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private BusinessRuleException technicianNotFound() {
        return new BusinessRuleException("TECHNICIAN_NOT_FOUND", "Technician was not found.", 404);
    }

    private BusinessRuleException userNotFound() {
        return new BusinessRuleException("USER_NOT_FOUND", "Authenticated user was not found.", 404);
    }

    public record LinkResult(Technician technician) {
    }
}
