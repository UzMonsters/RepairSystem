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
import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianSession;
import com.example.darks.repair_auto.telegram.technician.domain.TelegramTechnicianSessionState;
import com.example.darks.repair_auto.telegram.technician.infrastructure.TelegramTechnicianSessionRepository;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class TelegramTechnicianBotService {

    private final TelegramTechnicianSessionRepository sessionRepository;
    private final TechnicianRepository technicianRepository;
    private final RepairAssignmentRepository assignmentRepository;
    private final RepairAssignmentService assignmentService;
    private final RepairExecutionService executionService;
    private final RepairRequestService requestService;
    private final AttachmentService attachmentService;
    private final TechnicianTelegramLinkService linkService;
    private final TelegramBotClient botClient;
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
            @Qualifier("technicianTelegramBotClient") TelegramBotClient botClient) {
        this(sessionRepository, technicianRepository, assignmentRepository, assignmentService, executionService,
                requestService, attachmentService, linkService, botClient, Clock.systemUTC());
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
        this.sessionRepository = sessionRepository;
        this.technicianRepository = technicianRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
        this.executionService = executionService;
        this.requestService = requestService;
        this.attachmentService = attachmentService;
        this.linkService = linkService;
        this.botClient = botClient;
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
        TelegramTechnicianSession session = sessionRepository.findByTelegramUserId(sender.id())
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

    @Transactional(readOnly = true, noRollbackFor = BusinessRuleException.class)
    public void requireSwitchAllowed(Long telegramUserId, Long telegramChatId) {
        TelegramTechnicianSession session = sessionRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new BusinessRuleException(
                        "TELEGRAM_TECHNICIAN_NOT_LINKED",
                        "Technician profile is not linked.",
                        403));
        if (!telegramChatId.equals(session.getTelegramChatId())) {
            throw new BusinessRuleException(
                    "TELEGRAM_TECHNICIAN_NOT_LINKED",
                    "Technician profile is not linked.",
                    403);
        }
        requireLinkedTechnician(session);
    }

    private void handleMessage(TelegramTechnicianSession session, TelegramUpdatePayload update) {
        String text = trim(update.text());
        if (text != null && text.startsWith("/start tech_")) {
            session.pendingLink(linkService.hash(text.substring("/start tech_".length())), now());
            send(session, "choose_language", languageKeyboard());
            return;
        }
        if ("/technician".equalsIgnoreCase(text) || "/menu".equalsIgnoreCase(text)) {
            showMenu(session);
            return;
        }
        if ("/start".equalsIgnoreCase(text)) {
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
        if (!linked(session)) {
            send(session, "not_linked", null);
            return;
        }
        if (text == null) {
            send(session, "invalid_action", mainKeyboard(session.getLanguage()));
            return;
        }
        if (handleMenuText(session, text)) {
            return;
        }
        Long requestId = session.getSelectedRequestId();
        switch (session.getState()) {
            case AWAITING_REJECTION_REASON -> {
                assignmentService.rejectByTechnician(requestId, new AssignmentRejectionRequest(text), session.getTechnicianId());
                session.clearDraft(now());
                session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
                send(session, "rejected", mainKeyboard(session.getLanguage()));
            }
            case AWAITING_DIAGNOSIS -> {
                executionService.updateDiagnosisByTechnician(requestId, new DiagnosisRequest(text), session.getTechnicianId());
                session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
                send(session, "diagnosis_saved", jobKeyboard(requestId, session.getLanguage()));
            }
            case AWAITING_WAIT_REASON -> {
                executionService.waitForPartsByTechnician(requestId, new WaitForPartsRequest(text), session.getTechnicianId());
                session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
                send(session, "waiting", jobKeyboard(requestId, session.getLanguage()));
            }
            case AWAITING_RESUME_NOTE -> {
                executionService.resumeByTechnician(requestId, new ResumeRepairRequest(text), session.getTechnicianId());
                session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
                send(session, "resumed", jobKeyboard(requestId, session.getLanguage()));
            }
            case AWAITING_WORK_PERFORMED -> {
                session.draftText(text, now());
                session.state(TelegramTechnicianSessionState.AWAITING_COMPLETION_PHOTO, now());
                send(session, "send_completion_photo", null);
            }
            default -> send(session, "invalid_action", mainKeyboard(session.getLanguage()));
        }
    }

    private boolean handleMenuText(TelegramTechnicianSession session, String text) {
        String action = menuAction(text);
        if (MENU_PENDING.equals(action)) {
            showAssignments(session, AssignmentStatus.PENDING);
            return true;
        }
        if (MENU_ACTIVE.equals(action)) {
            showActive(session);
            return true;
        }
        if (MENU_RECENT.equals(action)) {
            showRecent(session);
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
        if (data.startsWith("tlang:")) {
            chooseLanguage(session, data.substring("tlang:".length()));
            if (callback.id() != null) {
                botClient.answerCallback(callback.id(), "");
            }
            return;
        }
        requireLinkedTechnician(session);
        if (data.equals("tmenu:pending")) {
            showAssignments(session, AssignmentStatus.PENDING);
        } else if (data.equals("tmenu:active")) {
            showActive(session);
        } else if (data.equals("tmenu:recent")) {
            showRecent(session);
        } else if (data.equals("tmenu:lang")) {
            send(session, "choose_language", languageKeyboard());
        } else if (data.startsWith("tjob:")) {
            showJob(session, parseId(data, "tjob:"));
        } else if (data.startsWith("taccept:")) {
            Long requestId = parseId(data, "taccept:");
            assignmentService.acceptByTechnician(requestId, session.getTechnicianId());
            send(session, "accepted", jobKeyboard(requestId, session.getLanguage()));
        } else if (data.startsWith("treject:")) {
            session.selectRequest(parseId(data, "treject:"), now());
            session.state(TelegramTechnicianSessionState.AWAITING_REJECTION_REASON, now());
            send(session, "send_rejection_reason", null);
        } else if (data.startsWith("tstart:")) {
            Long requestId = parseId(data, "tstart:");
            executionService.startByTechnician(requestId, session.getTechnicianId());
            send(session, "started", jobKeyboard(requestId, session.getLanguage()));
        } else if (data.startsWith("tdiagnosis:")) {
            session.selectRequest(parseId(data, "tdiagnosis:"), now());
            session.state(TelegramTechnicianSessionState.AWAITING_DIAGNOSIS, now());
            send(session, "send_diagnosis", null);
        } else if (data.startsWith("twait:")) {
            session.selectRequest(parseId(data, "twait:"), now());
            session.state(TelegramTechnicianSessionState.AWAITING_WAIT_REASON, now());
            send(session, "send_wait_reason", null);
        } else if (data.startsWith("tresume:")) {
            Long requestId = parseId(data, "tresume:");
            executionService.resumeByTechnician(
                    requestId,
                    new ResumeRepairRequest(null),
                    session.getTechnicianId());
            session.clearDraft(now());
            session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
            send(session, "resumed", jobKeyboard(requestId, session.getLanguage()));
        } else if (data.startsWith("tdiagphoto:")) {
            session.selectRequest(parseId(data, "tdiagphoto:"), now());
            session.state(TelegramTechnicianSessionState.AWAITING_DIAGNOSIS_PHOTO, now());
            send(session, "send_diagnosis_photo", null);
        } else if (data.startsWith("twork:")) {
            session.selectRequest(parseId(data, "twork:"), now());
            session.state(TelegramTechnicianSessionState.AWAITING_WORK_PERFORMED, now());
            send(session, "send_work", null);
        } else if (data.startsWith("tcomplete:")) {
            Long requestId = parseId(data, "tcomplete:");
            executionService.completeByTechnician(
                    requestId,
                    new CompleteRepairRequest(session.getDraftText(), null),
                    session.getTechnicianId());
            session.clearDraft(now());
            send(session, "completed", mainKeyboard(session.getLanguage()));
        } else {
            send(session, "invalid_action", mainKeyboard(session.getLanguage()));
        }
        if (callback.id() != null) {
            botClient.answerCallback(callback.id(), "");
        }
    }

    private void chooseLanguage(TelegramTechnicianSession session, String code) {
        LanguageCode language = parseLanguage(code);
        if (session.getPendingTokenHash() != null) {
            Technician technician = linkService.consume(
                    session.getPendingTokenHash(),
                    session.getTelegramUserId(),
                    session.getTelegramChatId(),
                    language).technician();
            session.link(technician, language, now());
            send(session, "linked", mainKeyboard(language));
            return;
        }
        session.language(language, now());
        showMenu(session);
    }

    private void showMenu(TelegramTechnicianSession session) {
        if (!linked(session)) {
            send(session, "not_linked", null);
            return;
        }
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        send(session, "main_menu", mainKeyboard(session.getLanguage()));
    }

    private void showAssignments(TelegramTechnicianSession session, AssignmentStatus status) {
        if (!linked(session)) {
            send(session, "not_linked", null);
            return;
        }
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        List<RepairAssignment> assignments = assignmentRepository
                .findByTechnicianIdAndStatusOrderByCreatedAtDesc(session.getTechnicianId(), status);
        sendList(session, assignments);
    }

    private void showActive(TelegramTechnicianSession session) {
        if (!linked(session)) {
            send(session, "not_linked", null);
            return;
        }
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        sendList(session, assignmentRepository.findByTechnicianIdAndStatusInOrderByCreatedAtDesc(
                session.getTechnicianId(),
                List.of(AssignmentStatus.ACCEPTED)));
    }

    private void showRecent(TelegramTechnicianSession session) {
        if (!linked(session)) {
            send(session, "not_linked", null);
            return;
        }
        session.clearDraft(now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        sendList(session, assignmentRepository.findByTechnicianIdAndStatusInOrderByCreatedAtDesc(
                session.getTechnicianId(),
                List.of(AssignmentStatus.REJECTED, AssignmentStatus.COMPLETED, AssignmentStatus.CANCELLED)));
    }

    private void sendList(TelegramTechnicianSession session, List<RepairAssignment> assignments) {
        if (assignments.isEmpty()) {
            send(session, "empty_jobs", mainKeyboard(session.getLanguage()));
            return;
        }
        List<RepairAssignment> visibleAssignments = assignments.stream().limit(10).toList();
        StringBuilder builder = new StringBuilder(msg(session.getLanguage(), "jobs"));
        for (int index = 0; index < visibleAssignments.size(); index++) {
            RepairAssignment assignment = visibleAssignments.get(index);
            builder.append("\n")
                    .append(index + 1)
                    .append(". ")
                    .append(jobLabel(assignment, session.getLanguage()))
                    .append(" | ")
                    .append(status(assignment.getRepairRequest().getStatus(), session.getLanguage()));
        }
        botClient.sendMessage(
                session.getTelegramChatId(),
                builder.toString(),
                listKeyboard(visibleAssignments, session.getLanguage()));
    }

    private void showJob(TelegramTechnicianSession session, Long requestId) {
        requireOwnedActiveAssignment(session, requestId);
        session.selectRequest(requestId, now());
        session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
        RepairRequestDetailResponse detail = requestService.get(requestId);
        botClient.sendMessage(
                session.getTelegramChatId(),
                detailText(detail, session.getLanguage()),
                jobKeyboard(requestId, session.getLanguage()));
    }

    private void handlePhoto(TelegramTechnicianSession session, List<TelegramUpdatePayload.TelegramPhotoSize> photos) {
        if (session.getState() != TelegramTechnicianSessionState.AWAITING_DIAGNOSIS_PHOTO
                && session.getState() != TelegramTechnicianSessionState.AWAITING_COMPLETION_PHOTO) {
            send(session, "invalid_action", mainKeyboard(session.getLanguage()));
            return;
        }
        Long requestId = session.getSelectedRequestId();
        requireOwnedActiveAssignmentForUpload(session, requestId);
        String fileId = photos.stream()
                .max(Comparator.comparingLong(this::photoWeight))
                .map(TelegramUpdatePayload.TelegramPhotoSize::fileId)
                .orElseThrow(() -> new BusinessRuleException("TELEGRAM_PHOTO_INVALID", "Photo is invalid.", 400));
        AttachmentType type = session.getState() == TelegramTechnicianSessionState.AWAITING_DIAGNOSIS_PHOTO
                ? AttachmentType.DIAGNOSIS_PHOTO
                : AttachmentType.COMPLETION_PHOTO;
        try {
            TelegramFileMetadata metadata = botClient.getFile(fileId);
            try (InputStream input = botClient.downloadFile(metadata.filePath(), metadata.fileSize())) {
                attachmentService.uploadFromTechnician(
                        requestId,
                        type,
                        "telegram-photo.jpg",
                        null,
                        metadata.fileSize(),
                        input,
                        session.getTechnicianId());
            }
        } catch (IOException | TelegramApiException exception) {
            throw new BusinessRuleException("ATTACHMENT_STORAGE_FAILED", "Attachment upload failed.", 503);
        }
        if (type == AttachmentType.COMPLETION_PHOTO) {
            send(session, "completion_photo_saved", completeKeyboard(requestId, session.getLanguage()));
        } else {
            session.state(TelegramTechnicianSessionState.MAIN_MENU, now());
            send(session, "diagnosis_photo_saved", jobKeyboard(requestId, session.getLanguage()));
        }
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
            requireLinkedTechnician(session);
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
        return technician;
    }

    private String detailText(RepairRequestDetailResponse detail, LanguageCode language) {
        return msg(language, "customer") + ": " + detail.customer().fullName()
                + "\n" + msg(language, "category") + ": " + switch (language) {
                    case EN -> detail.category().nameEn();
                    case RU -> detail.category().nameRu();
                    case UZ -> detail.category().nameUz();
                }
                + "\n" + msg(language, "status") + ": " + status(detail.status(), language)
                + "\n" + msg(language, "description") + ": " + detail.description()
                + "\n" + msg(language, "location") + ": " + (detail.address() == null
                ? detail.latitude() + ", " + detail.longitude()
                : detail.address());
    }

    private String jobLabel(RepairAssignment assignment, LanguageCode language) {
        return switch (language) {
            case EN -> assignment.getRepairRequest().getCategory().getNameEn();
            case RU -> assignment.getRepairRequest().getCategory().getNameRu();
            case UZ -> assignment.getRepairRequest().getCategory().getNameUz();
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

    private String listKeyboard(List<RepairAssignment> assignments, LanguageCode language) {
        List<List<String>> rows = new java.util.ArrayList<>();
        for (int index = 0; index < assignments.size(); index++) {
            RepairAssignment assignment = assignments.get(index);
            rows.add(List.of(button(
                    msg(language, "open") + " " + (index + 1),
                    "tjob:" + assignment.getRepairRequest().getId())));
        }
        return inline(rows);
    }

    private String jobKeyboard(Long requestId, LanguageCode language) {
        return assignmentRepository
                .findActiveByRequestId(requestId, RepairAssignmentRepository.ACTIVE_STATUSES)
                .map(assignment -> jobKeyboard(assignment, language))
                .orElse(null);
    }

    private String jobKeyboard(RepairAssignment assignment, LanguageCode language) {
        Long requestId = assignment.getRepairRequest().getId();
        if (assignment.getStatus() == AssignmentStatus.PENDING) {
            return inline(List.of(List.of(
                    button(msg(language, "action_accept"), "taccept:" + requestId),
                    button(msg(language, "action_reject"), "treject:" + requestId))));
        }
        if (assignment.getStatus() != AssignmentStatus.ACCEPTED) {
            return null;
        }
        return switch (assignment.getRepairRequest().getStatus()) {
            case ASSIGNED, SCHEDULED -> inline(List.of(List.of(
                    button(msg(language, "action_start"), "tstart:" + requestId))));
            case IN_PROGRESS -> inline(List.of(
                    List.of(button(msg(language, "action_diagnosis"), "tdiagnosis:" + requestId),
                            button(msg(language, "action_wait"), "twait:" + requestId)),
                    List.of(button(msg(language, "action_diag_photo"), "tdiagphoto:" + requestId),
                            button(msg(language, "action_complete"), "twork:" + requestId))));
            case WAITING_FOR_PARTS -> inline(List.of(List.of(
                    button(msg(language, "action_resume"), "tresume:" + requestId),
                    button(msg(language, "action_diagnosis"), "tdiagnosis:" + requestId))));
            case NEW, COMPLETED, CANCELLED -> null;
        };
    }

    private String completeKeyboard(Long requestId, LanguageCode language) {
        return inline(List.of(List.of(button(msg(language, "action_complete"), "tcomplete:" + requestId))));
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
                case "empty_jobs" -> "No jobs found.";
                case "open" -> "Open";
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
                case "empty_jobs" -> "Заявок нет.";
                case "open" -> "Открыть";
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
                case "empty_jobs" -> "Ishlar topilmadi.";
                case "open" -> "Ochish";
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

    private Long parseId(String data, String prefix) {
        try {
            return Long.valueOf(data.substring(prefix.length()));
        } catch (RuntimeException exception) {
            throw new BusinessRuleException("INVALID_CALLBACK", "Invalid callback.", 400);
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
