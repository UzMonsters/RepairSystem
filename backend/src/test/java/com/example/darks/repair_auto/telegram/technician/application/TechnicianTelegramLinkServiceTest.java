package com.example.darks.repair_auto.telegram.technician.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramProperties;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramUserContextRepository;
import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianLinkToken;
import com.example.darks.repair_auto.telegram.technician.infrastructure.TelegramTechnicianLinkTokenRepository;
import com.example.darks.repair_auto.telegram.technician.infrastructure.TelegramTechnicianSessionRepository;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TechnicianTelegramLinkServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-06T10:00:00Z");
    private static final String TOKEN_HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void givenSameTechnicianAndTelegramAccountWhenTokenConsumedThenIdempotentSuccessUpdatesLanguage() {
        Technician technician = technician(55L, LanguageCode.EN);
        technician.linkTelegram(9001L, 19001L, NOW);
        TelegramTechnicianLinkToken token = token(technician);
        TestContext context = context(token);
        when(context.technicians.findByTelegramUserIdForUpdate(9001L)).thenReturn(Optional.of(technician));

        TechnicianTelegramLinkService.LinkResult result = context.service.consume(
                TOKEN_HASH,
                9001L,
                19002L,
                LanguageCode.RU);

        assertThat(result.technician()).isSameAs(technician);
        assertThat(technician.getTelegramUserId()).isEqualTo(9001L);
        assertThat(technician.getTelegramChatId()).isEqualTo(19002L);
        assertThat(technician.getPreferredLanguage()).isEqualTo(LanguageCode.RU);
        assertThat(ReflectionTestUtils.getField(token, "usedByTelegramUserId")).isEqualTo(9001L);
        assertThat(ReflectionTestUtils.getField(token, "usedAt")).isNotNull();
    }

    @Test
    void givenTechnicianLinkedToDifferentTelegramAccountWhenTokenConsumedThenRejected() {
        Technician technician = technician(55L, LanguageCode.EN);
        technician.linkTelegram(9001L, 19001L, NOW);
        TestContext context = context(token(technician));

        assertThatThrownBy(() -> context.service.consume(TOKEN_HASH, 9002L, 19002L, LanguageCode.RU))
                .isInstanceOf(BusinessException.class);

        assertThat(technician.getTelegramUserId()).isEqualTo(9001L);
        assertThat(technician.getPreferredLanguage()).isEqualTo(LanguageCode.EN);
    }

    @Test
    void givenTelegramAccountLinkedToDifferentTechnicianWhenTokenConsumedThenRejected() {
        Technician target = technician(55L, LanguageCode.EN);
        Technician existing = technician(56L, LanguageCode.UZ);
        existing.linkTelegram(9001L, 19001L, NOW);
        TestContext context = context(token(target));
        when(context.technicians.findByTelegramUserIdForUpdate(9001L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> context.service.consume(TOKEN_HASH, 9001L, 19001L, LanguageCode.RU))
                .isInstanceOf(BusinessException.class);

        assertThat(target.getTelegramUserId()).isNull();
        assertThat(target.getPreferredLanguage()).isEqualTo(LanguageCode.EN);
    }

    @Test
    void givenExistingActiveTokenWhenCreatingNewLinkThenRevocationIsFlushedBeforeReplacementInsert() {
        Technician technician = technician(55L, LanguageCode.EN);
        User user = user(77L);
        TelegramTechnicianLinkToken existingToken = token(technician);
        TelegramTechnicianLinkTokenRepository tokens = mock(TelegramTechnicianLinkTokenRepository.class);
        TechnicianRepository technicians = mock(TechnicianRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(technicians.findByIdForUpdate(55L)).thenReturn(Optional.of(technician));
        when(users.findById(77L)).thenReturn(Optional.of(user));
        when(tokens.findFirstByTechnicianIdAndUsedAtIsNullAndRevokedAtIsNull(55L))
                .thenReturn(Optional.of(existingToken));
        TechnicianTelegramLinkService service = new TechnicianTelegramLinkService(
                technicians,
                users,
                tokens,
                mock(TelegramTechnicianSessionRepository.class),
                mock(TelegramUserContextRepository.class),
                new TelegramProperties(),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.create(55L, new AuthenticatedUser(user));

        assertThat(ReflectionTestUtils.getField(existingToken, "revokedAt")).isNotNull();
        verify(tokens).flush();
        verify(tokens).saveAndFlush(org.mockito.ArgumentMatchers.any(TelegramTechnicianLinkToken.class));
    }

    private TestContext context(TelegramTechnicianLinkToken token) {
        TelegramTechnicianLinkTokenRepository tokens = mock(TelegramTechnicianLinkTokenRepository.class);
        TechnicianRepository technicians = mock(TechnicianRepository.class);
        when(tokens.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(token));
        when(technicians.findByIdForUpdate(token.getTechnician().getId())).thenReturn(Optional.of(token.getTechnician()));
        when(technicians.findByTelegramUserIdForUpdate(9001L)).thenReturn(Optional.empty());
        TechnicianTelegramLinkService service = new TechnicianTelegramLinkService(
                technicians,
                mock(UserRepository.class),
                tokens,
                mock(TelegramTechnicianSessionRepository.class),
                mock(TelegramUserContextRepository.class),
                new TelegramProperties(),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));
        return new TestContext(service, technicians);
    }

    private Technician technician(Long id, LanguageCode language) {
        Technician technician = new Technician(
                "Technician " + id,
                "+99890%07d".formatted(id),
                "Washer",
                null,
                2,
                language,
                true,
                NOW);
        ReflectionTestUtils.setField(technician, "id", id);
        return technician;
    }

    private TelegramTechnicianLinkToken token(Technician technician) {
        return new TelegramTechnicianLinkToken(
                TOKEN_HASH,
                technician,
                mock(User.class),
                NOW.plusHours(1),
                NOW);
    }

    private User user(Long id) {
        User user = new User(
                "Admin",
                "admin@example.com",
                "hash",
                com.example.darks.repair_auto.identity.domain.UserRole.ADMIN,
                true,
                NOW);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private record TestContext(TechnicianTelegramLinkService service, TechnicianRepository technicians) {
    }
}
