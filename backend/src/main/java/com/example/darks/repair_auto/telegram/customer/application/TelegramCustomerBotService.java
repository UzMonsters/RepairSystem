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
import com.example.darks.repair_auto.telegram.core.infrastructure.TelegramProperties;
import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSession;
import com.example.darks.repair_auto.telegram.customer.domain.TelegramCustomerSessionState;
import com.example.darks.repair_auto.telegram.customer.infrastructure.TelegramCustomerSessionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramCustomerBotService {

    private static final int HISTORY_PAGE_SIZE = 5;

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
            TelegramBotClient botClient,
            TelegramMessages messages,
            TelegramKeyboards keyboards,
            TelegramProperties properties) {
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
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        switch (session.getState()) {
            case AWAITING_NAME, UPDATING_PROFILE_NAME -> handleName(session, text);
            case AWAITING_DESCRIPTION -> {
                session.draftDescription(text, now());
                session.state(TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP, now());
                send(session, "photo_prompt", keyboards.photos(messages, session.getLanguage()));
            }
            case AWAITING_LOCATION -> {
                session.draftAddress(text, now());
                session.state(TelegramCustomerSessionState.CONFIRMING_REQUEST, now());
                sendConfirmation(session);
            }
            case AWAITING_REVIEW_COMMENT -> handleReviewComment(session, text);
            default -> send(session, "invalid_action", mainKeyboard(session));
        }
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
        send(session, "send_contact", null);
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
                send(session, "updated", mainKeyboard(session));
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
            showMenu(session);
        } catch (BusinessRuleException exception) {
            if ("TELEGRAM_CUSTOMER_ARCHIVED".equals(exception.code())) {
                send(session, "archived_customer", null);
            } else {
                send(session, "link_conflict", null);
            }
        }
    }

    private void handlePhoto(TelegramCustomerSession session, List<TelegramUpdatePayload.TelegramPhotoSize> photos) {
        if (session.getState() != TelegramCustomerSessionState.AWAITING_PHOTO_OR_SKIP) {
            send(session, "invalid_action", mainKeyboard(session));
            return;
        }
        photos.stream()
                .max(Comparator.comparingLong(this::photoWeight))
                .map(TelegramUpdatePayload.TelegramPhotoSize::fileId)
                .ifPresent(fileId -> session.addPhotoFileId(fileId, properties.getMaxPendingPhotos(), now()));
        send(session, "photo_prompt", keyboards.photos(messages, session.getLanguage()));
    }

    private void handleLocation(TelegramCustomerSession session, TelegramUpdatePayload.TelegramLocation location) {
        if (session.getState() != TelegramCustomerSessionState.AWAITING_LOCATION) {
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
        String data = callback.data() == null ? "" : callback.data();
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
            send(session, "location_prompt", null);
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
            send(session, "send_new_phone", null);
        } else {
            send(session, "invalid_action", mainKeyboard(session));
        }
        if (callback.id() != null) {
            botClient.answerCallback(callback.id(), "");
        }
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
            send(session, "send_contact", null);
            return;
        }
        session.clearDraft(now());
        session.state(TelegramCustomerSessionState.SELECTING_CATEGORY, now());
        List<RepairCategory> categories = categoryRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc();
        botClient.sendMessage(
                session.getTelegramChatId(),
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
            sendCreated(session, session.getCreatedRequest().getRequestNumber(), false);
            return;
        }
        RepairRequest request = repairRequestService.telegramCreate(
                session.getCustomerId(),
                session.getDraftCategoryId(),
                session.getDraftDescription(),
                session.getDraftAddress(),
                session.getDraftLatitude(),
                session.getDraftLongitude(),
                "telegram-confirm-" + callback.id());
        session.createdRequest(request, now());
        boolean photoFailed = photoService.attachProblemPhotos(
                request.getId(),
                session.getCustomerId(),
                session.photoFileIds());
        session.clearDraft(now());
        session.state(TelegramCustomerSessionState.MAIN_MENU, now());
        sendCreated(session, request.getRequestNumber(), photoFailed);
    }

    private void showHistory(TelegramCustomerSession session, int page) {
        if (!registered(session)) {
            send(session, "send_contact", null);
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
        botClient.sendMessage(
                session.getTelegramChatId(),
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
        botClient.sendMessage(
                session.getTelegramChatId(),
                text,
                keyboards.requestDetails(requestId, canReview, messages, session.getLanguage()));
    }

    private void showProfile(TelegramCustomerSession session) {
        if (!registered(session)) {
            send(session, "send_contact", null);
            return;
        }
        Customer customer = session.getCustomer();
        botClient.sendMessage(
                session.getTelegramChatId(),
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
        String location = session.getDraftAddress() != null
                ? session.getDraftAddress()
                : session.getDraftLatitude() + ", " + session.getDraftLongitude();
        String text = messages.get(session.getLanguage(), "confirm_prompt")
                + "\nCategory: " + escape(keyboards.label(category, session.getLanguage()))
                + "\nDescription: " + escape(session.getDraftDescription())
                + "\nLocation: " + escape(location)
                + "\nPhotos: " + session.photoFileIds().size()
                + "\nLanguage: " + session.getLanguage();
        botClient.sendMessage(session.getTelegramChatId(), text, keyboards.confirm(messages, session.getLanguage()));
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
            send(session, "send_contact", null);
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
        botClient.sendMessage(
                session.getTelegramChatId(),
                messages.get(session.getLanguage(), "eligible_reviews"),
                keyboards.eligibleReviewRequests(requests, session.getLanguage()));
    }

    private void chooseReviewRequest(TelegramCustomerSession session, String data) {
        if (!activeRegistered(session)) {
            send(session, "send_contact", null);
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
        botClient.sendMessage(
                session.getTelegramChatId(),
                messages.format(
                        session.getLanguage(),
                        "review_confirmation",
                        escape(request.getRequestNumber()),
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

    private void sendCreated(TelegramCustomerSession session, String requestNumber, boolean photoFailed) {
        send(session, "request_created", mainKeyboard(session), requestNumber);
        if (photoFailed) {
            send(session, "photo_failed", mainKeyboard(session));
        }
    }

    private String historyText(List<RepairRequestSummaryResponse> requests, LanguageCode language) {
        StringBuilder builder = new StringBuilder(messages.get(language, "my_requests"));
        for (RepairRequestSummaryResponse request : requests) {
            builder.append("\n")
                    .append(escape(request.requestNumber()))
                    .append(" | ")
                    .append(escape(categoryLabel(request, language)))
                    .append(" | ")
                    .append(escape(messages.requestStatus(request.status(), language)))
                    .append(" | ")
                    .append(request.createdAt());
        }
        return builder.toString();
    }

    private String detailsText(RepairRequestDetailResponse details, LanguageCode language) {
        return escape(details.requestNumber())
                + "\nCategory: " + escape(categoryLabel(details, language))
                + "\nDescription: " + escape(details.description())
                + "\nStatus: " + escape(messages.requestStatus(details.status(), language))
                + "\nPriority: " + details.priority()
                + "\nLocation: " + escape(details.address() == null
                        ? details.latitude() + ", " + details.longitude()
                        : details.address())
                + "\nCreated: " + details.createdAt();
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
        botClient.sendMessage(session.getTelegramChatId(), text, keyboard);
    }

    private String mainKeyboard(TelegramCustomerSession session) {
        return mainKeyboard(session.getLanguage());
    }

    private String mainKeyboard(LanguageCode language) {
        return keyboards.main(messages, language);
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
