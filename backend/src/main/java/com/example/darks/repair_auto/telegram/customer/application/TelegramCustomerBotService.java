package com.example.darks.repair_auto.telegram.customer.application;

import com.example.darks.repair_auto.catalog.category.domain.RepairCategory;
import com.example.darks.repair_auto.catalog.category.infrastructure.RepairCategoryRepository;
import com.example.darks.repair_auto.customer.application.CustomerService;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestDetailResponse;
import com.example.darks.repair_auto.repair.request.api.dto.RepairRequestSummaryResponse;
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
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramProperties;
import com.example.darks.repair_auto.telegram.conversation.TelegramChatCleanupService;
import com.example.darks.repair_auto.telegram.conversation.TelegramConversationPolicy;
import com.example.darks.repair_auto.telegram.conversation.TelegramInputClassifier;
import com.example.darks.repair_auto.telegram.conversation.TelegramUnexpectedInputHandler;
import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSession;
import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSessionState;
import com.example.darks.repair_auto.telegram.customer.infrastructure.TelegramCustomerSessionRepository;
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
    private final TelegramMessages messages;
    private final TelegramKeyboards keyboards;
    private final TelegramProperties properties;
    private final TelegramUnexpectedInputHandler unexpectedInputHandler;
    private final TelegramChatCleanupService cleanupService;
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
            TelegramMessages messages,
            TelegramKeyboards keyboards,
            TelegramProperties properties,
            TelegramUnexpectedInputHandler unexpectedInputHandler,
            TelegramChatCleanupService cleanupService,
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
                messages,
                keyboards,
                properties,
                unexpectedInputHandler,
                cleanupService,
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
                messages,
                keyboards,
                properties,
                new TelegramUnexpectedInputHandler(
                        new TelegramInputClassifier(),
                        new TelegramConversationPolicy(),
                        new TelegramChatCleanupService()),
                new TelegramChatCleanupService(),
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
            TelegramMessages messages,
            TelegramKeyboards keyboards,
            TelegramProperties properties,
            TelegramUnexpectedInputHandler unexpectedInputHandler,
            TelegramChatCleanupService cleanupService,
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
        this.messages = messages;
        this.keyboards = keyboards;
        this.properties = properties;
        this.unexpectedInputHandler = unexpectedInputHandler;
        this.cleanupService = cleanupService;
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
        if (isUnknownCommand(update)) {
            cleanupIncoming(session, update);
            return;
        }
        if (!isGlobalCommand(update.text()) && unexpectedInputHandler.cleanupIfUnexpected(session, update, botClient)) {
            return;
        }
        if (isUnexpectedCustomerText(session, update)) {
            cleanupIncoming(session, update);
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
            cleanupSessionMessages(session);
            session.clearDraft(now());
            session.state(TelegramCustomerSessionState.LANGUAGE_SELECTION, now());
            send(session, "choose_language", keyboards.language());
            return;
        }
        if ("/cancel".equalsIgnoreCase(text)) {
            cleanupSessionMessages(session);
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
                send(session, "photo_prompt", keyboards.photos(messages, session.getLanguage()));
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
            showHistory(session, 0);
            return true;
        }
        if (text.equals(messages.get(language, "leave_review"))) {
            startReview(session);
            return true;
        }
        if (text.equals(messages.get(language, "profile"))) {
            showProfile(session);
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
            send(session,
                    "photo_received",
                    keyboards.photos(messages, session.getLanguage()),
                    acceptedCount,
                    max);
        } else {
            send(session,
                    "photo_received_max",
                    null,
                    acceptedCount,
                    max);
            session.state(TelegramCustomerSessionState.AWAITING_LOCATION, now());
            sendRaw(
                    session,
                    messages.get(session.getLanguage(), "request.location.title"),
                    keyboards.location(messages, session.getLanguage()));
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
        sendConfirmation(session);
    }

    private void handleCallback(
            TelegramCustomerSession session,
            TelegramUpdatePayload.TelegramCallbackQuery callback) {
        cleanupService.answerCallbackQuietly(botClient, callback.id(), "customer");
        String data = callback.data() == null ? "" : callback.data();
        if (!isCallbackActionAllowed(session, data)) {
            if (isMalformedCallbackData(data)) {
                throw new BusinessRuleException("INVALID_CALLBACK", "Invalid callback.", 400);
            }
            send(session, "invalid_action", mainKeyboard(session));
            cleanupCallbackKeyboard(session, callback);
            return;
        }
        if (data.startsWith("lang:")) {
            changeLanguage(session, data.substring("lang:".length()));
        } else if (data.equals("menu:create")) {
            startRequest(session);
        } else if (data.equals("menu:history")) {
            showHistory(session, 0);
        } else if (data.equals("menu:review")) {
            startReview(session);
        } else if (data.equals("menu:profile")) {
            showProfile(session);
        } else if (data.equals("menu:language")) {
            session.state(TelegramCustomerSessionState.LANGUAGE_SELECTION, now());
            send(session, "choose_language", keyboards.language());
        } else if (data.equals("menu:help")) {
            send(session, "help", mainKeyboard(session));
        } else if (data.equals("menu:back")) {
            showMenu(session);
        } else if (data.startsWith("cat:")) {
            chooseCategory(session, data);
        } else if (data.equals("photo:skip")) {
            session.state(TelegramCustomerSessionState.AWAITING_LOCATION, now());
            sendRaw(
                    session,
                    messages.get(session.getLanguage(), "request.location.title"),
                    keyboards.location(messages, session.getLanguage()));
        } else if (data.equals("confirm:create")) {
            confirmRequest(session, callback);
        } else if (data.equals("confirm:edit")) {
            startRequest(session);
        } else if (data.startsWith("hist:")) {
            showHistory(session, parseInt(data.substring("hist:".length()), 0));
        } else if (data.startsWith("req:")) {
            showRequestDetails(session, data);
        } else if (data.startsWith("revreq:")) {
            chooseReviewRequest(session, data);
        } else if (data.startsWith("revrate:")) {
            chooseReviewRating(session, data);
        } else if (data.equals("revcomment:skip")) {
            session.draftReviewComment(null, now());
            session.state(TelegramCustomerSessionState.CONFIRMING_REVIEW, now());
            sendReviewConfirmation(session);
        } else if (data.equals("review:submit")) {
            submitReview(session);
        } else if (data.equals("review:rating")) {
            if (session.getReviewRequestId() == null) {
                send(session, "invalid_action", mainKeyboard(session));
            } else {
                session.state(TelegramCustomerSessionState.SELECTING_REVIEW_RATING, now());
                send(session, "select_rating", keyboards.reviewRating(messages, session.getLanguage()));
            }
        } else if (data.equals("review:comment")) {
            if (session.getReviewRequestId() == null || session.getDraftReviewRating() == null) {
                send(session, "invalid_action", mainKeyboard(session));
            } else {
                session.state(TelegramCustomerSessionState.AWAITING_REVIEW_COMMENT, now());
                send(session, "optional_comment", keyboards.reviewComment(messages, session.getLanguage()));
            }
        } else if (data.equals("review:cancel")) {
            session.clearReviewDraft(now());
            session.state(TelegramCustomerSessionState.MAIN_MENU, now());
            send(session, "cancelled", mainKeyboard(session));
        } else if (data.equals("profile:name")) {
            session.state(TelegramCustomerSessionState.UPDATING_PROFILE_NAME, now());
            send(session, "send_new_name", null);
        } else if (data.equals("profile:phone")) {
            session.state(TelegramCustomerSessionState.UPDATING_PROFILE_PHONE, now());
            send(session, "send_new_phone", contactKeyboard(session));
        } else {
            send(session, "invalid_action", mainKeyboard(session));
        }
        cleanupCallbackKeyboard(session, callback);
    }

    private void changeLanguage(TelegramCustomerSession session, String languageCode) {
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
        send(session, "send_name", null);
    }

    private void startRequest(TelegramCustomerSession session) {
        if (!registered(session)) {
            send(session, "send_contact", contactKeyboard(session));
            return;
        }
        session.clearDraft(now());
        session.state(TelegramCustomerSessionState.SELECTING_CATEGORY, now());
        List<RepairCategory> categories = categoryRepository.findByActiveTrueOrderByIdAsc();
        sendRaw(
                session,
                messages.get(session.getLanguage(), "choose_category"),
                keyboards.categories(categories, session.getLanguage()));
    }

    private void chooseCategory(TelegramCustomerSession session, String data) {
        if (session.getState() != TelegramCustomerSessionState.SELECTING_CATEGORY) {
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        Long categoryId = parseLong(data.substring("cat:".length()));
        RepairCategory category = categoryRepository.findById(categoryId)
                .filter(RepairCategory::isActive)
                .orElseThrow(() -> new BusinessRuleException("INVALID_CATEGORY", "Invalid category.", 400));
        session.draftCategory(category.getId(), now());
        session.state(TelegramCustomerSessionState.AWAITING_DESCRIPTION, now());
        send(session, "send_description", null);
    }

    private void confirmRequest(
            TelegramCustomerSession session,
            TelegramUpdatePayload.TelegramCallbackQuery callback) {
        if (session.getState() != TelegramCustomerSessionState.CONFIRMING_REQUEST) {
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        if (session.getCreatedRequest() != null) {
            sendCreated(session, false);
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
        cleanupSessionMessages(session);
        session.clearDraft(now());
        session.state(TelegramCustomerSessionState.MAIN_MENU, now());
        sendCreated(session, photoFailed);
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

    private void showHistory(TelegramCustomerSession session, int page) {
        if (!registered(session)) {
            send(session, "send_contact", contactKeyboard(session));
            return;
        }
        var response = repairRequestService.customerHistory(
                session.getCustomerId(),
                new RepairRequestQuery(null, null, null, null, null, null, null, null, null, null, null),
                PageRequest.of(Math.max(page, 0), HISTORY_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        if (response.content().isEmpty()) {
            send(session, "empty_history", mainKeyboard(session));
            return;
        }
        session.historyPage(page, now());
        RepairRequestSummaryResponse first = response.content().get(0);
        sendRaw(
                session,
                historyText(response.content(), session.getLanguage()),
                keyboards.history(first.id(), page, !response.last(), messages, session.getLanguage()));
    }

    private void showRequestDetails(TelegramCustomerSession session, String data) {
        Long requestId = parseLong(data.substring("req:".length()));
        RepairRequestDetailResponse details = repairRequestService.get(requestId);
        if (!details.customer().id().equals(session.getCustomerId())) {
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        CustomerReviewSummary review = reviewService.customerReview(session.getCustomerId(), requestId);
        boolean canReview = details.status() == RepairRequestStatus.COMPLETED && review == null
                && reviewService.canReview(session.getCustomerId(), requestId);
        String text = detailsText(details, session.getLanguage());
        if (review != null) {
            text += "\n" + messages.format(
                    session.getLanguage(),
                    "your_review",
                    review.rating(),
                    escape(review.comment() == null ? messages.get(session.getLanguage(), "no_comment") : review.comment()));
        }
        sendRaw(
                session,
                text,
                keyboards.requestDetails(requestId, canReview, messages, session.getLanguage()));
    }

    private void showProfile(TelegramCustomerSession session) {
        if (!registered(session)) {
            send(session, "send_contact", contactKeyboard(session));
            return;
        }
        Customer customer = session.getCustomer();
        sendRaw(
                session,
                messages.format(
                        session.getLanguage(),
                        "profile_name",
                        escape(customer.getFullName()),
                        escape(customer.getPhone()),
                        customer.getPreferredLanguage()),
                keyboards.profile(messages, session.getLanguage()));
    }

    private void sendConfirmation(TelegramCustomerSession session) {
        RepairCategory category = categoryRepository.findById(session.getDraftCategoryId())
                .orElseThrow(() -> new BusinessRuleException("INVALID_CATEGORY", "Invalid category.", 400));
        String location;
        if (session.getDraftAddress() != null && !session.getDraftAddress().isBlank()) {
            location = session.getDraftAddress();
        } else if (session.getDraftLatitude() != null && session.getDraftLongitude() != null) {
            location = "📍 " + session.getDraftLatitude() + ", " + session.getDraftLongitude();
        } else {
            location = messages.get(session.getLanguage(), "request.location.not_provided");
        }
        LanguageCode language = session.getLanguage();
        String text = messages.get(session.getLanguage(), "confirm_prompt")
                + "\n" + field(language, "field.category", escape(keyboards.label(category, language)))
                + "\n" + field(language, "field.description", escape(session.getDraftDescription()))
                + "\n" + field(language, "field.location", escape(location))
                + "\n" + field(language, "field.photos", String.valueOf(session.photoFileIds().size()))
                + "\n" + field(language, "field.language", String.valueOf(language));
        sendRaw(session, text, keyboards.confirm(messages, session.getLanguage()));
    }

    private void handleLocationStepText(TelegramCustomerSession session, String text) {
        if (isEnterAddressText(text)) {
            session.state(TelegramCustomerSessionState.AWAITING_LOCATION_ADDRESS, now());
            sendRaw(
                    session,
                    messages.get(session.getLanguage(), "request.location.address_prompt"),
                    keyboards.removeReplyKeyboard());
            return;
        }
        if (isSkipLocationText(text)) {
            session.draftLocation(null, null, now());
            session.draftAddress(null, now());
            session.state(TelegramCustomerSessionState.CONFIRMING_REQUEST, now());
            sendConfirmation(session);
            return;
        }
        sendRaw(
                session,
                messages.get(session.getLanguage(), "request.location.invalid"),
                keyboards.location(messages, session.getLanguage()));
    }

    private void handleLocationAddressText(TelegramCustomerSession session, String text) {
        String trimmed = text == null ? null : text.trim();
        if (trimmed == null || trimmed.isEmpty() || trimmed.length() > 500) {
            sendRaw(session, messages.get(session.getLanguage(), "invalid_request_data"), null);
            return;
        }
        session.draftAddress(trimmed, now());
        session.state(TelegramCustomerSessionState.CONFIRMING_REQUEST, now());
        sendConfirmation(session);
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
        sendRaw(
                session,
                messages.get(session.getLanguage(), "eligible_reviews"),
                keyboards.eligibleReviewRequests(requests, session.getLanguage()));
    }

    private void chooseReviewRequest(TelegramCustomerSession session, String data) {
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
        send(session, "select_rating", keyboards.reviewRating(messages, session.getLanguage()));
    }

    private void chooseReviewRating(TelegramCustomerSession session, String data) {
        if (session.getReviewRequestId() == null) {
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        int rating = parseInt(data.substring("revrate:".length()), 0);
        if (rating < 1 || rating > 5) {
            send(session, "invalid_rating", keyboards.reviewRating(messages, session.getLanguage()));
            return;
        }
        session.draftReviewRating(rating, now());
        session.state(TelegramCustomerSessionState.AWAITING_REVIEW_COMMENT, now());
        send(session, "optional_comment", keyboards.reviewComment(messages, session.getLanguage()));
    }

    private void handleReviewComment(TelegramCustomerSession session, String text) {
        String comment = trim(text);
        if (comment != null && comment.length() > RepairReview.MAX_COMMENT_LENGTH) {
            send(session, "comment_too_long", keyboards.reviewComment(messages, session.getLanguage()));
            return;
        }
        session.draftReviewComment(comment, now());
        session.state(TelegramCustomerSessionState.CONFIRMING_REVIEW, now());
        sendReviewConfirmation(session);
    }

    private void sendReviewConfirmation(TelegramCustomerSession session) {
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
        sendRaw(
                session,
                messages.format(
                        session.getLanguage(),
                        "review_confirmation",
                        escape(keyboards.label(request.getCategory(), session.getLanguage())),
                        rating,
                        escape(comment)),
                keyboards.reviewConfirm(messages, session.getLanguage()));
    }

    private void submitReview(TelegramCustomerSession session) {
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
            send(session, "thank_you_review", mainKeyboard(session));
        } catch (BusinessRuleException exception) {
            if ("REVIEW_ALREADY_EXISTS".equals(exception.code())) {
                session.clearReviewDraft(now());
                session.state(TelegramCustomerSessionState.MAIN_MENU, now());
                send(session, "already_reviewed", mainKeyboard(session));
                return;
            }
            throw exception;
        }
    }

    private void sendCreated(TelegramCustomerSession session, boolean photoFailed) {
        send(session, "request_created", mainKeyboard(session));
        if (photoFailed) {
            send(session, "photo_failed", mainKeyboard(session));
        }
    }

    private String historyText(List<RepairRequestSummaryResponse> requests, LanguageCode language) {
        StringBuilder builder = new StringBuilder(messages.get(language, "my_requests"));
        for (RepairRequestSummaryResponse request : requests) {
            builder.append("\n")
                    .append(escape(categoryLabel(request, language)))
                    .append(" | ")
                    .append(escape(messages.requestStatus(request.status(), language)))
                    .append(" | ")
                    .append(formatTelegramDate(request.createdAt()));
        }
        return builder.toString();
    }

    private String detailsText(RepairRequestDetailResponse details, LanguageCode language) {
        String locationDisplay;
        if (details.location() != null) {
            if (details.location().address() != null && !details.location().address().isBlank()) {
                locationDisplay = details.location().address();
            } else if (details.location().latitude() != null && details.location().longitude() != null) {
                locationDisplay = "📍 " + details.location().latitude() + ", " + details.location().longitude();
            } else {
                locationDisplay = messages.get(language, "request.location.not_provided");
            }
        } else if (details.address() != null && !details.address().isBlank()) {
            locationDisplay = details.address();
        } else if (details.latitude() != null && details.longitude() != null) {
            locationDisplay = "📍 " + details.latitude() + ", " + details.longitude();
        } else {
            locationDisplay = messages.get(language, "request.location.not_provided");
        }
        return field(language, "field.category", escape(categoryLabel(details, language)))
                + "\n" + field(language, "field.description", escape(details.description()))
                + "\n" + field(language, "field.status", escape(messages.requestStatus(details.status(), language)))
                + "\n" + field(language, "field.priority", escape(messages.requestPriority(details.priority(), language)))
                + "\n" + field(language, "field.location", escape(locationDisplay))
                + "\n" + field(language, "field.created", formatTelegramDate(details.createdAt()));
    }

    private String field(LanguageCode language, String key, String value) {
        return messages.get(language, key) + ": " + value;
    }

    private String formatTelegramDate(OffsetDateTime value) {
        if (value == null) {
            return "";
        }
        return telegramDateFormatter.format(Instant.from(value));
    }

    private String categoryLabel(RepairRequestSummaryResponse request, LanguageCode language) {
        return switch (language) {
            case EN -> request.category().nameEn();
            case RU -> request.category().nameRu();
            case UZ -> request.category().nameUz();
        };
    }

    private String categoryLabel(RepairRequestDetailResponse request, LanguageCode language) {
        return switch (language) {
            case EN -> request.category().nameEn();
            case RU -> request.category().nameRu();
            case UZ -> request.category().nameUz();
        };
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
        Long messageId = botClient.sendMessage(session.getTelegramChatId(), text, keyboard);
        trackBotMessage(session, messageId);
    }

    private Long sendRaw(TelegramCustomerSession session, String text, String keyboard) {
        Long messageId = botClient.sendMessage(session.getTelegramChatId(), text, keyboard);
        trackBotMessage(session, messageId);
        return messageId;
    }

    private void cleanupIncoming(TelegramCustomerSession session, TelegramUpdatePayload update) {
        Long messageId = update.message() == null ? null : update.message().messageId();
        cleanupService.deleteQuietly(botClient, session.getTelegramChatId(), messageId, "customer");
    }

    private void cleanupSessionMessages(TelegramCustomerSession session) {
        cleanupService.removeKeyboardQuietly(
                botClient,
                session.getTelegramChatId(),
                session.getActivePromptMessageId(),
                "customer");
        cleanupService.deleteAllQuietly(
                botClient,
                session.getTelegramChatId(),
                session.transientMessageIds(),
                "customer");
    }

    private void cleanupCallbackKeyboard(
            TelegramCustomerSession session,
            TelegramUpdatePayload.TelegramCallbackQuery callback) {
        cleanupService.removeKeyboardQuietly(
                botClient,
                session.getTelegramChatId(),
                callback.message() == null ? null : callback.message().messageId(),
                "customer");
    }

    private void trackBotMessage(TelegramCustomerSession session, Long messageId) {
        if (messageId == null) {
            return;
        }
        session.activePromptMessageId(messageId, now());
        session.trackTransientMessageId(messageId, 30, now());
    }

    private boolean isUnknownCommand(TelegramUpdatePayload update) {
        String text = trim(update.text());
        return text != null && text.startsWith("/") && !isGlobalCommand(text);
    }

    private boolean isGlobalCommand(String text) {
        String command = trim(text);
        return "/start".equalsIgnoreCase(command)
                || "/cancel".equalsIgnoreCase(command)
                || "/menu".equalsIgnoreCase(command)
                || "/customer".equalsIgnoreCase(command)
                || "/help".equalsIgnoreCase(command);
    }

    private boolean isUnexpectedCustomerText(TelegramCustomerSession session, TelegramUpdatePayload update) {
        String text = trim(update.text());
        if (text == null || text.startsWith("/")) {
            return false;
        }
        return switch (session.getState()) {
            case MAIN_MENU -> registered(session) && !isCustomerMenuText(session, text);
            case AWAITING_LOCATION -> !isEnterAddressText(text) && !isSkipLocationText(text);
            default -> false;
        };
    }

    private boolean isCustomerMenuText(TelegramCustomerSession session, String text) {
        LanguageCode language = session.getLanguage();
        return text.equals(messages.get(language, "create_request"))
                || text.equals(messages.get(language, "my_requests"))
                || text.equals(messages.get(language, "leave_review"))
                || text.equals(messages.get(language, "profile"))
                || text.equals(messages.get(language, "change_language"))
                || text.equals(messages.get(language, "help_button"))
                || text.equals(messages.get(language, "help"));
    }

    private boolean isCallbackActionAllowed(TelegramCustomerSession session, String data) {
        TelegramCustomerSessionState state = session.getState();
        if (data.startsWith("lang:")) {
            return state == TelegramCustomerSessionState.LANGUAGE_SELECTION;
        }
        if (data.equals("menu:create")
                || data.equals("menu:history")
                || data.equals("menu:review")
                || data.equals("menu:profile")
                || data.equals("menu:language")
                || data.equals("menu:help")) {
            return state == TelegramCustomerSessionState.MAIN_MENU;
        }
        if (data.equals("menu:back")) {
            return true;
        }
        if (data.startsWith("cat:")) {
            return state == TelegramCustomerSessionState.SELECTING_CATEGORY;
        }
        if (data.equals("photo:skip")) {
            return state == TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP;
        }
        if (data.equals("confirm:create") || data.equals("confirm:edit")) {
            return state == TelegramCustomerSessionState.CONFIRMING_REQUEST;
        }
        if (data.startsWith("hist:") || data.startsWith("req:")) {
            return state == TelegramCustomerSessionState.MAIN_MENU;
        }
        if (data.startsWith("revreq:")) {
            return state == TelegramCustomerSessionState.SELECTING_REVIEW_REQUEST
                    || state == TelegramCustomerSessionState.MAIN_MENU;
        }
        if (data.startsWith("revrate:")) {
            return state == TelegramCustomerSessionState.SELECTING_REVIEW_RATING;
        }
        if (data.equals("revcomment:skip")) {
            return state == TelegramCustomerSessionState.AWAITING_REVIEW_COMMENT;
        }
        if (data.equals("review:submit")
                || data.equals("review:rating")
                || data.equals("review:comment")
                || data.equals("review:cancel")) {
            return state == TelegramCustomerSessionState.CONFIRMING_REVIEW;
        }
        if (data.equals("profile:name") || data.equals("profile:phone")) {
            return state == TelegramCustomerSessionState.MAIN_MENU;
        }
        return false;
    }

    private boolean isMalformedCallbackData(String data) {
        return hasInvalidLong(data, "cat:")
                || hasInvalidLong(data, "hist:")
                || hasInvalidLong(data, "req:")
                || hasInvalidLong(data, "revreq:")
                || hasInvalidInt(data, "revrate:");
    }

    private boolean hasInvalidLong(String data, String prefix) {
        if (!data.startsWith(prefix)) {
            return false;
        }
        try {
            Long.valueOf(data.substring(prefix.length()));
            return false;
        } catch (RuntimeException exception) {
            return true;
        }
    }

    private boolean hasInvalidInt(String data, String prefix) {
        if (!data.startsWith(prefix)) {
            return false;
        }
        try {
            Integer.parseInt(data.substring(prefix.length()));
            return false;
        } catch (RuntimeException exception) {
            return true;
        }
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

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
