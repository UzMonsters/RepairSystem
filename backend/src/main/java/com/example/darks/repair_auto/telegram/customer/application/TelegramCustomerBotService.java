package com.example.darks.repair_auto.telegram.customer.application;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.customer.application.CustomerService;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestCategorySummary;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestDetailResponse;
import com.example.darks.repair_auto.repair.request.application.RepairRequestQuery;
import com.example.darks.repair_auto.repair.request.application.RepairRequestService;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.repair.request.domain.RepairRequestStatus;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import com.example.darks.repair_auto.review.application.CustomerReviewSummary;
import com.example.darks.repair_auto.review.application.EligibleReviewRequest;
import com.example.darks.repair_auto.review.application.RepairReviewService;
import com.example.darks.repair_auto.review.domain.RepairReview;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.telegram.core.api.TelegramUpdatePayload;
import com.example.darks.repair_auto.telegram.core.application.TelegramBotClient;
import com.example.darks.repair_auto.telegram.core.application.TelegramFileMetadata;
import com.example.darks.repair_auto.telegram.core.application.TelegramScreenService;
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramProperties;
import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSession;
import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSessionState;
import com.example.darks.repair_auto.telegram.customer.infrastructure.TelegramCustomerSessionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramCustomerBotService {

    private static final int HISTORY_PAGE_SIZE = 5;
    private static final DateTimeFormatter LIST_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DETAILS_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
    private static final DateTimeFormatter TELEGRAM_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final TelegramCustomerSessionRepository sessionRepository;
    private final RepairCategoryRepository categoryRepository;
    private final RepairRequestRepository requestRepository;
    private final CustomerService customerService;
    private final RepairRequestService repairRequestService;
    private final RepairReviewService reviewService;
    private final TelegramCustomerPhotoService photoService;
    private final TelegramBotClient botClient;
    private final TelegramScreenService screenService;
    private final TelegramMessages messages;
    private final TelegramKeyboards keyboards;
    private final TelegramProperties properties;
    private final DateTimeFormatter listDateFormatter;
    private final DateTimeFormatter detailsDateFormatter;
    private final DateTimeFormatter telegramDateFormatter;
    private final Clock clock;

    @Autowired
    public TelegramCustomerBotService(
            TelegramCustomerSessionRepository sessionRepository,
            RepairCategoryRepository categoryRepository,
            RepairRequestRepository requestRepository,
            CustomerService customerService,
            RepairRequestService repairRequestService,
            RepairReviewService reviewService,
            TelegramCustomerPhotoService photoService,
            @Qualifier("customerTelegramBotClient") TelegramBotClient botClient,
            TelegramScreenService screenService,
            TelegramMessages messages,
            TelegramKeyboards keyboards,
            TelegramProperties properties,
            ZoneId businessZone) {
        this(
                sessionRepository,
                categoryRepository,
                requestRepository,
                customerService,
                repairRequestService,
                reviewService,
                photoService,
                botClient,
                screenService,
                messages,
                keyboards,
                properties,
                businessZone,
                Clock.systemUTC());
    }

    TelegramCustomerBotService(
            TelegramCustomerSessionRepository sessionRepository,
            RepairCategoryRepository categoryRepository,
            RepairRequestRepository requestRepository,
            CustomerService customerService,
            RepairRequestService repairRequestService,
            RepairReviewService reviewService,
            TelegramCustomerPhotoService photoService,
            TelegramBotClient botClient,
            TelegramMessages messages,
            TelegramKeyboards keyboards,
            TelegramProperties properties,
            ZoneId businessZone,
            Clock clock) {
        this(
                sessionRepository,
                categoryRepository,
                requestRepository,
                customerService,
                repairRequestService,
                reviewService,
                photoService,
                botClient,
                new TelegramScreenService(),
                messages,
                keyboards,
                properties,
                businessZone,
                clock);
    }

    TelegramCustomerBotService(
            TelegramCustomerSessionRepository sessionRepository,
            RepairCategoryRepository categoryRepository,
            RepairRequestRepository requestRepository,
            CustomerService customerService,
            RepairRequestService repairRequestService,
            RepairReviewService reviewService,
            TelegramCustomerPhotoService photoService,
            TelegramBotClient botClient,
            TelegramScreenService screenService,
            TelegramMessages messages,
            TelegramKeyboards keyboards,
            TelegramProperties properties,
            ZoneId businessZone,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.categoryRepository = categoryRepository;
        this.requestRepository = requestRepository;
        this.customerService = customerService;
        this.repairRequestService = repairRequestService;
        this.reviewService = reviewService;
        this.photoService = photoService;
        this.botClient = botClient;
        this.screenService = screenService;
        this.messages = messages;
        this.keyboards = keyboards;
        this.properties = properties;
        this.listDateFormatter = LIST_DATE_FORMATTER.withZone(businessZone);
        this.detailsDateFormatter = DETAILS_DATE_FORMATTER.withZone(businessZone);
        this.telegramDateFormatter = TELEGRAM_DATE_FORMATTER.withZone(businessZone);
        this.clock = clock;
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public void handle(TelegramUpdatePayload update) {
        TelegramUpdatePayload.TelegramChat chat = update.chat();
        TelegramUpdatePayload.TelegramUser sender = update.sender();
        if (chat == null || sender == null || !"private".equals(chat.type())) {
            return;
        }
        TelegramCustomerSession session = sessionRepository.findByTelegramUserIdForUpdate(sender.id())
                .orElseGet(() -> sessionRepository.saveAndFlush(
                        new TelegramCustomerSession(sender.id(), chat.id(), now())));
        session.touch(chat.id(), now());
        if (update.callbackQuery() != null) {
            handleCallback(session, update.callbackQuery());
            return;
        }
        handleMessage(session, update);
    }

    @Transactional(readOnly = true, noRollbackFor = BusinessRuleException.class)
    public void requireSwitchAllowed(Long telegramUserId, Long telegramChatId) {
        TelegramCustomerSession session = sessionRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new BusinessRuleException(
                        "TELEGRAM_CUSTOMER_NOT_LINKED",
                        "Customer profile is not linked.",
                        403));
        Customer customer = session.getCustomer();
        if (customer == null
                || !customer.isActive()
                || customer.getTelegramUserId() == null
                || !customer.getTelegramUserId().equals(telegramUserId)
                || customer.getTelegramChatId() == null
                || !customer.getTelegramChatId().equals(telegramChatId)) {
            throw new BusinessRuleException(
                    "TELEGRAM_CUSTOMER_NOT_LINKED",
                    "Customer profile is not linked.",
                    403);
        }
    }

    private void handleMessage(TelegramCustomerSession session, TelegramUpdatePayload update) {
        String text = trim(update.text());
        if ("/start".equalsIgnoreCase(text)) {
            session.clearDraft(now());
            session.state(TelegramCustomerSessionState.LANGUAGE_SELECTION, now());
            send(session, "choose_language", keyboards.language());
            return;
        }
        if ("/cancel".equalsIgnoreCase(text)) {
            session.clearDraft(now());
            session.state(session.getCustomerId() == null
                    ? TelegramCustomerSessionState.LANGUAGE_SELECTION
                    : TelegramCustomerSessionState.MAIN_MENU, now());
            send(session, "cancelled", session.getCustomerId() == null ? keyboards.language() : mainKeyboard(session));
            return;
        }
        if ("/menu".equalsIgnoreCase(text)) {
            showMenu(session);
            return;
        }
        if ("/customer".equalsIgnoreCase(text)) {
            showMenu(session);
            return;
        }
        if ("/help".equalsIgnoreCase(text)) {
            send(session, "help", mainKeyboard(session));
            return;
        }
        if (update.contact() != null) {
            handleContact(session, update.contact());
            return;
        }
        if (!update.photo().isEmpty()) {
            handlePhoto(session, update.photo());
            return;
        }
        if (update.location() != null) {
            handleLocation(session, update.location());
            return;
        }
        handleText(session, text);
    }

    private void handleText(TelegramCustomerSession session, String text) {
        if (text == null) {
            if (session.getState() == TelegramCustomerSessionState.AWAITING_LOCATION_ADDRESS) {
                handleLocationAddressText(session, null);
                return;
            }
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        if (handleMenuText(session, text)) {
            return;
        }
        switch (session.getState()) {
            case AWAITING_NAME, UPDATING_PROFILE_NAME -> handleName(session, text);
            case AWAITING_DESCRIPTION -> {
                session.draftDescription(text, now());
                session.state(TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP, now());
                Long activeId = session.getActiveWorkflowMessageId();
                Long messageId = screenService.sendOrEdit(
                        botClient,
                        session.getTelegramChatId(),
                        activeId,
                        messages.get(session.getLanguage(), "photo_prompt"),
                        keyboards.photos(messages, session.getLanguage()));
                session.activeWorkflowMessageId(messageId, now());
            }
            case AWAITING_LOCATION -> handleLocationStepText(session, text);
            case AWAITING_LOCATION_ADDRESS -> handleLocationAddressText(session, text);
            case AWAITING_REVIEW_COMMENT -> handleReviewComment(session, text);
            default -> send(session, "invalid_action", mainKeyboard(session));
        }
    }

    private boolean handleMenuText(TelegramCustomerSession session, String text) {
        if (!registered(session)) {
            return false;
        }
        LanguageCode language = session.getLanguage();
        if (text.equals(messages.get(language, "create_request"))) {
            startRequest(session);
            return true;
        }
        if (text.equals(messages.get(language, "my_requests"))) {
            showHistory(session, 0, null);
            return true;
        }
        if (text.equals(messages.get(language, "leave_review"))) {
            startReview(session);
            return true;
        }
        if (text.equals(messages.get(language, "profile"))) {
            showProfile(session, null);
            return true;
        }
        if (text.equals(messages.get(language, "change_language"))) {
            session.state(TelegramCustomerSessionState.LANGUAGE_SELECTION, now());
            send(session, "choose_language", keyboards.language());
            return true;
        }
        if (text.equals(messages.get(language, "help_button"))) {
            send(session, "help", mainKeyboard(session));
            return true;
        }
        if (text.equals(messages.get(language, "help"))) {
            showMenu(session);
            return true;
        }
        return false;
    }

    private void handleName(TelegramCustomerSession session, String text) {
        if (text.length() < 2 || text.length() > 160) {
            send(session, "send_name", null);
            return;
        }
        if (session.getState() == TelegramCustomerSessionState.UPDATING_PROFILE_NAME) {
            Customer customer = customerService.updateTelegramProfileName(session.getCustomerId(), text);
            session.linkCustomer(customer, now());
            session.state(TelegramCustomerSessionState.MAIN_MENU, now());
            send(session, "updated", mainKeyboard(session));
            return;
        }
        session.draftFullName(text, now());
        session.state(TelegramCustomerSessionState.AWAITING_CONTACT, now());
        send(session, "send_contact", contactKeyboard(session));
    }

    private void handleContact(TelegramCustomerSession session, TelegramUpdatePayload.TelegramContact contact) {
        if (contact.userId() == null || !contact.userId().equals(session.getTelegramUserId())) {
            send(session, "invalid_contact", null);
            return;
        }
        try {
            if (session.getState() == TelegramCustomerSessionState.UPDATING_PROFILE_PHONE) {
                Customer customer = customerService.updateTelegramPhone(
                        session.getCustomerId(),
                        session.getTelegramUserId(),
                        session.getTelegramChatId(),
                        contact.phoneNumber(),
                        session.getLanguage());
                session.linkCustomer(customer, now());
                session.state(TelegramCustomerSessionState.MAIN_MENU, now());
                send(session, "updated", keyboards.removeReplyKeyboard());
                showMenu(session);
                return;
            }
            Customer customer = customerService.linkOrCreateTelegramCustomer(
                    session.getTelegramUserId(),
                    session.getTelegramChatId(),
                    session.getDraftFullName() == null ? contactName(contact) : session.getDraftFullName(),
                    contact.phoneNumber(),
                    session.getLanguage());
            session.linkCustomer(customer, now());
            session.clearDraft(now());
            session.state(TelegramCustomerSessionState.MAIN_MENU, now());
            send(session, "updated", keyboards.removeReplyKeyboard());
            showMenu(session);
        } catch (BusinessRuleException exception) {
            if ("TELEGRAM_CUSTOMER_ARCHIVED".equals(exception.code()) || "CUSTOMER_INACTIVE".equals(exception.code())) {
                send(session, "archived_customer", null);
            } else {
                send(session, "link_conflict", null);
            }
        }
    }

    private void handlePhoto(TelegramCustomerSession session, List<TelegramUpdatePayload.TelegramPhotoSize> photos) {
        int max = properties.getMaxPendingPhotos();
        if (session.photoFileIds().size() >= max) {
            send(session,
                    "max_photos_reached",
                    session.getState() == TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP
                            ? keyboards.photos(messages, session.getLanguage())
                            : null,
                    max);
            return;
        }
        if (session.getState() != TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP) {
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        TelegramUpdatePayload.TelegramPhotoSize best = photos.stream()
                .max(Comparator.comparingLong(this::photoWeight))
                .orElse(null);
        if (best == null || best.fileId() == null || best.fileId().isBlank()) {
            send(session, "photo_invalid", keyboards.photos(messages, session.getLanguage()));
            return;
        }
        int currentCount = session.photoFileIds().size();
        if (session.photoFileIds().contains(best.fileId())) {
            send(session,
                    "photo_duplicate",
                    currentCount < max ? keyboards.photos(messages, session.getLanguage()) : null,
                    currentCount,
                    max);
            return;
        }
        try {
            TelegramFileMetadata metadata = botClient.getFile(best.fileId());
            if (metadata == null || metadata.fileSize() <= 0) {
                send(session, "photo_download_failed", keyboards.photos(messages, session.getLanguage()));
                return;
            }
        } catch (RuntimeException exception) {
            send(session, "photo_download_failed", keyboards.photos(messages, session.getLanguage()));
            return;
        }
        session.addPhotoFileId(best.fileId(), max, now());
        int acceptedCount = session.photoFileIds().size();
        if (acceptedCount < max) {
            Long activeId = session.getActiveWorkflowMessageId();
            String photoText = messages.format(session.getLanguage(), "photo_received", acceptedCount, max);
            Long messageId = screenService.sendOrEdit(
                    botClient,
                    session.getTelegramChatId(),
                    activeId,
                    photoText,
                    keyboards.photos(messages, session.getLanguage()));
            session.activeWorkflowMessageId(messageId, now());
        } else {
            Long activeId = session.getActiveWorkflowMessageId();
            String photoText = messages.format(session.getLanguage(), "photo_received", acceptedCount, max);
            screenService.sendOrEdit(
                    botClient,
                    session.getTelegramChatId(),
                    activeId,
                    photoText,
                    null);
            session.state(TelegramCustomerSessionState.AWAITING_LOCATION, now());
            Long msgId = botClient.sendMessage(
                    session.getTelegramChatId(),
                    messages.get(session.getLanguage(), "request.location.title"),
                    keyboards.location(messages, session.getLanguage()));
            session.activeWorkflowMessageId(msgId, now());
        }
    }

    private void handleLocation(TelegramCustomerSession session, TelegramUpdatePayload.TelegramLocation location) {
        if (session.getState() != TelegramCustomerSessionState.AWAITING_LOCATION
                && session.getState() != TelegramCustomerSessionState.AWAITING_LOCATION_ADDRESS) {
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        session.draftLocation(location.latitude(), location.longitude(), now());
        session.state(TelegramCustomerSessionState.CONFIRMING_REQUEST, now());
        sendConfirmation(session, null);
    }

    private void handleCallback(
            TelegramCustomerSession session,
            TelegramUpdatePayload.TelegramCallbackQuery callback) {
        String data = callback.data() == null ? "" : callback.data();
        Long callbackMessageId = callback.message() == null ? null : callback.message().messageId();
        if (data.startsWith("lang:")) {
            changeLanguage(session, data.substring("lang:".length()), callbackMessageId);
        } else if (data.equals("menu:create")) {
            startRequest(session);
        } else if (data.equals("menu:history")) {
            showHistory(session, 0, callbackMessageId);
        } else if (data.equals("menu:review")) {
            startReview(session);
        } else if (data.equals("menu:profile")) {
            showProfile(session, callbackMessageId);
        } else if (data.equals("menu:language")) {
            session.state(TelegramCustomerSessionState.LANGUAGE_SELECTION, now());
            screenService.sendOrEdit(
                    botClient,
                    session.getTelegramChatId(),
                    callbackMessageId,
                    messages.get(session.getLanguage(), "choose_language"),
                    keyboards.language());
        } else if (data.equals("menu:help")) {
            send(session, "help", mainKeyboard(session));
        } else if (data.equals("menu:back")) {
            showMenu(session);
        } else if (data.startsWith("cat:")) {
            chooseCategory(session, data, callbackMessageId);
        } else if (data.equals("photo:skip")) {
            session.state(TelegramCustomerSessionState.AWAITING_LOCATION, now());
            Long msgId = botClient.sendMessage(
                    session.getTelegramChatId(),
                    messages.get(session.getLanguage(), "request.location.title"),
                    keyboards.location(messages, session.getLanguage()));
            session.activeWorkflowMessageId(msgId, now());
        } else if (data.equals("confirm:create")) {
            confirmRequest(session, callback);
        } else if (data.equals("confirm:edit")) {
            startRequest(session);
        } else if (data.startsWith("hist:")) {
            showHistory(session, parseInt(data.substring("hist:".length()), 0), callbackMessageId);
        } else if (data.startsWith("req:")) {
            showRequestDetails(session, data, callbackMessageId);
        } else if (data.startsWith("revreq:")) {
            chooseReviewRequest(session, data, callbackMessageId);
        } else if (data.startsWith("revrate:")) {
            chooseReviewRating(session, data, callbackMessageId);
        } else if (data.equals("revcomment:skip")) {
            session.draftReviewComment(null, now());
            session.state(TelegramCustomerSessionState.CONFIRMING_REVIEW, now());
            sendReviewConfirmation(session, callbackMessageId);
        } else if (data.equals("review:submit")) {
            submitReview(session, callbackMessageId);
        } else if (data.equals("review:rating")) {
            if (session.getReviewRequestId() == null) {
                send(session, "invalid_action", mainKeyboard(session));
            } else {
                session.state(TelegramCustomerSessionState.SELECTING_REVIEW_RATING, now());
                screenService.sendOrEdit(
                        botClient,
                        session.getTelegramChatId(),
                        callbackMessageId,
                        messages.get(session.getLanguage(), "select_rating"),
                        keyboards.reviewRating(messages, session.getLanguage()));
            }
        } else if (data.equals("review:comment")) {
            if (session.getReviewRequestId() == null || session.getDraftReviewRating() == null) {
                send(session, "invalid_action", mainKeyboard(session));
            } else {
                session.state(TelegramCustomerSessionState.AWAITING_REVIEW_COMMENT, now());
                screenService.sendOrEdit(
                        botClient,
                        session.getTelegramChatId(),
                        callbackMessageId,
                        messages.get(session.getLanguage(), "optional_comment"),
                        keyboards.reviewComment(messages, session.getLanguage()));
            }
        } else if (data.equals("review:cancel")) {
            session.clearReviewDraft(now());
            session.state(TelegramCustomerSessionState.MAIN_MENU, now());
            screenService.sendOrEdit(
                    botClient,
                    session.getTelegramChatId(),
                    callbackMessageId,
                    messages.get(session.getLanguage(), "cancelled"),
                    null);
            send(session, "main_menu", mainKeyboard(session));
        } else if (data.equals("profile:name")) {
            session.state(TelegramCustomerSessionState.UPDATING_PROFILE_NAME, now());
            screenService.sendOrEdit(
                    botClient,
                    session.getTelegramChatId(),
                    callbackMessageId,
                    messages.get(session.getLanguage(), "send_new_name"),
                    null);
        } else if (data.equals("profile:phone")) {
            session.state(TelegramCustomerSessionState.UPDATING_PROFILE_PHONE, now());
            send(session, "send_new_phone", contactKeyboard(session));
        } else {
            send(session, "invalid_action", mainKeyboard(session));
        }
        if (callback.id() != null) {
            botClient.answerCallback(callback.id(), "");
        }
    }

    private void changeLanguage(TelegramCustomerSession session, String languageCode, Long callbackMessageId) {
        LanguageCode language = parseLanguage(languageCode);
        session.language(language, now());
        if (session.getCustomerId() != null) {
            Customer customer = customerService.updateTelegramLanguage(session.getCustomerId(), language);
            session.linkCustomer(customer, now());
            session.state(TelegramCustomerSessionState.MAIN_MENU, now());
            showMenu(session);
            return;
        }
        session.state(TelegramCustomerSessionState.AWAITING_NAME, now());
        screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                callbackMessageId,
                messages.get(language, "send_name"),
                null);
    }

    private void startRequest(TelegramCustomerSession session) {
        if (!registered(session)) {
            send(session, "send_contact", contactKeyboard(session));
            return;
        }
        session.clearDraft(now());
        session.state(TelegramCustomerSessionState.SELECTING_CATEGORY, now());
        List<RepairCategory> categories = categoryRepository.findByActiveTrueOrderByIdAsc();
        Long messageId = botClient.sendMessage(
                session.getTelegramChatId(),
                messages.get(session.getLanguage(), "choose_category"),
                keyboards.categories(categories, session.getLanguage()));
        session.activeWorkflowMessageId(messageId, now());
    }

    private void chooseCategory(TelegramCustomerSession session, String data, Long callbackMessageId) {
        Long categoryId = parseLong(data.substring("cat:".length()));
        RepairCategory category = categoryRepository.findById(categoryId)
                .filter(RepairCategory::isActive)
                .orElseThrow(() -> new BusinessRuleException("INVALID_CATEGORY", "Invalid category.", 400));
        session.draftCategory(category.getId(), now());
        session.state(TelegramCustomerSessionState.AWAITING_DESCRIPTION, now());
        Long activeId = callbackMessageId != null ? callbackMessageId : session.getActiveWorkflowMessageId();
        Long messageId = screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                activeId,
                messages.get(session.getLanguage(), "send_description"),
                null);
        session.activeWorkflowMessageId(messageId, now());
    }

    private void confirmRequest(
            TelegramCustomerSession session,
            TelegramUpdatePayload.TelegramCallbackQuery callback) {
        if (session.getState() != TelegramCustomerSessionState.CONFIRMING_REQUEST) {
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        if (session.getCreatedRequest() != null) {
            sendCreated(session, false, callback.message() == null ? null : callback.message().messageId());
            return;
        }
        String sourceReference = confirmationSourceReference(session, callback);
        RepairRequest request = repairRequestService.telegramCreate(
                session.getCustomerId(),
                session.getDraftCategoryId(),
                session.getDraftDescription(),
                session.getDraftAddress(),
                session.getDraftLatitude(),
                session.getDraftLongitude(),
                sourceReference);
        session.createdRequest(request, now());
        boolean photoFailed = photoService.attachProblemPhotos(
                request.getId(),
                session.getCustomerId(),
                session.photoFileIds());
        Long cardMessageId = callback.message() == null ? null : callback.message().messageId();
        session.clearDraft(now());
        session.state(TelegramCustomerSessionState.MAIN_MENU, now());
        sendCreated(session, photoFailed, cardMessageId);
    }

    private String confirmationSourceReference(
            TelegramCustomerSession session,
            TelegramUpdatePayload.TelegramCallbackQuery callback) {
        Long messageId = callback.message() == null ? null : callback.message().messageId();
        if (messageId != null) {
            return "telegram-confirm-%d-%d".formatted(session.getTelegramChatId(), messageId);
        }
        return "telegram-confirm-%s".formatted(callback.id());
    }

    private void showHistory(TelegramCustomerSession session, int page, Long messageIdToEdit) {
        if (!registered(session)) {
            send(session, "send_contact", contactKeyboard(session));
            return;
        }
        int safePage = Math.max(page, 0);
        var response = repairRequestService.customerHistory(
                session.getCustomerId(),
                new RepairRequestQuery(null, null, null, null, null, null, null, null, null, null, null),
                PageRequest.of(safePage, HISTORY_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        if (response.content().isEmpty()) {
            if (safePage > 0) {
                showHistory(session, 0, messageIdToEdit);
                return;
            }
            screenService.sendOrEdit(
                    botClient,
                    session.getTelegramChatId(),
                    messageIdToEdit,
                    messages.get(session.getLanguage(), "my_requests_empty"),
                    keyboards.emptyHistory(messages, session.getLanguage()));
            return;
        }
        session.historyPage(safePage, now());
        screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                messageIdToEdit,
                messages.get(session.getLanguage(), "my_requests_prompt"),
                keyboards.history(response.content(), safePage, !response.last(), messages, session.getLanguage(), listDateFormatter));
    }

    private void showRequestDetails(TelegramCustomerSession session, String data, Long messageIdToEdit) {
        String payload = data.substring("req:".length());
        String[] parts = payload.split(":");
        Long requestId = parseLong(parts[0]);
        int page = parts.length > 1 ? parseInt(parts[1], 0) : 0;
        if (!registered(session) || requestRepository.findByIdAndCustomerId(requestId, session.getCustomerId()).isEmpty()) {
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        RepairRequestDetailResponse details = repairRequestService.get(requestId);
        CustomerReviewSummary review = reviewService.customerReview(session.getCustomerId(), requestId);
        boolean canReview = details.status() == RepairRequestStatus.COMPLETED && review == null
                && reviewService.canReview(session.getCustomerId(), requestId);
        String text = detailsText(details, session.getLanguage());
        if (review != null) {
            text += "\n\n" + messages.format(
                    session.getLanguage(),
                    "your_review",
                    review.rating(),
                    review.comment() == null ? messages.get(session.getLanguage(), "no_comment") : review.comment());
        }
        BigDecimal lat = extractLatitude(details);
        BigDecimal lon = extractLongitude(details);
        screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                messageIdToEdit,
                text,
                keyboards.requestDetails(requestId, page, canReview, lat, lon, messages, session.getLanguage()));
    }

    private BigDecimal extractLatitude(RepairRequestDetailResponse details) {
        if (details.location() != null && details.location().latitude() != null) {
            return details.location().latitude();
        }
        return details.latitude();
    }

    private BigDecimal extractLongitude(RepairRequestDetailResponse details) {
        if (details.location() != null && details.location().longitude() != null) {
            return details.location().longitude();
        }
        return details.longitude();
    }

    private void showProfile(TelegramCustomerSession session, Long messageIdToEdit) {
        if (!registered(session)) {
            send(session, "send_contact", contactKeyboard(session));
            return;
        }
        Customer customer = session.getCustomer();
        screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                messageIdToEdit,
                messages.format(
                        session.getLanguage(),
                        "profile_name",
                        customer.getFullName(),
                        customer.getPhone(),
                        customer.getPreferredLanguage()),
                keyboards.profile(messages, session.getLanguage()));
    }

    private void sendConfirmation(TelegramCustomerSession session, Long messageIdToEdit) {
        RepairCategory category = categoryRepository.findById(session.getDraftCategoryId())
                .orElseThrow(() -> new BusinessRuleException("INVALID_CATEGORY", "Invalid category.", 400));
        String location;
        if (session.getDraftAddress() != null && !session.getDraftAddress().isBlank()) {
            location = session.getDraftAddress();
        } else if (session.getDraftLatitude() != null && session.getDraftLongitude() != null) {
            location = messages.get(session.getLanguage(), "location_attached");
        } else {
            location = messages.get(session.getLanguage(), "request.location.not_provided");
        }
        LanguageCode language = session.getLanguage();
        String text = messages.get(session.getLanguage(), "confirm_prompt")
                + "\n" + field(language, "field.category", keyboards.label(category, language))
                + "\n" + field(language, "field.description", session.getDraftDescription())
                + "\n" + field(language, "field.location", location)
                + "\n" + field(language, "field.photos", String.valueOf(session.photoFileIds().size()))
                + "\n" + field(language, "field.language", String.valueOf(language));
        Long targetMessageId = messageIdToEdit != null ? messageIdToEdit : session.getActiveWorkflowMessageId();
        Long resId = screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                targetMessageId,
                text,
                keyboards.confirm(messages, session.getLanguage()));
        session.activeWorkflowMessageId(resId, now());
    }

    private void handleLocationStepText(TelegramCustomerSession session, String text) {
        if (isEnterAddressText(text)) {
            session.state(TelegramCustomerSessionState.AWAITING_LOCATION_ADDRESS, now());
            Long msgId = botClient.sendMessage(
                    session.getTelegramChatId(),
                    messages.get(session.getLanguage(), "request.location.address_prompt"),
                    keyboards.removeReplyKeyboard());
            session.activeWorkflowMessageId(msgId, now());
            return;
        }
        if (isSkipLocationText(text)) {
            session.draftLocation(null, null, now());
            session.draftAddress(null, now());
            session.state(TelegramCustomerSessionState.CONFIRMING_REQUEST, now());
            sendConfirmation(session, null);
            return;
        }
        Long msgId = botClient.sendMessage(
                session.getTelegramChatId(),
                messages.get(session.getLanguage(), "request.location.invalid"),
                keyboards.location(messages, session.getLanguage()));
        session.activeWorkflowMessageId(msgId, now());
    }

    private void handleLocationAddressText(TelegramCustomerSession session, String text) {
        String trimmed = text == null ? null : text.trim();
        if (trimmed == null || trimmed.isEmpty() || trimmed.length() > 500) {
            botClient.sendMessage(session.getTelegramChatId(), messages.get(session.getLanguage(), "invalid_request_data"), null);
            return;
        }
        session.draftAddress(trimmed, now());
        session.state(TelegramCustomerSessionState.CONFIRMING_REQUEST, now());
        sendConfirmation(session, null);
    }

    private boolean isEnterAddressText(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        for (LanguageCode lang : LanguageCode.values()) {
            if (trimmed.equals(messages.get(lang, "request.location.enter_address"))) {
                return true;
            }
        }
        return trimmed.equalsIgnoreCase("Enter address")
                || trimmed.equalsIgnoreCase("Ввести адрес")
                || trimmed.equalsIgnoreCase("Manzil kiritish")
                || trimmed.contains("Enter address")
                || trimmed.contains("Ввести адрес")
                || trimmed.contains("Manzil kiritish")
                || trimmed.equalsIgnoreCase("⌨️ Enter address")
                || trimmed.equalsIgnoreCase("⌨️ Ввести адрес")
                || trimmed.equalsIgnoreCase("⌨️ Manzil kiritish");
    }

    private boolean isSkipLocationText(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        for (LanguageCode lang : LanguageCode.values()) {
            if (trimmed.equals(messages.get(lang, "request.location.skip"))
                    || trimmed.equals(messages.get(lang, "skip"))) {
                return true;
            }
        }
        return trimmed.equalsIgnoreCase("/skip")
                || trimmed.equalsIgnoreCase("Skip")
                || trimmed.equalsIgnoreCase("Пропустить")
                || trimmed.equalsIgnoreCase("O'tkazib yuborish")
                || trimmed.equalsIgnoreCase("⏭ Skip")
                || trimmed.equalsIgnoreCase("⏭ Пропустить")
                || trimmed.equalsIgnoreCase("⏭ O'tkazib yuborish");
    }

    private void showMenu(TelegramCustomerSession session) {
        if (session.getCustomerId() == null) {
            session.state(TelegramCustomerSessionState.LANGUAGE_SELECTION, now());
            send(session, "choose_language", keyboards.language());
            return;
        }
        session.clearDraft(now());
        session.state(TelegramCustomerSessionState.MAIN_MENU, now());
        send(session, "main_menu", mainKeyboard(session));
    }

    private void startReview(TelegramCustomerSession session) {
        if (!activeRegistered(session)) {
            send(session, "send_contact", contactKeyboard(session));
            return;
        }
        session.clearReviewDraft(now());
        List<EligibleReviewRequest> requests = reviewService.eligibleRequests(
                session.getCustomerId(),
                PageRequest.of(0, HISTORY_PAGE_SIZE));
        if (requests.isEmpty()) {
            send(session, "no_eligible_reviews", mainKeyboard(session));
            return;
        }
        session.state(TelegramCustomerSessionState.SELECTING_REVIEW_REQUEST, now());
        Long messageId = botClient.sendMessage(
                session.getTelegramChatId(),
                messages.get(session.getLanguage(), "eligible_reviews"),
                keyboards.eligibleReviewRequests(requests, session.getLanguage()));
        session.activeWorkflowMessageId(messageId, now());
    }

    private void chooseReviewRequest(TelegramCustomerSession session, String data, Long callbackMessageId) {
        if (!activeRegistered(session)) {
            send(session, "send_contact", contactKeyboard(session));
            return;
        }
        Long requestId = parseLong(data.substring("revreq:".length()));
        if (!reviewService.canReview(session.getCustomerId(), requestId)) {
            send(session, "review_access_denied", mainKeyboard(session));
            return;
        }
        RepairRequest request = requestRepository.findWithRelationsById(requestId)
                .orElseThrow(() -> new BusinessRuleException("INVALID_CALLBACK", "Invalid callback.", 400));
        session.reviewRequest(request, now());
        session.draftReviewRating(null, now());
        session.draftReviewComment(null, now());
        session.state(TelegramCustomerSessionState.SELECTING_REVIEW_RATING, now());
        screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                callbackMessageId,
                messages.get(session.getLanguage(), "select_rating"),
                keyboards.reviewRating(messages, session.getLanguage()));
    }

    private void chooseReviewRating(TelegramCustomerSession session, String data, Long callbackMessageId) {
        if (session.getReviewRequestId() == null) {
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        int rating = parseInt(data.substring("revrate:".length()), 0);
        if (rating < 1 || rating > 5) {
            screenService.sendOrEdit(
                    botClient,
                    session.getTelegramChatId(),
                    callbackMessageId,
                    messages.get(session.getLanguage(), "invalid_rating"),
                    keyboards.reviewRating(messages, session.getLanguage()));
            return;
        }
        session.draftReviewRating(rating, now());
        session.state(TelegramCustomerSessionState.AWAITING_REVIEW_COMMENT, now());
        Long messageId = screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                callbackMessageId,
                messages.get(session.getLanguage(), "optional_comment"),
                keyboards.reviewComment(messages, session.getLanguage()));
        session.activeWorkflowMessageId(messageId, now());
    }

    private void handleReviewComment(TelegramCustomerSession session, String text) {
        String comment = trim(text);
        if (comment != null && comment.length() > RepairReview.MAX_COMMENT_LENGTH) {
            send(session, "comment_too_long", keyboards.reviewComment(messages, session.getLanguage()));
            return;
        }
        session.draftReviewComment(comment, now());
        session.state(TelegramCustomerSessionState.CONFIRMING_REVIEW, now());
        sendReviewConfirmation(session, session.getActiveWorkflowMessageId());
    }

    private void sendReviewConfirmation(TelegramCustomerSession session, Long messageIdToEdit) {
        Long requestId = session.getReviewRequestId();
        Integer rating = session.getDraftReviewRating();
        if (requestId == null || rating == null) {
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        RepairRequest request = requestRepository.findWithRelationsById(requestId)
                .orElseThrow(() -> new BusinessRuleException("INVALID_CALLBACK", "Invalid callback.", 400));
        String comment = session.getDraftReviewComment() == null
                ? messages.get(session.getLanguage(), "no_comment")
                : session.getDraftReviewComment();
        Long resId = screenService.sendOrEdit(
                botClient,
                session.getTelegramChatId(),
                messageIdToEdit,
                messages.format(
                        session.getLanguage(),
                        "review_confirmation",
                        keyboards.label(request.getCategory(), session.getLanguage()),
                        rating,
                        comment),
                keyboards.reviewConfirm(messages, session.getLanguage()));
        session.activeWorkflowMessageId(resId, now());
    }

    private void submitReview(TelegramCustomerSession session, Long messageIdToEdit) {
        if (session.getState() != TelegramCustomerSessionState.CONFIRMING_REVIEW
                || session.getReviewRequestId() == null
                || session.getDraftReviewRating() == null) {
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        try {
            reviewService.submitFromTelegram(
                    session.getTelegramUserId(),
                    session.getTelegramChatId(),
                    session.getReviewRequestId(),
                    session.getDraftReviewRating(),
                    session.getDraftReviewComment(),
                    session.getLanguage());
            session.clearReviewDraft(now());
            session.state(TelegramCustomerSessionState.MAIN_MENU, now());
            screenService.sendOrEdit(
                    botClient,
                    session.getTelegramChatId(),
                    messageIdToEdit,
                    messages.get(session.getLanguage(), "thank_you_review"),
                    null);
            send(session, "main_menu", mainKeyboard(session));
        } catch (BusinessRuleException exception) {
            if ("REVIEW_ALREADY_EXISTS".equals(exception.code())) {
                session.clearReviewDraft(now());
                session.state(TelegramCustomerSessionState.MAIN_MENU, now());
                screenService.sendOrEdit(
                        botClient,
                        session.getTelegramChatId(),
                        messageIdToEdit,
                        messages.get(session.getLanguage(), "already_reviewed"),
                        null);
                send(session, "main_menu", mainKeyboard(session));
                return;
            }
            throw exception;
        }
    }

    private void sendCreated(TelegramCustomerSession session, boolean photoFailed, Long cardMessageId) {
        if (cardMessageId != null) {
            screenService.sendOrEdit(
                    botClient,
                    session.getTelegramChatId(),
                    cardMessageId,
                    messages.get(session.getLanguage(), "request_created"),
                    null);
        }
        send(session, "main_menu", mainKeyboard(session));
        if (photoFailed) {
            send(session, "photo_failed", mainKeyboard(session));
        }
    }

    private String detailsText(RepairRequestDetailResponse details, LanguageCode language) {
        StringBuilder builder = new StringBuilder();
        String category = details.category() == null ? "" : categorySummaryLabel(details.category(), language);
        if (!category.isBlank()) {
            builder.append("🔧 ").append(category).append("\n\n");
        }
        String statusText = messages.statusIcon(details.status()) + " " + messages.requestStatus(details.status(), language);
        builder.append(statusText).append("\n\n");

        builder.append(messages.get(language, "detail.problem")).append("\n");
        builder.append(details.description() == null ? "" : details.description()).append("\n\n");

        String locationDisplay = extractLocationDisplay(details, language);
        if (locationDisplay != null && !locationDisplay.isBlank()) {
            builder.append(messages.get(language, "detail.location")).append("\n");
            builder.append(locationDisplay).append("\n\n");
        }

        builder.append(messages.get(language, "detail.created")).append("\n");
        builder.append(formatDetailsDate(details.createdAt()));

        return builder.toString();
    }

    private String extractLocationDisplay(RepairRequestDetailResponse details, LanguageCode language) {
        if (details.location() != null) {
            if (details.location().address() != null && !details.location().address().isBlank()) {
                return details.location().address();
            }
            if (details.location().latitude() != null && details.location().longitude() != null) {
                return messages.get(language, "location_attached");
            }
        }
        if (details.address() != null && !details.address().isBlank()) {
            return details.address();
        }
        if (details.latitude() != null && details.longitude() != null) {
            return messages.get(language, "location_attached");
        }
        return null;
    }

    private String field(LanguageCode language, String key, String value) {
        return messages.get(language, key) + ": " + value;
    }

    private String formatDetailsDate(OffsetDateTime value) {
        if (value == null) {
            return "";
        }
        return detailsDateFormatter.format(Instant.from(value));
    }

    private String formatTelegramDate(OffsetDateTime value) {
        if (value == null) {
            return "";
        }
        return telegramDateFormatter.format(Instant.from(value));
    }

    private String categorySummaryLabel(RepairRequestCategorySummary category, LanguageCode language) {
        if (category == null) {
            return "";
        }
        String name = switch (language) {
            case EN -> category.nameEn();
            case RU -> category.nameRu();
            case UZ -> category.nameUz();
        };
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (category.name() != null && !category.name().isBlank()) {
            return category.name();
        }
        if (category.nameUz() != null && !category.nameUz().isBlank()) {
            return category.nameUz();
        }
        if (category.nameRu() != null && !category.nameRu().isBlank()) {
            return category.nameRu();
        }
        if (category.nameEn() != null && !category.nameEn().isBlank()) {
            return category.nameEn();
        }
        return "";
    }

    private boolean registered(TelegramCustomerSession session) {
        return session.getCustomerId() != null;
    }

    private boolean activeRegistered(TelegramCustomerSession session) {
        return session.getCustomer() != null && session.getCustomer().isActive();
    }

    private void send(TelegramCustomerSession session, String key, String keyboard, Object... args) {
        String text = args.length == 0
                ? messages.get(session.getLanguage(), key)
                : messages.format(session.getLanguage(), key, args);
        botClient.sendMessage(session.getTelegramChatId(), text, keyboard);
    }

    private String mainKeyboard(TelegramCustomerSession session) {
        return mainKeyboard(session.getLanguage());
    }

    private String mainKeyboard(LanguageCode language) {
        return keyboards.main(messages, language);
    }

    private String contactKeyboard(TelegramCustomerSession session) {
        return keyboards.contact(messages, session.getLanguage());
    }

    private String contactName(TelegramUpdatePayload.TelegramContact contact) {
        String name = (contact.firstName() == null ? "" : contact.firstName())
                + " "
                + (contact.lastName() == null ? "" : contact.lastName());
        String trimmed = name.trim();
        return trimmed.isBlank() ? "Telegram Customer" : trimmed;
    }

    private long photoWeight(TelegramUpdatePayload.TelegramPhotoSize photo) {
        if (photo.fileSize() != null) {
            return photo.fileSize();
        }
        long width = photo.width() == null ? 0 : photo.width();
        long height = photo.height() == null ? 0 : photo.height();
        return width * height;
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

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
