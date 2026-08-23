package com.example.darks.repair_auto.telegram.technician.application;

import com.example.darks.repair_auto.repair.assignment.api.dto.AssignmentRejectionRequest;
import com.example.darks.repair_auto.repair.assignment.application.RepairAssignmentService;
import com.example.darks.repair_auto.repair.assignment.domain.AssignmentStatus;
import com.example.darks.repair_auto.repair.assignment.domain.RepairAssignment;
import com.example.darks.repair_auto.repair.assignment.infrastructure.RepairAssignmentRepository;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentService;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.execution.api.dto.CompleteRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.DiagnosisRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.ResumeRepairRequest;
import com.example.darks.repair_auto.repair.execution.api.dto.WaitForPartsRequest;
import com.example.darks.repair_auto.repair.execution.application.RepairExecutionService;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestDetailResponse;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.api.TelegramUpdatePayload;
import com.example.darks.repair_auto.telegram.core.application.TelegramApiException;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import com.example.darks.repair_auto.telegram.core.application.TelegramScreenService;
import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianSession;
import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianSessionState;
import com.example.darks.repair_auto.telegram.technician.infrastructure.TelegramTechnicianSessionRepository;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramTechnicianBotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramTechnicianBotService.class);
    private static final int PAGE_SIZE = 5;
    private static final DateTimeFormatter LIST_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final TelegramTechnicianSessionRepository sessionRepository;
    private final TechnicianRepository technicianRepository;
    private final RepairAssignmentRepository assignmentRepository;
    private final RepairAssignmentService assignmentService;
    private final RepairExecutionService executionService;
    private final RepairRequestService requestService;
    private final AttachmentService attachmentService;
    private final TechnicianTelegramLinkService linkService;
    private final TelegramBotClient botClient;
    private final TelegramScreenService screenService;
    private final DateTimeFormatter listDateFormatter;
    private final Clock clock;

    private static final String MENU_PENDING = "menu_pending";
    private static final String MENU_ACTIVE = "menu_active";
    private static final String MENU_RECENT = "menu_recent";
    private static final String MENU_LANGUAGE = "menu_language";

    @Autowired
    public TelegramTechnicianBotService(
            TelegramTechnicianSessionRepository sessionRepository,
            TechnicianRepository technicianRepository,
            RepairAssignmentRepository assignmentRepository,
            RepairAssignmentService assignmentService,
            RepairExecutionService executionService,
            RepairRequestService requestService,
            AttachmentService attachmentService,
            TechnicianTelegramLinkService linkService,
            @Qualifier("technicianTelegramBotClient") TelegramBotClient botClient,
            TelegramScreenService screenService,
            ZoneId businessZone) {
        this(sessionRepository, technicianRepository, assignmentRepository, assignmentService, executionService,
                requestService, attachmentService, linkService, botClient, screenService, businessZone, Clock.systemUTC());
    }

    TelegramTechnicianBotService(
            TelegramTechnicianSessionRepository sessionRepository,
            TechnicianRepository technicianRepository,
            RepairAssignmentRepository assignmentRepository,
            RepairAssignmentService assignmentService,
            RepairExecutionService executionService,
            RepairRequestService requestService,
            AttachmentService attachmentService,
            TechnicianTelegramLinkService linkService,
            TelegramBotClient botClient,
            Clock clock) {
        this(sessionRepository, technicianRepository, assignmentRepository, assignmentService, executionService,
                requestService, attachmentService, linkService, botClient, new TelegramScreenService(), ZoneOffset.UTC, clock);
    }

    TelegramTechnicianBotService(
            TelegramTechnicianSessionRepository sessionRepository,
            TechnicianRepository technicianRepository,
            RepairAssignmentRepository assignmentRepository,
            RepairAssignmentService assignmentService,
            RepairExecutionService executionService,
            RepairRequestService requestService,
            AttachmentService attachmentService,
            TechnicianTelegramLinkService linkService,
            TelegramBotClient botClient,
            TelegramScreenService screenService,
            ZoneId businessZone,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.technicianRepository = technicianRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
        this.executionService = executionService;
        this.requestService = requestService;
        this.attachmentService = attachmentService;
        this.linkService = linkService;
        this.botClient = botClient;
        this.screenService = screenService;
        this.listDateFormatter = LIST_DATE_FORMATTER.withZone(businessZone != null ? businessZone : ZoneOffset.UTC);
        this.clock = clock;
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public void handle(TelegramUpdatePayload update) {
        TelegramUpdatePayload.TelegramChat chat = update.chat();
        TelegramUpdatePayload.TelegramUser sender = update.sender();
        if (chat == null || sender == null || !"private".equals(chat.type())) {
            return;
        }
        String text = trim(update.text());
        if (text != null && text.startsWith("/start tech_")) {
            linkService.requireUsableToken(linkService.hash(text.substring("/start tech_".length())));
        }
        TelegramTechnicianSession session = sessionRepository.findByTelegramUserIdForUpdate(sender.id())
                .orElseGet(() -> sessionRepository.saveAndFlush(
                        new TelegramTechnicianSession(sender.id(), chat.id(), now())));
        session.touch(chat.id(), now());
        if (update.callbackQuery() != null) {
            handleCallback(session, update.callbackQuery());
            return;
        }
        handleMessage(session, update);
    }

    public void respondBusinessError(TelegramUpdatePayload update, BusinessRuleException exception) {
        TelegramUpdatePayload.TelegramChat chat = update.chat();
        TelegramUpdatePayload.TelegramUser sender = update.sender();
        if (chat == null || sender == null || chat.id() == null) {
            return;
        }
        LanguageCode language = sessionRepository.findByTelegramUserId(sender.id())
                .map(TelegramTechnicianSession::getLanguage)
                .orElse(LanguageCode.UZ);
        botClient.sendMessage(chat.id(), businessError(language, exception.code()), null);
    }

    public void respondBusinessErrorCallback(
            TelegramUpdatePayload.TelegramCallbackQuery callback,
            BusinessRuleException exception) {
        if (callback == null || callback.id() == null) {
            return;
        }
        LanguageCode language = LanguageCode.UZ;
        if (callback.from() != null && callback.from().id() != null) {
            language = sessionRepository.findByTelegramUserId(callback.from().id())
                    .map(TelegramTechnicianSession::getLanguage)
                    .orElse(LanguageCode.UZ);
        }
        String errorMsg = businessError(language, exception.code());
        botClient.answerCallback(callback.id(), errorMsg, true);
    }

    @Transactional(readOnly = true, noRollbackFor = BusinessRuleException.class)
    public void requireSwitchAllowed(Long telegramUserId, Long telegramChatId) {
        TelegramTechnicianSession session = sessionRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new BusinessRuleException(
                        "TELEGRAM_TECHNICIAN_NOT_LINKED",
                        "Technician profile is not linked.",
                        403));
        if (session.getTechnicianId() == null) {
            throw new BusinessRuleException(
                    "TELEGRAM_TECHNICIAN_NOT_LINKED",
                    "Technician profile is not linked.",
                    403);
        }
        Technician technician = technicianRepository.findById(session.getTechnicianId())
                .orElseThrow(() -> new BusinessRuleException(
                        "TELEGRAM_TECHNICIAN_NOT_LINKED",
                        "Technician profile is not linked.",
                        403));
        if (!technician.isActive()) {
            throw new BusinessRuleException(
                    "TECHNICIAN_INACTIVE",
                    "Technician profile is not available.",
                    409);
        }
        if (technician.getTelegramUserId() == null
                || !technician.getTelegramUserId().equals(telegramUserId)
                || technician.getTelegramChatId() == null
                || !technician.getTelegramChatId().equals(telegramChatId)) {
            throw new BusinessRuleException(
                    "TELEGRAM_TECHNICIAN_NOT_LINKED",
                    "Technician profile is not linked.",
                    403);
        }
    }

    private void handleMessage(TelegramTechnicianSession session, TelegramUpdatePayload update) {
        String text = trim(update.text());
        if (text != null && text.startsWith("/start tech_")) {
            String token = text.substring("/start tech_".length()).trim();
            session.pendingLink(linkService.hash(token), now());
            send(session, "choose_language", languageKeyboard());
            return;
        }
        if ("/start".equalsIgnoreCase(text)) {
            if (linked(session)) {
                showMenu(session);
                return;
            }
            send(session, "not_linked", null);
            return;
        }
        if ("/technician".equalsIgnoreCase(text) || "/menu".equalsIgnoreCase(text)) {
            showMenu(session);
            return;
        }
        if ("/cancel".equalsIgnoreCase(text)) {
            session.clearDraft(now());
            session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
            send(session, "cancelled", mainKeyboard(session.getLanguage()));
            return;
        }
        if (!update.photo().isEmpty()) {
            handlePhoto(session, update.photo());
            return;
        }
        handleText(session, text);
    }

    private void handleText(TelegramTechnicianSession session, String text) {
        if (text == null) {
            send(session, "invalid_action", mainKeyboard(session.getLanguage()));
            return;
        }
        if (handleMenuText(session, text)) {
            return;
        }
        switch (session.getState()) {
            case AWAITING_REJECTION_REASON -> handleRejectionReason(session, text);
            case AWAITING_DIAGNOSIS -> handleDiagnosisText(session, text);
            case AWAITING_WAIT_REASON -> handleWaitReasonText(session, text);
            case AWAITING_WORK_PERFORMED -> handleWorkPerformedText(session, text);
            default -> send(session, "invalid_action", mainKeyboard(session.getLanguage()));
        }
    }

    private void handleRejectionReason(TelegramTechnicianSession session, String text) {
        Long requestId = session.getPendingRequestId();
        requireOwnedActiveAssignment(session, requestId);
        assignmentService.rejectByTechnician(
                requestId,
                new AssignmentRejectionRequest(text),
                session.getTechnicianId());
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        botClient.sendMessage(
                session.getTelegramChatId(),
                msg(session.getLanguage(), "rejected"),
                null);
        send(session, "main_menu", mainKeyboard(session.getLanguage()));
    }

    private void handleDiagnosisText(TelegramTechnicianSession session, String text) {
        Long requestId = session.getPendingRequestId();
        requireOwnedActiveAssignment(session, requestId);
        executionService.updateDiagnosisByTechnician(
                requestId,
                new DiagnosisRequest(text),
                session.getTechnicianId());
        if (session.getDraftText() != null && !session.getDraftText().isBlank()) {
            Long msgId = botClient.sendMessage(
                    session.getTelegramChatId(),
                    msg(session.getLanguage(), "diagnosis_saved"),
                    completeKeyboard(requestId, "a", 0, session.getLanguage()));
            session.activeWorkflowMessageId(msgId, now());
            return;
        }
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        RepairAssignment assignment = requireOwnedAssignmentForView(session, requestId);
        RepairRequestDetailResponse detail = requestService.get(requestId);
        Long msgId = botClient.sendMessage(
                session.getTelegramChatId(),
                msg(session.getLanguage(), "diagnosis_saved") + "\n\n" + detailText(assignment, detail, session.getLanguage()),
                jobKeyboard(assignment, "a", 0, session.getLanguage()));
        session.activeWorkflowMessageId(msgId, now());
    }

    private void handleWaitReasonText(TelegramTechnicianSession session, String text) {
        Long requestId = session.getPendingRequestId();
        requireOwnedActiveAssignment(session, requestId);
        executionService.waitForPartsByTechnician(
                requestId,
                new WaitForPartsRequest(text),
                session.getTechnicianId());
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        RepairAssignment assignment = requireOwnedAssignmentForView(session, requestId);
        RepairRequestDetailResponse detail = requestService.get(requestId);
        Long msgId = botClient.sendMessage(
                session.getTelegramChatId(),
                msg(session.getLanguage(), "waiting") + "\n\n" + detailText(assignment, detail, session.getLanguage()),
                jobKeyboard(assignment, "a", 0, session.getLanguage()));
        session.activeWorkflowMessageId(msgId, now());
    }

    private void handleWorkPerformedText(TelegramTechnicianSession session, String text) {
        Long requestId = session.getPendingRequestId();
        requireOwnedActiveAssignment(session, requestId);
        session.draftText(text, now());
        session.state(TelegramTechnicianSessionState.AWAITING_COMPLETION_PHOTO, now());
        Long msgId = botClient.sendMessage(
                session.getTelegramChatId(),
                msg(session.getLanguage(), "send_completion_photo"),
                cancelInputKeyboard(requestId, "a", 0, session.getLanguage()));
        session.activeWorkflowMessageId(msgId, now());
    }

    private boolean handleMenuText(TelegramTechnicianSession session, String text) {
        if (!linked(session)) {
            return false;
        }
        String action = menuAction(text);
        if (MENU_PENDING.equals(action)) {
            showAssignments(session, AssignmentStatus.PENDING, 0, null);
            return true;
        }
        if (MENU_ACTIVE.equals(action)) {
            showActive(session, 0, null);
            return true;
        }
        if (MENU_RECENT.equals(action)) {
            showRecent(session, 0, null);
            return true;
        }
        if (MENU_LANGUAGE.equals(action)) {
            send(session, "choose_language", languageKeyboard());
            return true;
        }
        return false;
    }

    private String menuAction(String text) {
        for (LanguageCode language : LanguageCode.values()) {
            if (msg(language, MENU_PENDING).equals(text)) {
                return MENU_PENDING;
            }
            if (msg(language, MENU_ACTIVE).equals(text)) {
                return MENU_ACTIVE;
            }
            if (msg(language, MENU_RECENT).equals(text)) {
                return MENU_RECENT;
            }
            if (msg(language, MENU_LANGUAGE).equals(text)) {
                return MENU_LANGUAGE;
            }
        }
        return "";
    }

    private void handleCallback(
            TelegramTechnicianSession session,
            TelegramUpdatePayload.TelegramCallbackQuery callback) {
        String data = callback.data() == null ? "" : callback.data();
        Long callbackMessageId = callback.message() == null ? null : callback.message().messageId();

        if (data.startsWith("tlang:")) {
            chooseLanguage(session, data.substring("tlang:".length()), callbackMessageId);
            if (callback.id() != null) {
                botClient.answerCallback(callback.id(), "");
            }
            return;
        }
        requireLinkedTechnician(session);

        if (data.equals("tmenu:main")) {
            showMenu(session);
        } else if (data.startsWith("tlist:")) {
            handleListCallback(session, data, callbackMessageId);
        } else if (data.equals("tmenu:pending")) {
            showAssignments(session, AssignmentStatus.PENDING, 0, callbackMessageId);
        } else if (data.equals("tmenu:active")) {
            showActive(session, 0, callbackMessageId);
        } else if (data.equals("tmenu:recent")) {
            showRecent(session, 0, callbackMessageId);
        } else if (data.equals("tmenu:lang")) {
            screenService.sendOrEdit(
                    botClient,
                    session.getTelegramChatId(),
                    callbackMessageId,
                    msg(session.getLanguage(), "choose_language"),
                    languageKeyboard());
        } else if (data.startsWith("tjob:")) {
            handleJobCallback(session, data, callbackMessageId);
        } else if (data.startsWith("taccept:")) {
            handleAcceptCallback(session, data, callbackMessageId);
        } else if (data.startsWith("treject:")) {
            handleRejectCallback(session, data, callback, callbackMessageId);
        } else if (data.startsWith("tstart:")) {
            handleStartCallback(session, data, callbackMessageId);
        } else if (data.startsWith("tdiagnosis:")) {
            handleDiagnosisCallback(session, data, callback, callbackMessageId);
        } else if (data.startsWith("twait:")) {
            handleWaitCallback(session, data, callback, callbackMessageId);
        } else if (data.startsWith("tresume:")) {
            handleResumeCallback(session, data, callbackMessageId);
        } else if (data.startsWith("tdiagphoto:")) {
            handleDiagPhotoCallback(session, data, callback, callbackMessageId);
        } else if (data.startsWith("twork:")) {
            handleWorkCallback(session, data, callback, callbackMessageId);
        } else if (data.startsWith("tcomplete:")) {
            handleCompleteCallback(session, data, callbackMessageId);
        } else if (data.startsWith("tcancelinput:")) {
            handleCancelInputCallback(session, data, callbackMessageId);
        } else {
            send(session, "invalid_action", mainKeyboard(session.getLanguage()));
        }

        if (callback.id() != null) {
            botClient.answerCallback(callback.id(), "");
        }
    }

    private boolean checkPendingInputCollision(
            TelegramTechnicianSession session,
            Long targetRequestId,
            TelegramUpdatePayload.TelegramCallbackQuery callback) {
        if (session.getPendingRequestId() != null
                && !session.getPendingRequestId().equals(targetRequestId)
                && session.getState() != null
                && session.getState().isAwaitingInput()) {
            if (callback.id() != null) {
                botClient.answerCallback(callback.id(), msg(session.getLanguage(), "finish_current_input_first"), true);
            }
            return false;
        }
        return true;
    }

    private void handleListCallback(TelegramTechnicianSession session, String data, Long callbackMessageId) {
        String[] parts = data.split(":");
        String origin = parts.length > 1 ? parts[1] : "p";
        int page = parts.length > 2 ? parseInt(parts[2], 0) : 0;
        if ("a".equals(origin)) {
            showActive(session, page, callbackMessageId);
        } else if ("r".equals(origin)) {
            showRecent(session, page, callbackMessageId);
        } else {
            showAssignments(session, AssignmentStatus.PENDING, page, callbackMessageId);
        }
    }

    private void handleJobCallback(TelegramTechnicianSession session, String data, Long callbackMessageId) {
        String[] parts = data.split(":");
        Long requestId = parseLong(parts[1]);
        String origin = parts.length > 2 ? parts[2] : "p";
        int page = parts.length > 3 ? parseInt(parts[3], 0) : 0;
        showJob(session, requestId, origin, page, callbackMessageId);
    }

    private void handleAcceptCallback(TelegramTechnicianSession session, String data, Long callbackMessageId) {
        String[] parts = data.split(":");
        Long requestId = parseLong(parts[1]);
        String origin = parts.length > 2 ? parts[2] : "p";
        int page = parts.length > 3 ? parseInt(parts[3], 0) : 0;
        assignmentService.acceptByTechnician(requestId, session.getTechnicianId());
        showJob(session, requestId, origin, page, callbackMessageId);
    }

    private void handleRejectCallback(
            TelegramTechnicianSession session,
            String data,
            TelegramUpdatePayload.TelegramCallbackQuery callback,
            Long callbackMessageId) {
        String[] parts = data.split(":");
        Long requestId = parseLong(parts[1]);
        String origin = parts.length > 2 ? parts[2] : "p";
        int page = parts.length > 3 ? parseInt(parts[3], 0) : 0;
        if (!checkPendingInputCollision(session, requestId, callback)) {
            return;
        }
        requireOwnedActiveAssignment(session, requestId);
        session.pendingRequest(requestId, callbackMessageId, now());
        session.state(TelegramTechnicianSessionState.AWAITING_REJECTION_REASON, now());
        Long msgId = screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                callbackMessageId,
                msg(session.getLanguage(), "send_rejection_reason"),
                cancelInputKeyboard(requestId, origin, page, session.getLanguage()));
        session.activeWorkflowMessageId(msgId, now());
    }

    private void handleStartCallback(TelegramTechnicianSession session, String data, Long callbackMessageId) {
        String[] parts = data.split(":");
        Long requestId = parseLong(parts[1]);
        String origin = parts.length > 2 ? parts[2] : "a";
        int page = parts.length > 3 ? parseInt(parts[3], 0) : 0;
        executionService.startByTechnician(requestId, session.getTechnicianId());
        showJob(session, requestId, origin, page, callbackMessageId);
    }

    private void handleDiagnosisCallback(
            TelegramTechnicianSession session,
            String data,
            TelegramUpdatePayload.TelegramCallbackQuery callback,
            Long callbackMessageId) {
        String[] parts = data.split(":");
        Long requestId = parseLong(parts[1]);
        String origin = parts.length > 2 ? parts[2] : "a";
        int page = parts.length > 3 ? parseInt(parts[3], 0) : 0;
        if (!checkPendingInputCollision(session, requestId, callback)) {
            return;
        }
        requireOwnedActiveAssignment(session, requestId);
        session.pendingRequest(requestId, callbackMessageId, now());
        session.state(TelegramTechnicianSessionState.AWAITING_DIAGNOSIS, now());
        Long msgId = screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                callbackMessageId,
                msg(session.getLanguage(), "send_diagnosis"),
                cancelInputKeyboard(requestId, origin, page, session.getLanguage()));
        session.activeWorkflowMessageId(msgId, now());
    }

    private void handleWaitCallback(
            TelegramTechnicianSession session,
            String data,
            TelegramUpdatePayload.TelegramCallbackQuery callback,
            Long callbackMessageId) {
        String[] parts = data.split(":");
        Long requestId = parseLong(parts[1]);
        String origin = parts.length > 2 ? parts[2] : "a";
        int page = parts.length > 3 ? parseInt(parts[3], 0) : 0;
        if (!checkPendingInputCollision(session, requestId, callback)) {
            return;
        }
        requireOwnedActiveAssignment(session, requestId);
        session.pendingRequest(requestId, callbackMessageId, now());
        session.state(TelegramTechnicianSessionState.AWAITING_WAIT_REASON, now());
        Long msgId = screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                callbackMessageId,
                msg(session.getLanguage(), "send_wait_reason"),
                cancelInputKeyboard(requestId, origin, page, session.getLanguage()));
        session.activeWorkflowMessageId(msgId, now());
    }

    private void handleResumeCallback(TelegramTechnicianSession session, String data, Long callbackMessageId) {
        String[] parts = data.split(":");
        Long requestId = parseLong(parts[1]);
        String origin = parts.length > 2 ? parts[2] : "a";
        int page = parts.length > 3 ? parseInt(parts[3], 0) : 0;
        executionService.resumeByTechnician(
                requestId,
                new ResumeRepairRequest(null),
                session.getTechnicianId());
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        showJob(session, requestId, origin, page, callbackMessageId);
    }

    private void handleDiagPhotoCallback(
            TelegramTechnicianSession session,
            String data,
            TelegramUpdatePayload.TelegramCallbackQuery callback,
            Long callbackMessageId) {
        String[] parts = data.split(":");
        Long requestId = parseLong(parts[1]);
        String origin = parts.length > 2 ? parts[2] : "a";
        int page = parts.length > 3 ? parseInt(parts[3], 0) : 0;
        if (!checkPendingInputCollision(session, requestId, callback)) {
            return;
        }
        requireOwnedActiveAssignment(session, requestId);
        session.pendingRequest(requestId, callbackMessageId, now());
        session.state(TelegramTechnicianSessionState.AWAITING_DIAGNOSIS_PHOTO, now());
        Long msgId = screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                callbackMessageId,
                msg(session.getLanguage(), "send_diagnosis_photo"),
                cancelInputKeyboard(requestId, origin, page, session.getLanguage()));
        session.activeWorkflowMessageId(msgId, now());
    }

    private void handleWorkCallback(
            TelegramTechnicianSession session,
            String data,
            TelegramUpdatePayload.TelegramCallbackQuery callback,
            Long callbackMessageId) {
        String[] parts = data.split(":");
        Long requestId = parseLong(parts[1]);
        String origin = parts.length > 2 ? parts[2] : "a";
        int page = parts.length > 3 ? parseInt(parts[3], 0) : 0;
        if (!checkPendingInputCollision(session, requestId, callback)) {
            return;
        }
        requireOwnedActiveAssignment(session, requestId);
        session.pendingRequest(requestId, callbackMessageId, now());
        session.state(TelegramTechnicianSessionState.AWAITING_WORK_PERFORMED, now());
        Long msgId = screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                callbackMessageId,
                msg(session.getLanguage(), "send_work"),
                cancelInputKeyboard(requestId, origin, page, session.getLanguage()));
        session.activeWorkflowMessageId(msgId, now());
    }

    private void handleCompleteCallback(TelegramTechnicianSession session, String data, Long callbackMessageId) {
        String[] parts = data.split(":");
        Long requestId = parseLong(parts[1]);
        String origin = parts.length > 2 ? parts[2] : "a";
        int page = parts.length > 3 ? parseInt(parts[3], 0) : 0;
        executionService.completeByTechnician(
                requestId,
                new CompleteRepairRequest(session.getDraftText(), null),
                session.getTechnicianId());
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        RepairRequestDetailResponse detail = requestService.get(requestId);
        String text = "✅ " + msg(session.getLanguage(), "completed") + "\n\n" + detailText(detail, session.getLanguage());
        screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                callbackMessageId,
                text,
                completedJobKeyboard(origin, page, session.getLanguage()));
    }

    private void handleCancelInputCallback(TelegramTechnicianSession session, String data, Long callbackMessageId) {
        String[] parts = data.split(":");
        Long requestId = parseLong(parts[1]);
        String origin = parts.length > 2 ? parts[2] : "a";
        int page = parts.length > 3 ? parseInt(parts[3], 0) : 0;
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        showJob(session, requestId, origin, page, callbackMessageId);
    }

    private void chooseLanguage(TelegramTechnicianSession session, String code, Long callbackMessageId) {
        LanguageCode language = parseLanguage(code);
        if (session.getPendingTokenHash() != null) {
            Technician technician = linkService.consume(
                    session.getPendingTokenHash(),
                    session.getTelegramUserId(),
                    session.getTelegramChatId(),
                    language).technician();
            session.link(technician, language, now());
            screenService.sendOrEdit(
                    botClient,
                    session.getTelegramChatId(),
                    callbackMessageId,
                    msg(language, "linked"),
                    null);
            send(session, "main_menu", mainKeyboard(language));
            return;
        }
        if (!linked(session)) {
            send(session, "not_linked", null);
            return;
        }
        Technician technician = requireLinkedTechnician(session);
        technician.updateTelegramLanguage(language, now());
        session.language(language, now());
        showMenu(session);
    }

    private void showMenu(TelegramTechnicianSession session) {
        if (!linked(session)) {
            send(session, "not_linked", null);
            return;
        }
        requireLinkedTechnician(session);
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        send(session, "main_menu", mainKeyboard(session.getLanguage()));
    }

    private void showAssignments(
            TelegramTechnicianSession session,
            AssignmentStatus status,
            int page,
            Long messageIdToEdit) {
        if (!linked(session)) {
            send(session, "not_linked", null);
            return;
        }
        requireLinkedTechnician(session);
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        Page<RepairAssignment> assignmentPage = assignmentRepository
                .findJobsByTechnicianIdAndStatusIn(
                        session.getTechnicianId(),
                        List.of(status),
                        PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        sendList(session, assignmentPage, "p", "jobs_pending_title", messageIdToEdit);
    }

    private void showActive(TelegramTechnicianSession session, int page, Long messageIdToEdit) {
        if (!linked(session)) {
            send(session, "not_linked", null);
            return;
        }
        requireLinkedTechnician(session);
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        Page<RepairAssignment> assignmentPage = assignmentRepository
                .findJobsByTechnicianIdAndStatusIn(
                        session.getTechnicianId(),
                        List.of(AssignmentStatus.ACCEPTED),
                        PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        sendList(session, assignmentPage, "a", "jobs_active_title", messageIdToEdit);
    }

    private void showRecent(TelegramTechnicianSession session, int page, Long messageIdToEdit) {
        if (!linked(session)) {
            send(session, "not_linked", null);
            return;
        }
        requireLinkedTechnician(session);
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        Page<RepairAssignment> assignmentPage = assignmentRepository
                .findJobsByTechnicianIdAndStatusIn(
                        session.getTechnicianId(),
                        List.of(AssignmentStatus.REJECTED, AssignmentStatus.COMPLETED, AssignmentStatus.CANCELLED),
                        PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        sendList(session, assignmentPage, "r", "jobs_recent_title", messageIdToEdit);
    }

    private void sendList(
            TelegramTechnicianSession session,
            Page<RepairAssignment> assignmentPage,
            String origin,
            String titleKey,
            Long messageIdToEdit) {
        if (assignmentPage.isEmpty()) {
            screenService.sendOrEdit(
                    botClient,
                    session.getTelegramChatId(),
                    messageIdToEdit,
                    msg(session.getLanguage(), "empty_jobs"),
                    emptyListKeyboard(session.getLanguage()));
            return;
        }
        String text = msg(session.getLanguage(), titleKey);
        screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                messageIdToEdit,
                text,
                paginatedListKeyboard(assignmentPage, origin, session.getLanguage()));
    }

    private void showJob(
            TelegramTechnicianSession session,
            Long requestId,
            String origin,
            int page,
            Long messageIdToEdit) {
        RepairAssignment assignment = requireOwnedAssignmentForView(session, requestId);
        RepairRequestDetailResponse detail = requestService.get(requestId);
        String text = detailText(assignment, detail, session.getLanguage());
        screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                messageIdToEdit,
                text,
                jobKeyboard(assignment, origin, page, session.getLanguage()));
    }

    private void handlePhoto(TelegramTechnicianSession session, List<TelegramUpdatePayload.TelegramPhotoSize> photos) {
        if (session.getState() != TelegramTechnicianSessionState.AWAITING_DIAGNOSIS_PHOTO
                && session.getState() != TelegramTechnicianSessionState.AWAITING_COMPLETION_PHOTO) {
            send(session, "invalid_action", mainKeyboard(session.getLanguage()));
            return;
        }
        Long requestId = session.getPendingRequestId();
        requireOwnedActiveAssignmentForUpload(session, requestId);
        TelegramUpdatePayload.TelegramPhotoSize photo = photos.stream()
                .max(Comparator.comparingLong(this::photoWeight))
                .orElseThrow(() -> new BusinessRuleException("TELEGRAM_PHOTO_INVALID", "Photo is invalid.", 400));
        String fileId = photo.fileId();
        AttachmentType type = session.getState() == TelegramTechnicianSessionState.AWAITING_DIAGNOSIS_PHOTO
                ? AttachmentType.DIAGNOSIS_PHOTO
                : AttachmentType.COMPLETION_PHOTO;
        try {
            TelegramFileMetadata metadata = botClient.getFile(fileId);
            long fileSize = metadata.fileSize() > 0 ? metadata.fileSize() : (photo.fileSize() == null ? 0 : photo.fileSize());
            if (fileSize <= 0) {
                throw new TelegramApiException("Telegram file size is unavailable.");
            }
            try (InputStream input = botClient.downloadFile(metadata.filePath(), fileSize)) {
                attachmentService.uploadFromTechnician(
                        requestId,
                        type,
                        "telegram-photo.jpg",
                        null,
                        fileSize,
                        input,
                        session.getTechnicianId());
            }
        } catch (BusinessRuleException exception) {
            LOGGER.warn(
                    "Technician Telegram photo upload failed requestId={} technicianId={} type={} code={}",
                    requestId,
                    session.getTechnicianId(),
                    type,
                    exception.code());
            throw exception;
        } catch (IOException | TelegramApiException exception) {
            LOGGER.warn(
                    "Technician Telegram photo upload failed requestId={} technicianId={} type={} errorType={} message={}",
                    requestId,
                    session.getTechnicianId(),
                    type,
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
            throw new BusinessRuleException("ATTACHMENT_STORAGE_FAILED", "Attachment upload failed.", 503);
        }
        if (type == AttachmentType.COMPLETION_PHOTO) {
            if (diagnosisPresent(requestId)) {
                Long msgId = botClient.sendMessage(
                        session.getTelegramChatId(),
                        msg(session.getLanguage(), "completion_photo_saved"),
                        completeKeyboard(requestId, "a", 0, session.getLanguage()));
                session.activeWorkflowMessageId(msgId, now());
            } else {
                session.state(TelegramTechnicianSessionState.AWAITING_DIAGNOSIS, now());
                Long msgId = botClient.sendMessage(
                        session.getTelegramChatId(),
                        msg(session.getLanguage(), "send_diagnosis"),
                        cancelInputKeyboard(requestId, "a", 0, session.getLanguage()));
                session.activeWorkflowMessageId(msgId, now());
            }
        } else {
            session.clearDraft(now());
            session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
            RepairAssignment assignment = requireOwnedAssignmentForView(session, requestId);
            RepairRequestDetailResponse detail = requestService.get(requestId);
            Long msgId = botClient.sendMessage(
                    session.getTelegramChatId(),
                    msg(session.getLanguage(), "diagnosis_photo_saved") + "\n\n" + detailText(assignment, detail, session.getLanguage()),
                    jobKeyboard(assignment, "a", 0, session.getLanguage()));
            session.activeWorkflowMessageId(msgId, now());
        }
    }

    private boolean diagnosisPresent(Long requestId) {
        RepairRequestDetailResponse detail = requestService.get(requestId);
        return detail.execution() != null && detail.execution().diagnosisPresent();
    }

    private void requireOwnedActiveAssignment(TelegramTechnicianSession session, Long requestId) {
        requireLinkedTechnician(session);
        requireOwnedAssignment(session, requestId);
    }

    private void requireOwnedActiveAssignmentForUpload(TelegramTechnicianSession session, Long requestId) {
        requireLinkedTechnicianWithoutLock(session);
        requireOwnedAssignment(session, requestId);
    }

    private void requireOwnedAssignment(TelegramTechnicianSession session, Long requestId) {
        RepairAssignment assignment = assignmentRepository
                .findActiveByRequestId(requestId, RepairAssignmentRepository.ACTIVE_STATUSES)
                .orElseThrow(() -> new BusinessRuleException("ACTIVE_ASSIGNMENT_NOT_FOUND", "Assignment not found.", 404));
        if (!assignment.getTechnician().getId().equals(session.getTechnicianId())) {
            throw new BusinessRuleException("TECHNICIAN_ASSIGNMENT_FORBIDDEN", "Repair request belongs to another technician.", 403);
        }
    }

    private RepairAssignment requireOwnedAssignmentForView(TelegramTechnicianSession session, Long requestId) {
        requireLinkedTechnician(session);
        List<RepairAssignment> assignments = assignmentRepository
                .findByRepairRequestIdAndTechnicianIdOrderByCreatedAtDesc(requestId, session.getTechnicianId());
        if (assignments.isEmpty()) {
            throw new BusinessRuleException("ASSIGNMENT_NOT_FOUND", "Assignment not found.", 404);
        }
        return assignments.getFirst();
    }

    private void requireLinkedTechnicianWithoutLock(TelegramTechnicianSession session) {
        if (session.getTechnicianId() == null) {
            throw new BusinessRuleException(
                    "TELEGRAM_TECHNICIAN_NOT_LINKED",
                    "Technician profile is not linked.",
                    403);
        }
        Technician technician = technicianRepository.findById(session.getTechnicianId())
                .orElseThrow(() -> new BusinessRuleException(
                    "TELEGRAM_TECHNICIAN_NOT_LINKED",
                    "Technician profile is not linked.",
                    403));
        if (!technician.isActive()
                || technician.getTelegramUserId() == null
                || !session.getTelegramUserId().equals(technician.getTelegramUserId())
                || technician.getTelegramChatId() == null
                || !session.getTelegramChatId().equals(technician.getTelegramChatId())) {
            session.unlink(now());
            throw new BusinessRuleException(
                    "TECHNICIAN_INACTIVE",
                    "Technician profile is not available.",
                    409);
        }
    }

    private boolean linked(TelegramTechnicianSession session) {
        try {
            requireLinkedTechnicianWithoutLock(session);
            return true;
        } catch (BusinessRuleException exception) {
            return false;
        }
    }

    private Technician requireLinkedTechnician(TelegramTechnicianSession session) {
        if (session.getTechnicianId() == null) {
            throw new BusinessRuleException(
                    "TELEGRAM_TECHNICIAN_NOT_LINKED",
                    "Technician profile is not linked.",
                    403);
        }
        Technician technician = technicianRepository.findByIdForUpdate(session.getTechnicianId())
                .orElseThrow(() -> new BusinessRuleException(
                        "TELEGRAM_TECHNICIAN_NOT_LINKED",
                        "Technician profile is not linked.",
                        403));
        if (!technician.isActive()
                || technician.getTelegramUserId() == null
                || !session.getTelegramUserId().equals(technician.getTelegramUserId())
                || technician.getTelegramChatId() == null
                || !session.getTelegramChatId().equals(technician.getTelegramChatId())) {
            session.unlink(now());
            throw new BusinessRuleException(
                    "TECHNICIAN_INACTIVE",
                    "Technician profile is not available.",
                    409);
        }
        if (technician.getPreferredLanguage() != null
                && technician.getPreferredLanguage() != session.getLanguage()) {
            session.language(technician.getPreferredLanguage(), now());
        }
        return technician;
    }

    private String detailText(RepairRequestDetailResponse detail, LanguageCode language) {
        return detailText(null, detail, language);
    }

    private String detailText(RepairAssignment assignment, RepairRequestDetailResponse detail, LanguageCode language) {
        String categoryName = switch (language) {
            case EN -> detail.category().nameEn();
            case RU -> detail.category().nameRu();
            case UZ -> detail.category().nameUz();
        };
        String locationDisplay = extractLocationDisplay(detail, language);
        StringBuilder builder = new StringBuilder();
        builder.append(msg(language, "customer")).append(": ").append(detail.customer().fullName())
                .append("\n").append(msg(language, "category")).append(": ").append(categoryName)
                .append("\n").append(msg(language, "status")).append(": ")
                .append(technicianAssignmentStatusText(assignment, detail.status(), language))
                .append("\n").append(msg(language, "description")).append(": ").append(detail.description());
        if (locationDisplay != null && !locationDisplay.isBlank()) {
            builder.append("\n").append(msg(language, "location")).append(": ").append(locationDisplay);
        }
        return builder.toString();
    }

    private String technicianAssignmentStatusText(
            RepairAssignment assignment,
            RepairRequestStatus requestStatus,
            LanguageCode language) {
        if (assignment != null && assignment.getStatus() == AssignmentStatus.REJECTED) {
            return switch (language) {
                case EN -> "❌ Rejected";
                case RU -> "❌ Отклонено";
                case UZ -> "❌ Rad etilgan";
            };
        }
        if ((assignment != null && assignment.getStatus() == AssignmentStatus.COMPLETED)
                || requestStatus == RepairRequestStatus.COMPLETED) {
            return switch (language) {
                case EN -> "✅ Completed";
                case RU -> "✅ Завершено";
                case UZ -> "✅ Yakunlangan";
            };
        }
        if ((assignment != null && assignment.getStatus() == AssignmentStatus.CANCELLED)
                || requestStatus == RepairRequestStatus.CANCELLED) {
            return switch (language) {
                case EN -> "🚫 Cancelled";
                case RU -> "🚫 Отменено";
                case UZ -> "🚫 Bekor qilingan";
            };
        }
        if (assignment != null && assignment.getStatus() == AssignmentStatus.PENDING) {
            return switch (language) {
                case EN -> "🆕 Pending";
                case RU -> "🆕 Ожидает";
                case UZ -> "🆕 Kutilmoqda";
            };
        }
        if (requestStatus == RepairRequestStatus.WAITING_FOR_PARTS) {
            return switch (language) {
                case EN -> "⏸ Waiting for parts";
                case RU -> "⏸ Ожидание запчастей";
                case UZ -> "⏸ Ehtiyot qism kutilmoqda";
            };
        }
        if (requestStatus == RepairRequestStatus.IN_PROGRESS) {
            return switch (language) {
                case EN -> "🛠 In progress";
                case RU -> "🛠 В процессе";
                case UZ -> "🛠 Jarayonda";
            };
        }
        return switch (language) {
            case EN -> "📌 Assigned";
            case RU -> "📌 Назначен";
            case UZ -> "📌 Biriktirilgan";
        };
    }

    private String extractLocationDisplay(RepairRequestDetailResponse detail, LanguageCode language) {
        if (detail.location() != null) {
            if (detail.location().address() != null && !detail.location().address().isBlank()) {
                return detail.location().address();
            }
            if (detail.location().latitude() != null && detail.location().longitude() != null) {
                return msg(language, "location_attached");
            }
        }
        if (detail.address() != null && !detail.address().isBlank()) {
            return detail.address();
        }
        if (detail.latitude() != null && detail.longitude() != null) {
            return msg(language, "location_attached");
        }
        return null;
    }

    private BigDecimal extractLatitude(RepairRequestDetailResponse detail) {
        if (detail.location() != null && detail.location().latitude() != null) {
            return detail.location().latitude();
        }
        return detail.latitude();
    }

    private BigDecimal extractLongitude(RepairRequestDetailResponse detail) {
        if (detail.location() != null && detail.location().longitude() != null) {
            return detail.location().longitude();
        }
        return detail.longitude();
    }

    private String jobLabel(RepairAssignment assignment, LanguageCode language) {
        String name = switch (language) {
            case EN -> assignment.getRepairRequest().getCategory().getNameEn();
            case RU -> assignment.getRepairRequest().getCategory().getNameRu();
            case UZ -> assignment.getRepairRequest().getCategory().getNameUz();
        };
        String date = "";
        if (assignment.getRepairRequest().getCreatedAt() != null) {
            date = listDateFormatter.format(Instant.from(assignment.getRepairRequest().getCreatedAt()));
        }
        String icon = technicianJobStatusIcon(assignment);
        return icon + " " + name + (date.isBlank() ? "" : " · " + date);
    }

    private String technicianJobStatusIcon(RepairAssignment assignment) {
        if (assignment.getStatus() == AssignmentStatus.REJECTED) {
            return "❌";
        }
        if (assignment.getStatus() == AssignmentStatus.COMPLETED) {
            return "✅";
        }
        if (assignment.getStatus() == AssignmentStatus.CANCELLED) {
            return "🚫";
        }
        if (assignment.getStatus() == AssignmentStatus.PENDING) {
            return "🆕";
        }
        return switch (assignment.getRepairRequest().getStatus()) {
            case IN_PROGRESS -> "🛠";
            case WAITING_FOR_PARTS -> "⏸";
            case ASSIGNED, SCHEDULED -> "📌";
            case COMPLETED -> "✅";
            case CANCELLED -> "🚫";
            default -> "📌";
        };
    }

    private String statusIcon(RepairRequestStatus status) {
        return switch (status) {
            case NEW -> "🆕";
            case ASSIGNED, SCHEDULED -> "📌";
            case IN_PROGRESS -> "⚙️";
            case WAITING_FOR_PARTS -> "⏳";
            case COMPLETED -> "✅";
            case CANCELLED -> "❌";
        };
    }

    private String mainKeyboard(LanguageCode language) {
        return reply(List.of(
                List.of(msg(language, MENU_PENDING), msg(language, MENU_ACTIVE)),
                List.of(msg(language, MENU_RECENT), msg(language, MENU_LANGUAGE))));
    }

    private String languageKeyboard() {
        return inline(List.of(List.of(
                button("English", "tlang:EN"),
                button("Русский", "tlang:RU"),
                button("O'zbek", "tlang:UZ"))));
    }

    private String paginatedListKeyboard(
            Page<RepairAssignment> page,
            String origin,
            LanguageCode language) {
        List<List<String>> rows = new ArrayList<>();
        for (RepairAssignment assignment : page.getContent()) {
            rows.add(List.of(button(
                    jobLabel(assignment, language),
                    "tjob:" + assignment.getRepairRequest().getId() + ":" + origin + ":" + page.getNumber())));
        }
        if (page.getNumber() > 0 || page.hasNext()) {
            List<String> paging = new ArrayList<>();
            if (page.getNumber() > 0) {
                paging.add(button(msg(language, "previous"), "tlist:" + origin + ":" + (page.getNumber() - 1)));
            }
            if (page.hasNext()) {
                paging.add(button(msg(language, "next"), "tlist:" + origin + ":" + (page.getNumber() + 1)));
            }
            rows.add(paging);
        }
        rows.add(List.of(button(msg(language, "main_menu_button"), "tmenu:main")));
        return inline(rows);
    }

    private String emptyListKeyboard(LanguageCode language) {
        return inline(List.of(List.of(button(msg(language, "main_menu_button"), "tmenu:main"))));
    }

    private String cancelInputKeyboard(Long requestId, String origin, int page, LanguageCode language) {
        return inline(List.of(List.of(button(
                msg(language, "cancel_input"),
                "tcancelinput:" + requestId + ":" + origin + ":" + page))));
    }

    private String completedJobKeyboard(String origin, int page, LanguageCode language) {
        return inline(List.of(
                List.of(button(msg(language, "back_to_jobs"), "tlist:" + origin + ":" + page)),
                List.of(button(msg(language, "main_menu_button"), "tmenu:main"))));
    }

    private String jobKeyboard(Long requestId, String origin, int page, LanguageCode language) {
        return assignmentRepository
                .findActiveByRequestId(requestId, RepairAssignmentRepository.ACTIVE_STATUSES)
                .map(assignment -> jobKeyboard(assignment, origin, page, language))
                .orElse(null);
    }

    private String jobKeyboard(
            RepairAssignment assignment,
            String origin,
            int page,
            LanguageCode language) {
        Long requestId = assignment.getRepairRequest().getId();
        List<List<String>> rows = new ArrayList<>();

        RepairRequestDetailResponse detail = requestService.get(requestId);
        BigDecimal lat = extractLatitude(detail);
        BigDecimal lon = extractLongitude(detail);
        if (lat != null && lon != null) {
            rows.add(List.of(urlButton(
                    msg(language, "open_on_map"),
                    "https://maps.google.com/?q=" + lat.toPlainString() + "," + lon.toPlainString())));
        }

        String suffix = ":" + origin + ":" + page;
        if (assignment.getStatus() == AssignmentStatus.PENDING) {
            rows.add(List.of(
                    button(msg(language, "action_accept"), "taccept:" + requestId + suffix),
                    button(msg(language, "action_reject"), "treject:" + requestId + suffix)));
        } else if (assignment.getStatus() == AssignmentStatus.ACCEPTED) {
            switch (assignment.getRepairRequest().getStatus()) {
                case ASSIGNED, SCHEDULED -> rows.add(List.of(
                        button(msg(language, "action_start"), "tstart:" + requestId + suffix)));
                case IN_PROGRESS -> {
                    rows.add(List.of(
                            button(msg(language, "action_diagnosis"), "tdiagnosis:" + requestId + suffix),
                            button(msg(language, "action_wait"), "twait:" + requestId + suffix)));
                    rows.add(List.of(
                            button(msg(language, "action_diag_photo"), "tdiagphoto:" + requestId + suffix),
                            button(msg(language, "action_complete"), "twork:" + requestId + suffix)));
                }
                case WAITING_FOR_PARTS -> rows.add(List.of(
                        button(msg(language, "action_resume"), "tresume:" + requestId + suffix),
                        button(msg(language, "action_diagnosis"), "tdiagnosis:" + requestId + suffix)));
                case NEW, COMPLETED, CANCELLED -> { }
            }
        }
        rows.add(List.of(button(msg(language, "back_to_jobs"), "tlist:" + origin + ":" + page)));
        rows.add(List.of(button(msg(language, "main_menu_button"), "tmenu:main")));
        return inline(rows);
    }

    private String completeKeyboard(Long requestId, String origin, int page, LanguageCode language) {
        String suffix = ":" + origin + ":" + page;
        return inline(List.of(
                List.of(button(msg(language, "action_complete"), "tcomplete:" + requestId + suffix)),
                List.of(button(msg(language, "cancel_input"), "tcancelinput:" + requestId + suffix))));
    }

    private String inline(List<List<String>> rows) {
        return "{\"inline_keyboard\":[" + rows.stream()
                .map(row -> "[" + String.join(",", row) + "]")
                .reduce((left, right) -> left + "," + right)
                .orElse("") + "]}";
    }

    private String reply(List<List<String>> rows) {
        return "{\"keyboard\":[" + rows.stream()
                .map(row -> "[" + row.stream()
                        .map(text -> "{\"text\":\"" + json(text) + "\"}")
                        .reduce((left, right) -> left + "," + right)
                        .orElse("") + "]")
                .reduce((left, right) -> left + "," + right)
                .orElse("") + "],\"resize_keyboard\":true,\"is_persistent\":true}";
    }

    private String button(String text, String callbackData) {
        return "{\"text\":\"" + json(text) + "\",\"callback_data\":\"" + json(callbackData) + "\"}";
    }

    private String urlButton(String text, String url) {
        return "{\"text\":\"" + json(text) + "\",\"url\":\"" + json(url) + "\"}";
    }

    private String json(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void send(TelegramTechnicianSession session, String key, String keyboard) {
        botClient.sendMessage(session.getTelegramChatId(), msg(session.getLanguage(), key), keyboard);
    }

    private String msg(LanguageCode language, String key) {
        return switch (language) {
            case EN -> switch (key) {
                case "choose_language" -> "Choose a language.";
                case "linked" -> "Technician profile linked.";
                case "not_linked" -> "Technician profile is not linked.";
                case "main_menu" -> "Technician menu";
                case MENU_PENDING -> "Pending";
                case MENU_ACTIVE -> "Active";
                case MENU_RECENT -> "Recent";
                case MENU_LANGUAGE -> "Language";
                case "jobs" -> "Jobs";
                case "jobs_pending_title" -> "🧰 Pending assignments\n\nSelect a job:";
                case "jobs_active_title" -> "🔧 Active jobs\n\nSelect a job:";
                case "jobs_recent_title" -> "📋 Recent jobs\n\nSelect a job:";
                case "empty_jobs" -> "No jobs found.";
                case "open" -> "Open";
                case "previous" -> "◀️ Previous";
                case "next" -> "Next ▶️";
                case "back_to_jobs" -> "◀️ Back to jobs";
                case "main_menu_button" -> "🏠 Main menu";
                case "action_accept" -> "Accept";
                case "action_reject" -> "Reject";
                case "action_start" -> "Start";
                case "action_diagnosis" -> "Diagnosis";
                case "action_wait" -> "Wait";
                case "action_resume" -> "Resume";
                case "action_diag_photo" -> "Diag photo";
                case "action_complete" -> "Complete";
                case "accepted" -> "Assignment accepted.";
                case "rejected" -> "Assignment rejected.";
                case "started" -> "Repair started.";
                case "send_rejection_reason" -> "Send rejection reason.";
                case "send_diagnosis" -> "Send diagnosis.";
                case "diagnosis_saved" -> "Diagnosis saved.";
                case "send_wait_reason" -> "Send waiting reason.";
                case "waiting" -> "Moved to waiting for parts.";
                case "send_resume_note" -> "Send resume note.";
                case "resumed" -> "Repair resumed.";
                case "send_diagnosis_photo" -> "Send diagnosis photo.";
                case "diagnosis_photo_saved" -> "Diagnosis photo saved.";
                case "send_work" -> "Send work performed.";
                case "send_completion_photo" -> "Send completion photo.";
                case "completion_photo_saved" -> "Completion photo saved.";
                case "completed" -> "Repair completed.";
                case "cancelled" -> "Cancelled.";
                case "cancel_input" -> "Cancel";
                case "finish_current_input_first" -> "Please finish or cancel the current input for the other request first.";
                case "open_on_map" -> "🗺 Open on map";
                case "location_attached" -> "Location attached";
                case "invalid_assignment" -> "Assignment is not available.";
                case "link_invalid" -> "This link is invalid or expired.";
                case "link_conflict" -> "This Telegram profile cannot be linked.";
                case "inactive_technician" -> "Technician profile is inactive.";
                case "upload_failed" -> "Photo could not be uploaded. Please try again.";
                case "customer" -> "Customer";
                case "category" -> "Category";
                case "status" -> "Status";
                case "description" -> "Description";
                case "location" -> "Location";
                default -> "This action is not available.";
            };
            case RU -> switch (key) {
                case "choose_language" -> "Выберите язык.";
                case "linked" -> "Профиль техника привязан.";
                case "not_linked" -> "Профиль техника не привязан.";
                case "main_menu" -> "Меню техника";
                case MENU_PENDING -> "Ожидающие";
                case MENU_ACTIVE -> "Активные";
                case MENU_RECENT -> "Недавние";
                case MENU_LANGUAGE -> "Язык";
                case "jobs" -> "Заявки";
                case "jobs_pending_title" -> "🧰 Ожидающие заявки\n\nВыберите заявку:";
                case "jobs_active_title" -> "🔧 Активные работы\n\nВыберите работу:";
                case "jobs_recent_title" -> "📋 Недавние работы\n\nВыберите работу:";
                case "empty_jobs" -> "Заявок нет.";
                case "open" -> "Открыть";
                case "previous" -> "◀️ Назад";
                case "next" -> "Далее ▶️";
                case "back_to_jobs" -> "◀️ Назад к заявкам";
                case "main_menu_button" -> "🏠 Главное меню";
                case "action_accept" -> "Принять";
                case "action_reject" -> "Отклонить";
                case "action_start" -> "Начать";
                case "action_diagnosis" -> "Диагностика";
                case "action_wait" -> "Ожидание";
                case "action_resume" -> "Возобновить";
                case "action_diag_photo" -> "Фото диагностики";
                case "action_complete" -> "Завершить";
                case "accepted" -> "Заявка принята.";
                case "rejected" -> "Заявка отклонена.";
                case "started" -> "Ремонт начат.";
                case "send_rejection_reason" -> "Отправьте причину отказа.";
                case "send_diagnosis" -> "Отправьте диагностику.";
                case "diagnosis_saved" -> "Диагностика сохранена.";
                case "send_wait_reason" -> "Отправьте причину ожидания.";
                case "waiting" -> "Заявка переведена в ожидание запчастей.";
                case "send_resume_note" -> "Отправьте заметку о возобновлении.";
                case "resumed" -> "Ремонт возобновлен.";
                case "send_diagnosis_photo" -> "Отправьте фото диагностики.";
                case "diagnosis_photo_saved" -> "Фото диагностики сохранено.";
                case "send_work" -> "Опишите выполненную работу.";
                case "send_completion_photo" -> "Отправьте фото завершения.";
                case "completion_photo_saved" -> "Фото завершения сохранено.";
                case "completed" -> "Ремонт завершен.";
                case "cancelled" -> "Отменено.";
                case "cancel_input" -> "Отменить";
                case "finish_current_input_first" -> "Пожалуйста, сначала завершите или отмените ввод для другой заявки.";
                case "open_on_map" -> "🗺 Открыть на карте";
                case "location_attached" -> "Местоположение указано";
                case "invalid_assignment" -> "Заявка недоступна.";
                case "link_invalid" -> "Ссылка недействительна или истекла.";
                case "link_conflict" -> "Этот Telegram-профиль нельзя привязать.";
                case "inactive_technician" -> "Профиль техника неактивен.";
                case "upload_failed" -> "Фото не удалось загрузить. Попробуйте снова.";
                case "customer" -> "Клиент";
                case "category" -> "Категория";
                case "status" -> "Статус";
                case "description" -> "Описание";
                case "location" -> "Адрес";
                default -> "Это действие недоступно.";
            };
            case UZ -> switch (key) {
                case "choose_language" -> "Tilni tanlang.";
                case "linked" -> "Texnik profili bog'landi.";
                case "not_linked" -> "Texnik profili bog'lanmagan.";
                case "main_menu" -> "Texnik menyusi";
                case MENU_PENDING -> "Kutilayotgan";
                case MENU_ACTIVE -> "Faol";
                case MENU_RECENT -> "So'nggi";
                case MENU_LANGUAGE -> "Til";
                case "jobs" -> "Ishlar";
                case "jobs_pending_title" -> "🧰 Kutilayotgan topshiriqlar\n\nTopshiriqni tanlang:";
                case "jobs_active_title" -> "🔧 Faol ishlar\n\nIshni tanlang:";
                case "jobs_recent_title" -> "📋 So'nggi ishlar\n\nIshni tanlang:";
                case "empty_jobs" -> "Ishlar topilmadi.";
                case "open" -> "Ochish";
                case "previous" -> "◀️ Oldingi";
                case "next" -> "Keyingi ▶️";
                case "back_to_jobs" -> "◀️ Ishlar ro'yxatiga qaytish";
                case "main_menu_button" -> "🏠 Asosiy menyu";
                case "action_accept" -> "Qabul qilish";
                case "action_reject" -> "Rad etish";
                case "action_start" -> "Boshlash";
                case "action_diagnosis" -> "Tashxis";
                case "action_wait" -> "Kutish";
                case "action_resume" -> "Davom ettirish";
                case "action_diag_photo" -> "Tashxis fotosi";
                case "action_complete" -> "Yakunlash";
                case "accepted" -> "Topshiriq qabul qilindi.";
                case "rejected" -> "Topshiriq rad etildi.";
                case "started" -> "Ta'mirlash boshlandi.";
                case "send_rejection_reason" -> "Rad etish sababini yuboring.";
                case "send_diagnosis" -> "Tashxis matnini yuboring.";
                case "diagnosis_saved" -> "Tashxis saqlandi.";
                case "send_wait_reason" -> "Kutish sababini yuboring.";
                case "waiting" -> "Ehtiyot qismlar kutilmoqda holatiga o'tkazildi.";
                case "send_resume_note" -> "Davom ettirish izohini yuboring.";
                case "resumed" -> "Ta'mirlash davom ettirildi.";
                case "send_diagnosis_photo" -> "Tashxis fotosuratini yuboring.";
                case "diagnosis_photo_saved" -> "Tashxis fotosurati saqlandi.";
                case "send_work" -> "Bajarilgan ishni yozing.";
                case "send_completion_photo" -> "Yakunlash fotosuratini yuboring.";
                case "completion_photo_saved" -> "Yakunlash fotosurati saqlandi.";
                case "completed" -> "Ta'mirlash yakunlandi.";
                case "cancelled" -> "Bekor qilindi.";
                case "cancel_input" -> "Bekor qilish";
                case "finish_current_input_first" -> "Iltimos, avval boshqa ariza uchun kiritishni yakunlang yoki bekor qiling.";
                case "open_on_map" -> "🗺 Xaritada ochish";
                case "location_attached" -> "Joylashuv biriktirilgan";
                case "invalid_assignment" -> "Topshiriq mavjud emas.";
                case "link_invalid" -> "Havola yaroqsiz yoki muddati tugagan.";
                case "link_conflict" -> "Bu Telegram profilini bog'lab bo'lmaydi.";
                case "inactive_technician" -> "Texnik profili faol emas.";
                case "upload_failed" -> "Foto yuklanmadi. Qayta urinib ko'ring.";
                case "customer" -> "Mijoz";
                case "category" -> "Kategoriya";
                case "status" -> "Holat";
                case "description" -> "Tavsif";
                case "location" -> "Manzil";
                default -> "Bu amal mavjud emas.";
            };
        };
    }

    private String businessError(LanguageCode language, String code) {
        String key = switch (code) {
            case "COMPLETION_PHOTO_REQUIRED" -> "send_completion_photo";
            case "ACTIVE_ACCEPTED_ASSIGNMENT_REQUIRED", "TECHNICIAN_ASSIGNMENT_FORBIDDEN",
                    "ACTIVE_ASSIGNMENT_NOT_FOUND", "ASSIGNMENT_NOT_PENDING",
                    "REPAIR_REQUEST_NOT_ASSIGNABLE" -> "invalid_assignment";
            case "TECHNICIAN_INACTIVE" -> "inactive_technician";
            case "TELEGRAM_TECHNICIAN_NOT_LINKED" -> "not_linked";
            case "TELEGRAM_TECHNICIAN_LINK_INVALID", "TECHNICIAN_LINK_TOKEN_ALREADY_USED" -> "link_invalid";
            case "TECHNICIAN_TELEGRAM_ALREADY_LINKED", "TECHNICIAN_TELEGRAM_ACCESS_DENIED",
                    "TELEGRAM_TECHNICIAN_LINK_CONFLICT" -> "link_conflict";
            case "ATTACHMENT_STORAGE_FAILED" -> "upload_failed";
            default -> "invalid_action";
        };
        return msg(language, key);
    }

    private String status(RepairRequestStatus status, LanguageCode language) {
        return switch (language) {
            case EN -> switch (status) {
                case NEW -> "New";
                case ASSIGNED -> "Assigned";
                case SCHEDULED -> "Scheduled";
                case IN_PROGRESS -> "In progress";
                case WAITING_FOR_PARTS -> "Waiting for parts";
                case COMPLETED -> "Completed";
                case CANCELLED -> "Cancelled";
            };
            case RU -> switch (status) {
                case NEW -> "Новая";
                case ASSIGNED -> "Назначена";
                case SCHEDULED -> "Запланирована";
                case IN_PROGRESS -> "В работе";
                case WAITING_FOR_PARTS -> "Ожидает запчасти";
                case COMPLETED -> "Завершена";
                case CANCELLED -> "Отменена";
            };
            case UZ -> switch (status) {
                case NEW -> "Yangi";
                case ASSIGNED -> "Biriktirilgan";
                case SCHEDULED -> "Rejalashtirilgan";
                case IN_PROGRESS -> "Jarayonda";
                case WAITING_FOR_PARTS -> "Ehtiyot qismlar kutilmoqda";
                case COMPLETED -> "Yakunlangan";
                case CANCELLED -> "Bekor qilingan";
            };
        };
    }

    private LanguageCode parseLanguage(String code) {
        try {
            return LanguageCode.valueOf(code.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            return LanguageCode.UZ;
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (RuntimeException exception) {
            throw new BusinessRuleException("INVALID_CALLBACK", "Invalid callback.", 400);
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private long photoWeight(TelegramUpdatePayload.TelegramPhotoSize photo) {
        if (photo.fileSize() != null) {
            return photo.fileSize();
        }
        long width = photo.width() == null ? 0 : photo.width();
        long height = photo.height() == null ? 0 : photo.height();
        return width * height;
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
