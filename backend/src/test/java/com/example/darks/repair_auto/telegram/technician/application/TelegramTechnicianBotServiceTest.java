package com.example.darks.repair_auto.telegram.technician.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentService;
import com.example.darks.repair_auto.repair.execution.application.RepairExecutionService;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.api.TelegramUpdatePayload;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianSession;
import com.example.darks.repair_auto.telegram.technician.infrastructure.TelegramTechnicianSessionRepository;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TelegramTechnicianBotServiceTest {

    @Test
    void givenLinkedTechnicianThenMainMenuUsesReplyKeyboard() {
        TelegramTechnicianSession session = linkedSession(91001L, 92001L);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of());

        service.handle(message(1L, 91001L, 92001L, "/menu"));

        assertThat(botClient.last().replyMarkupJson())
                .contains("\"keyboard\"")
                .contains("\"resize_keyboard\":true")
                .contains("\"is_persistent\":true")
                .doesNotContain("\"inline_keyboard\"")
                .contains("Pending")
                .contains("Active")
                .contains("Recent")
                .contains("Language");
    }

    @Test
    void givenLinkedTechnicianWhenReplyKeyboardPendingSentThenPendingJobsAreShown() {
        TelegramTechnicianSession session = linkedSession(93001L, 94001L);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of());

        service.handle(message(2L, 93001L, 94001L, "Pending"));

        assertThat(botClient.last().text()).contains("No jobs found");
        assertThat(botClient.last().replyMarkupJson())
                .contains("\"keyboard\"")
                .contains("Pending")
                .doesNotContain("\"inline_keyboard\"");
    }

    private TelegramTechnicianBotService service(
            TelegramTechnicianSession session,
            RecordingTelegramBotClient botClient,
            List<com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment> assignments) {
        TelegramTechnicianSessionRepository sessions = mock(TelegramTechnicianSessionRepository.class);
        TechnicianRepository technicians = mock(TechnicianRepository.class);
        RepairAssignmentRepository assignmentRepository = mock(RepairAssignmentRepository.class);
        when(sessions.findByTelegramUserId(session.getTelegramUserId())).thenReturn(Optional.of(session));
        when(technicians.findByIdForUpdate(session.getTechnicianId())).thenReturn(Optional.of(session.getTechnician()));
        when(assignmentRepository.findByTechnicianIdAndStatusOrderByCreatedAtDesc(
                session.getTechnicianId(),
                AssignmentStatus.PENDING)).thenReturn(assignments);
        return new TelegramTechnicianBotService(
                sessions,
                technicians,
                assignmentRepository,
                mock(RepairAssignmentService.class),
                mock(RepairExecutionService.class),
                mock(RepairRequestService.class),
                mock(AttachmentService.class),
                mock(TechnicianTelegramLinkService.class),
                botClient,
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));
    }

    private TelegramTechnicianSession linkedSession(Long userId, Long chatId) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-06T10:00:00Z");
        Technician technician = new Technician(
                "Technician One",
                "+998902223344",
                "Washer",
                null,
                2,
                LanguageCode.EN,
                true,
                now);
        ReflectionTestUtils.setField(technician, "id", 55L);
        technician.linkTelegram(userId, chatId, now);
        TelegramTechnicianSession session = new TelegramTechnicianSession(userId, chatId, now);
        session.link(technician, LanguageCode.EN, now);
        return session;
    }

    private TelegramUpdatePayload message(Long updateId, Long userId, Long chatId, String text) {
        return new TelegramUpdatePayload(
                updateId,
                new TelegramUpdatePayload.TelegramMessage(
                        updateId,
                        new TelegramUpdatePayload.TelegramUser(userId, "Tech", null),
                        new TelegramUpdatePayload.TelegramChat(chatId, "private"),
                        text,
                        null,
                        null,
                        null),
                null);
    }

    private static final class RecordingTelegramBotClient implements TelegramBotClient {

        private final List<SentMessage> messages = new ArrayList<>();

        SentMessage last() {
            return messages.getLast();
        }

        @Override
        public void sendMessage(Long chatId, String text, String replyMarkupJson) {
            messages.add(new SentMessage(chatId, text, replyMarkupJson));
        }

        @Override
        public void answerCallback(String callbackQueryId, String text) {
        }

        @Override
        public TelegramFileMetadata getFile(String fileId) {
            return new TelegramFileMetadata(fileId, "photos/" + fileId + ".jpg", 0);
        }

        @Override
        public InputStream downloadFile(String filePath, long maxSizeBytes) {
            return InputStream.nullInputStream();
        }
    }

    private record SentMessage(Long chatId, String text, String replyMarkupJson) {
    }
}
