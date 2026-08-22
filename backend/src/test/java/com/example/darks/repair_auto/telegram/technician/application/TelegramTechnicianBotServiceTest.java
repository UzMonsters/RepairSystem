package com.example.darks.repair_auto.telegram.technician.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentService;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.execution.application.RepairExecutionService;
import com.example.darks.repair_auto.repair.execution.api.dto.RepairExecutionSummary;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestDetailResponse;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestPriority;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.api.TelegramUpdatePayload;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianSession;
import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianSessionState;
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

    @Test
    void givenPendingJobsThenListAndButtonsDoNotExposeRequestCode() {
        TelegramTechnicianSession session = linkedSession(93002L, 94002L);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of(assignment(
                session,
                AssignmentStatus.PENDING,
                RepairRequestStatus.ASSIGNED)));

        service.handle(message(8L, 93002L, 94002L, "Pending"));

        assertThat(botClient.last().text())
                .contains("1. Washer | Assigned")
                .doesNotContain("REP-2026-000002");
        assertThat(botClient.last().replyMarkupJson())
                .contains("Open 1")
                .doesNotContain("REP-2026-000002");
    }

    @Test
    void givenUzbekTechnicianWhenPendingJobsShownThenListAndButtonsAreUzbek() {
        TelegramTechnicianSession session = linkedSession(93009L, 94009L, LanguageCode.UZ);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of(assignment(
                session,
                AssignmentStatus.PENDING,
                RepairRequestStatus.ASSIGNED)));

        service.handle(message(15L, 93009L, 94009L, "Kutilayotgan"));

        assertThat(botClient.last().text())
                .contains("Ishlar")
                .contains("1. Kir yuvish mashinasi | Biriktirilgan")
                .doesNotContain("Jobs")
                .doesNotContain("Assigned");
        assertThat(botClient.last().replyMarkupJson())
                .contains("Ochish 1")
                .doesNotContain("Open 1");
    }

    @Test
    void givenAcceptedAssignmentThenOnlyStartActionIsShown() {
        TelegramTechnicianSession session = linkedSession(93003L, 94003L);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of(assignment(
                session,
                AssignmentStatus.ACCEPTED,
                RepairRequestStatus.ASSIGNED)));

        service.handle(callback(9L, 93003L, 94003L, "cb-accept", "taccept:123"));

        assertThat(botClient.last().text()).contains("Assignment accepted");
        assertThat(botClient.last().replyMarkupJson())
                .contains("Start")
                .doesNotContain("Accept")
                .doesNotContain("Reject")
                .doesNotContain("Diagnosis")
                .doesNotContain("Complete");
    }

    @Test
    void givenSessionLanguageDriftedWhenAcceptedThenTechnicianPreferredLanguageWins() {
        TelegramTechnicianSession session = linkedSession(93010L, 94010L, LanguageCode.EN);
        session.getTechnician().updateTelegramLanguage(LanguageCode.UZ, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of(assignment(
                session,
                AssignmentStatus.ACCEPTED,
                RepairRequestStatus.ASSIGNED)));

        service.handle(callback(16L, 93010L, 94010L, "cb-accept", "taccept:123"));

        assertThat(session.getLanguage()).isEqualTo(LanguageCode.UZ);
        assertThat(botClient.last().text())
                .contains("Topshiriq qabul qilindi")
                .doesNotContain("Assignment accepted");
        assertThat(botClient.last().replyMarkupJson())
                .contains("Boshlash")
                .doesNotContain("Start");
    }

    @Test
    void givenInProgressAssignmentThenOnlyInProgressActionsAreShown() {
        TelegramTechnicianSession session = linkedSession(93004L, 94004L);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of(assignment(
                session,
                AssignmentStatus.ACCEPTED,
                RepairRequestStatus.IN_PROGRESS)));

        service.handle(callback(10L, 93004L, 94004L, "cb-start", "tstart:123"));

        assertThat(botClient.last().text()).contains("Repair started");
        assertThat(botClient.last().replyMarkupJson())
                .contains("Diagnosis")
                .contains("Wait")
                .contains("Diag photo")
                .contains("Complete")
                .doesNotContain("Accept")
                .doesNotContain("Reject")
                .doesNotContain("Start")
                .doesNotContain("Resume");
    }

    @Test
    void givenWaitingAssignmentThenOnlyResumeAndDiagnosisActionsAreShown() {
        TelegramTechnicianSession session = linkedSession(93005L, 94005L);
        session.selectRequest(123L, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.state(
                TelegramTechnicianSessionState.AWAITING_WAIT_REASON,
                OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of(assignment(
                session,
                AssignmentStatus.ACCEPTED,
                RepairRequestStatus.WAITING_FOR_PARTS)));

        service.handle(message(11L, 93005L, 94005L, "Waiting reason"));

        assertThat(botClient.last().replyMarkupJson())
                .contains("Resume")
                .contains("Diagnosis")
                .doesNotContain("Accept")
                .doesNotContain("Reject")
                .doesNotContain("Start")
                .doesNotContain("Complete");
    }

    @Test
    void givenWaitingAssignmentWhenResumeTappedThenRepairResumesImmediately() {
        TelegramTechnicianSession session = linkedSession(93007L, 94007L);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of(assignment(
                session,
                AssignmentStatus.ACCEPTED,
                RepairRequestStatus.IN_PROGRESS)));

        service.handle(callback(13L, 93007L, 94007L, "cb-resume", "tresume:123"));

        assertThat(botClient.last().text()).contains("Repair resumed");
        assertThat(botClient.last().replyMarkupJson())
                .contains("Diagnosis")
                .contains("Complete");
        assertThat(session.getState()).isEqualTo(TelegramTechnicianSessionState.MAIN_MENU);
        assertThat(session.getSelectedRequestId()).isNull();
    }

    @Test
    void givenTechnicianAwaitingWorkTextWhenOpeningActiveThenDraftStateIsCleared() {
        TelegramTechnicianSession session = linkedSession(93008L, 94008L);
        session.selectRequest(123L, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.state(
                TelegramTechnicianSessionState.AWAITING_WORK_PERFORMED,
                OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of(assignment(
                session,
                AssignmentStatus.ACCEPTED,
                RepairRequestStatus.IN_PROGRESS)));

        service.handle(message(14L, 93008L, 94008L, "Active"));

        assertThat(botClient.last().text()).contains("Jobs");
        assertThat(session.getState()).isEqualTo(TelegramTechnicianSessionState.MAIN_MENU);
        assertThat(session.getSelectedRequestId()).isNull();
    }

    @Test
    void givenWorkTextSentThenCompletionPhotoPromptDoesNotRepeatActionPanel() {
        TelegramTechnicianSession session = linkedSession(93006L, 94006L);
        session.selectRequest(123L, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.state(
                TelegramTechnicianSessionState.AWAITING_WORK_PERFORMED,
                OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of(assignment(
                session,
                AssignmentStatus.ACCEPTED,
                RepairRequestStatus.IN_PROGRESS)));

        service.handle(message(12L, 93006L, 94006L, "Work done."));

        assertThat(botClient.last().text()).contains("Send completion photo");
        assertThat(botClient.last().replyMarkupJson()).isNull();
    }

    @Test
    void givenRussianLanguageThenMainMenuAndPendingEmptyStateAreRussian() {
        TelegramTechnicianSession session = linkedSession(95001L, 96001L, LanguageCode.RU);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of());

        service.handle(message(3L, 95001L, 96001L, "/menu"));
        service.handle(message(4L, 95001L, 96001L, "Ожидающие"));

        assertThat(botClient.messages().get(0).replyMarkupJson())
                .contains("Ожидающие")
                .contains("Активные")
                .contains("Недавние")
                .contains("Язык")
                .doesNotContain("Pending");
        assertThat(botClient.last().text()).contains("Заявок нет");
        assertThat(botClient.last().replyMarkupJson()).contains("Ожидающие");
    }

    @Test
    void givenUzbekLanguageThenMainMenuAndPendingEmptyStateAreUzbek() {
        TelegramTechnicianSession session = linkedSession(97001L, 98001L, LanguageCode.UZ);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TelegramTechnicianBotService service = service(session, botClient, List.of());

        service.handle(message(5L, 97001L, 98001L, "/menu"));
        service.handle(message(6L, 97001L, 98001L, "Kutilayotgan"));

        assertThat(botClient.messages().get(0).replyMarkupJson())
                .contains("Kutilayotgan")
                .contains("Faol")
                .contains("So'nggi")
                .contains("Til")
                .doesNotContain("Pending");
        assertThat(botClient.last().text()).contains("Ishlar topilmadi");
        assertThat(botClient.last().replyMarkupJson()).contains("Kutilayotgan");
    }

    @Test
    void givenPendingLinkWhenRussianSelectedThenSessionLanguageAndKeyboardUpdateImmediately() {
        TelegramTechnicianSession session = linkedSession(99001L, 99002L, LanguageCode.EN);
        session.pendingLink("token-hash", OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        TechnicianTelegramLinkService linkService = mock(TechnicianTelegramLinkService.class);
        when(linkService.consume("token-hash", 99001L, 99002L, LanguageCode.RU))
                .thenReturn(new TechnicianTelegramLinkService.LinkResult(session.getTechnician()));
        TelegramTechnicianBotService service = service(session, botClient, List.of(), linkService);

        service.handle(callback(7L, 99001L, 99002L, "cb-lang", "tlang:RU"));

        assertThat(session.getLanguage()).isEqualTo(LanguageCode.RU);
        assertThat(session.getPendingTokenHash()).isNull();
        assertThat(botClient.last().text()).contains("Профиль техника привязан");
        assertThat(botClient.last().replyMarkupJson())
                .contains("Ожидающие")
                .doesNotContain("Pending");
    }

    @Test
    void givenModeSwitchAllowedThenTechnicianIsValidatedWithoutWriteLock() {
        TelegramTechnicianSession session = linkedSession(99003L, 99004L, LanguageCode.EN);
        TelegramTechnicianSessionRepository sessions = mock(TelegramTechnicianSessionRepository.class);
        TechnicianRepository technicians = mock(TechnicianRepository.class);
        when(sessions.findByTelegramUserId(session.getTelegramUserId())).thenReturn(Optional.of(session));
        when(technicians.findById(session.getTechnicianId())).thenReturn(Optional.of(session.getTechnician()));
        TelegramTechnicianBotService service = new TelegramTechnicianBotService(
                sessions,
                technicians,
                mock(RepairAssignmentRepository.class),
                mock(RepairAssignmentService.class),
                mock(RepairExecutionService.class),
                mock(RepairRequestService.class),
                mock(AttachmentService.class),
                mock(TechnicianTelegramLinkService.class),
                new RecordingTelegramBotClient(),
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.requireSwitchAllowed(session.getTelegramUserId(), session.getTelegramChatId());

        verify(technicians).findById(session.getTechnicianId());
        verify(technicians, never()).findByIdForUpdate(any());
    }

    @Test
    void givenTelegramFileMetadataWithoutSizeThenWebhookPhotoSizeIsUsedForTechnicianUpload() {
        TelegramTechnicianSession session = linkedSession(99005L, 99006L, LanguageCode.EN);
        session.selectRequest(123L, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.state(
                TelegramTechnicianSessionState.AWAITING_COMPLETION_PHOTO,
                OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        TelegramTechnicianSessionRepository sessions = mock(TelegramTechnicianSessionRepository.class);
        TechnicianRepository technicians = mock(TechnicianRepository.class);
        RepairAssignmentRepository assignmentRepository = mock(RepairAssignmentRepository.class);
        AttachmentService attachmentService = mock(AttachmentService.class);
        RepairRequestService requestService = mock(RepairRequestService.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(session.getTelegramUserId())).thenReturn(Optional.of(session));
        when(technicians.findById(session.getTechnicianId())).thenReturn(Optional.of(session.getTechnician()));
        RepairAssignment assignment = assignment(session, AssignmentStatus.ACCEPTED, RepairRequestStatus.IN_PROGRESS);
        when(assignmentRepository.findActiveByRequestId(eq(123L), any())).thenReturn(Optional.of(assignment));
        when(requestService.get(123L)).thenReturn(detail(true));
        TelegramTechnicianBotService service = new TelegramTechnicianBotService(
                sessions,
                technicians,
                assignmentRepository,
                mock(RepairAssignmentService.class),
                mock(RepairExecutionService.class),
                requestService,
                attachmentService,
                mock(TechnicianTelegramLinkService.class),
                botClient,
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(photo(20L, 99005L, 99006L, "completion-photo", 3456L));

        assertThat(botClient.lastDownloadMaxSizeBytes()).isEqualTo(3456L);
        verify(attachmentService).uploadFromTechnician(
                eq(123L),
                eq(AttachmentType.COMPLETION_PHOTO),
                eq("telegram-photo.jpg"),
                any(),
                eq(3456L),
                any(),
                eq(session.getTechnicianId()));
    }

    @Test
    void givenCompletionPhotoWithoutDiagnosisThenBotPromptsDiagnosisBeforeFinalComplete() {
        TelegramTechnicianSession session = linkedSession(99007L, 99008L, LanguageCode.UZ);
        session.selectRequest(123L, OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.draftText("Tugadi", OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        session.state(
                TelegramTechnicianSessionState.AWAITING_COMPLETION_PHOTO,
                OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        TelegramTechnicianSessionRepository sessions = mock(TelegramTechnicianSessionRepository.class);
        TechnicianRepository technicians = mock(TechnicianRepository.class);
        RepairAssignmentRepository assignmentRepository = mock(RepairAssignmentRepository.class);
        AttachmentService attachmentService = mock(AttachmentService.class);
        RepairExecutionService executionService = mock(RepairExecutionService.class);
        RepairRequestService requestService = mock(RepairRequestService.class);
        RecordingTelegramBotClient botClient = new RecordingTelegramBotClient();
        when(sessions.findByTelegramUserIdForUpdate(session.getTelegramUserId())).thenReturn(Optional.of(session));
        when(technicians.findById(session.getTechnicianId())).thenReturn(Optional.of(session.getTechnician()));
        when(technicians.findByIdForUpdate(session.getTechnicianId())).thenReturn(Optional.of(session.getTechnician()));
        RepairAssignment assignment = assignment(session, AssignmentStatus.ACCEPTED, RepairRequestStatus.IN_PROGRESS);
        when(assignmentRepository.findActiveByRequestId(eq(123L), any())).thenReturn(Optional.of(assignment));
        when(requestService.get(123L)).thenReturn(detail(false));
        TelegramTechnicianBotService service = new TelegramTechnicianBotService(
                sessions,
                technicians,
                assignmentRepository,
                mock(RepairAssignmentService.class),
                executionService,
                requestService,
                attachmentService,
                mock(TechnicianTelegramLinkService.class),
                botClient,
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));

        service.handle(photo(21L, 99007L, 99008L, "completion-photo", 3456L));

        assertThat(botClient.last().text()).contains("Tashxis matnini yuboring");
        assertThat(botClient.last().replyMarkupJson()).isNull();
        assertThat(session.getState()).isEqualTo(TelegramTechnicianSessionState.AWAITING_DIAGNOSIS);
        assertThat(session.getDraftText()).isEqualTo("Tugadi");

        service.handle(message(22L, 99007L, 99008L, "Hammasi soz"));

        assertThat(botClient.last().text()).contains("Tashxis saqlandi");
        assertThat(botClient.last().replyMarkupJson())
                .contains("Yakunlash")
                .contains("tcomplete:123");
        assertThat(session.getDraftText()).isEqualTo("Tugadi");
    }

    private TelegramTechnicianBotService service(
            TelegramTechnicianSession session,
            RecordingTelegramBotClient botClient,
            List<com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment> assignments) {
        return service(session, botClient, assignments, mock(TechnicianTelegramLinkService.class));
    }

    private TelegramTechnicianBotService service(
            TelegramTechnicianSession session,
            RecordingTelegramBotClient botClient,
            List<com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment> assignments,
            TechnicianTelegramLinkService linkService) {
        TelegramTechnicianSessionRepository sessions = mock(TelegramTechnicianSessionRepository.class);
        TechnicianRepository technicians = mock(TechnicianRepository.class);
        RepairAssignmentRepository assignmentRepository = mock(RepairAssignmentRepository.class);
        when(sessions.findByTelegramUserIdForUpdate(session.getTelegramUserId())).thenReturn(Optional.of(session));
        when(sessions.findByTelegramUserId(session.getTelegramUserId())).thenReturn(Optional.of(session));
        when(technicians.findById(session.getTechnicianId())).thenReturn(Optional.of(session.getTechnician()));
        when(technicians.findByIdForUpdate(session.getTechnicianId())).thenReturn(Optional.of(session.getTechnician()));
        RepairRequestService requestService = mock(RepairRequestService.class);
        when(assignmentRepository.findByTechnicianIdAndStatusOrderByCreatedAtDesc(
                session.getTechnicianId(),
                AssignmentStatus.PENDING)).thenReturn(assignments);
        when(assignmentRepository.findByTechnicianIdAndStatusInOrderByCreatedAtDesc(
                eq(session.getTechnicianId()),
                any())).thenReturn(assignments);
        for (RepairAssignment assignment : assignments) {
            when(assignmentRepository.findActiveByRequestId(
                    eq(assignment.getRepairRequest().getId()),
                    any())).thenReturn(Optional.of(assignment));
            when(requestService.get(assignment.getRepairRequest().getId())).thenReturn(detail(true));
        }
        return new TelegramTechnicianBotService(
                sessions,
                technicians,
                assignmentRepository,
                mock(RepairAssignmentService.class),
                mock(RepairExecutionService.class),
                requestService,
                mock(AttachmentService.class),
                linkService,
                botClient,
                Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"), ZoneOffset.UTC));
    }

    private RepairRequestDetailResponse detail(boolean diagnosisPresent) {
        return new RepairRequestDetailResponse(
                123L,
                "REP-2026-000002",
                RepairRequestStatus.IN_PROGRESS,
                RepairRequestPriority.NORMAL,
                null,
                "Washer leaks.",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new RepairExecutionSummary(
                        1L,
                        OffsetDateTime.parse("2026-08-06T10:00:00Z"),
                        diagnosisPresent,
                        null,
                        null,
                        null),
                null,
                OffsetDateTime.parse("2026-08-06T10:00:00Z"),
                OffsetDateTime.parse("2026-08-06T10:00:00Z"));
    }

    private TelegramTechnicianSession linkedSession(Long userId, Long chatId) {
        return linkedSession(userId, chatId, LanguageCode.EN);
    }

    private TelegramTechnicianSession linkedSession(Long userId, Long chatId, LanguageCode language) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-06T10:00:00Z");
        Technician technician = new Technician(
                "Technician One",
                "+998902223344",
                "Washer",
                null,
                2,
                language,
                true,
                now);
        ReflectionTestUtils.setField(technician, "id", 55L);
        technician.linkTelegram(userId, chatId, now);
        TelegramTechnicianSession session = new TelegramTechnicianSession(userId, chatId, now);
        session.link(technician, language, now);
        return session;
    }

    private RepairAssignment assignment(
            TelegramTechnicianSession session,
            AssignmentStatus assignmentStatus,
            RepairRequestStatus requestStatus) {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-06T10:00:00Z");
        Customer customer = Customer.telegram(
                "Repair Customer",
                "+998901111111",
                11111L,
                21111L,
                LanguageCode.EN,
                now);
        RepairCategory category = new RepairCategory(
                "Washer",
                "Стиральная машина",
                "Kir yuvish mashinasi",
                "washer",
                "washer-ru",
                "washer-uz",
                null,
                null,
                null,
                true,
                now);
        RepairRequest request = new RepairRequest(
                "REP-2026-000002",
                customer,
                category,
                "Washer leaks.",
                "Tashkent",
                null,
                null,
                RepairRequestPriority.NORMAL,
                null,
                null,
                null,
                now);
        ReflectionTestUtils.setField(request, "id", 123L);
        ReflectionTestUtils.setField(request, "status", requestStatus);
        RepairAssignment assignment = new RepairAssignment(request, session.getTechnician(), null, mock(User.class), now);
        ReflectionTestUtils.setField(assignment, "status", assignmentStatus);
        return assignment;
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

    private TelegramUpdatePayload callback(Long updateId, Long userId, Long chatId, String id, String data) {
        return new TelegramUpdatePayload(
                updateId,
                null,
                new TelegramUpdatePayload.TelegramCallbackQuery(
                        id,
                        new TelegramUpdatePayload.TelegramUser(userId, "Tech", null),
                        new TelegramUpdatePayload.TelegramMessage(
                                updateId,
                                null,
                                new TelegramUpdatePayload.TelegramChat(chatId, "private"),
                                null,
                                null,
                                null,
                                null),
                        data));
    }

    private TelegramUpdatePayload photo(Long updateId, Long userId, Long chatId, String fileId, Long size) {
        return new TelegramUpdatePayload(
                updateId,
                new TelegramUpdatePayload.TelegramMessage(
                        updateId,
                        new TelegramUpdatePayload.TelegramUser(userId, "Tech", null),
                        new TelegramUpdatePayload.TelegramChat(chatId, "private"),
                        null,
                        null,
                        null,
                        List.of(new TelegramUpdatePayload.TelegramPhotoSize(fileId, null, 800, 600, size))),
                null);
    }

    private static final class RecordingTelegramBotClient implements TelegramBotClient {

        private final List<SentMessage> messages = new ArrayList<>();
        private long lastDownloadMaxSizeBytes = -1;

        SentMessage last() {
            return messages.getLast();
        }

        List<SentMessage> messages() {
            return messages;
        }

        long lastDownloadMaxSizeBytes() {
            return lastDownloadMaxSizeBytes;
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
            this.lastDownloadMaxSizeBytes = maxSizeBytes;
            return InputStream.nullInputStream();
        }

        @Override
        public void sendPhoto(Long chatId, String filename, byte[] photoBytes, String caption) {
        }

        @Override
        public void sendMediaGroup(Long chatId, List<com.example.darks.repair_auto.telegram.core.application.TelegramMediaPhoto> photos) {
        }

        @Override
        public void sendLocation(Long chatId, double latitude, double longitude) {
        }
    }

    private record SentMessage(Long chatId, String text, String replyMarkupJson) {
    }
}
