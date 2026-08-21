package com.example.darks.repair_auto.telegram.customer.domain;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.repair.request.domain.RepairRequest;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "telegram_customer_sessions")
public class TelegramCustomerSession {

    private static final String PHOTO_DELIMITER = "\n";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_user_id", nullable = false, unique = true)
    private Long telegramUserId;

    @Column(name = "telegram_chat_id", nullable = false, unique = true)
    private Long telegramChatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private LanguageCode language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TelegramCustomerSessionState state;

    @Column(name = "draft_full_name", length = 160)
    private String draftFullName;

    @Column(name = "draft_category_id")
    private Long draftCategoryId;

    @Column(name = "draft_description", length = 2000)
    private String draftDescription;

    @Column(name = "draft_address", length = 500)
    private String draftAddress;

    @Column(name = "draft_latitude", precision = 9, scale = 6)
    private BigDecimal draftLatitude;

    @Column(name = "draft_longitude", precision = 10, scale = 6)
    private BigDecimal draftLongitude;

    @Column(name = "draft_photo_file_ids", columnDefinition = "text")
    private String draftPhotoFileIds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_request_id")
    private RepairRequest createdRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_request_id")
    private RepairRequest reviewRequest;

    @Column(name = "draft_review_rating")
    private Integer draftReviewRating;

    @Column(name = "draft_review_comment", length = 1000)
    private String draftReviewComment;

    @Column(name = "history_page", nullable = false)
    private int historyPage;

    @Column(name = "last_interaction_at", nullable = false)
    private OffsetDateTime lastInteractionAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected TelegramCustomerSession() {
    }

    public TelegramCustomerSession(Long telegramUserId, Long telegramChatId, OffsetDateTime now) {
        this.telegramUserId = telegramUserId;
        this.telegramChatId = telegramChatId;
        this.language = LanguageCode.UZ;
        this.state = TelegramCustomerSessionState.LANGUAGE_SELECTION;
        this.historyPage = 0;
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

    public Customer getCustomer() {
        return customer;
    }

    public Long getCustomerId() {
        return customer == null ? null : customer.getId();
    }

    public LanguageCode getLanguage() {
        return language;
    }

    public TelegramCustomerSessionState getState() {
        return state;
    }

    public String getDraftFullName() {
        return draftFullName;
    }

    public Long getDraftCategoryId() {
        return draftCategoryId;
    }

    public String getDraftDescription() {
        return draftDescription;
    }

    public String getDraftAddress() {
        return draftAddress;
    }

    public BigDecimal getDraftLatitude() {
        return draftLatitude;
    }

    public BigDecimal getDraftLongitude() {
        return draftLongitude;
    }

    public RepairRequest getCreatedRequest() {
        return createdRequest;
    }

    public RepairRequest getReviewRequest() {
        return reviewRequest;
    }

    public Long getReviewRequestId() {
        return reviewRequest == null ? null : reviewRequest.getId();
    }

    public Integer getDraftReviewRating() {
        return draftReviewRating;
    }

    public String getDraftReviewComment() {
        return draftReviewComment;
    }

    public int getHistoryPage() {
        return historyPage;
    }

    public void touch(Long chatId, OffsetDateTime now) {
        this.telegramChatId = chatId;
        this.lastInteractionAt = now;
        this.updatedAt = now;
    }

    public void language(LanguageCode language, OffsetDateTime now) {
        this.language = language;
        this.updatedAt = now;
    }

    public void state(TelegramCustomerSessionState state, OffsetDateTime now) {
        this.state = state;
        this.updatedAt = now;
    }

    public void draftFullName(String fullName, OffsetDateTime now) {
        this.draftFullName = fullName;
        this.updatedAt = now;
    }

    public void linkCustomer(Customer customer, OffsetDateTime now) {
        this.customer = customer;
        this.updatedAt = now;
    }

    public void draftCategory(Long categoryId, OffsetDateTime now) {
        this.draftCategoryId = categoryId;
        this.updatedAt = now;
    }

    public void draftDescription(String description, OffsetDateTime now) {
        this.draftDescription = description;
        this.updatedAt = now;
    }

    public void draftAddress(String address, OffsetDateTime now) {
        this.draftAddress = address;
        this.draftLatitude = null;
        this.draftLongitude = null;
        this.updatedAt = now;
    }

    public void draftLocation(BigDecimal latitude, BigDecimal longitude, OffsetDateTime now) {
        this.draftAddress = null;
        this.draftLatitude = latitude;
        this.draftLongitude = longitude;
        this.updatedAt = now;
    }

    public void createdRequest(RepairRequest request, OffsetDateTime now) {
        this.createdRequest = request;
        this.updatedAt = now;
    }

    public void reviewRequest(RepairRequest request, OffsetDateTime now) {
        this.reviewRequest = request;
        this.updatedAt = now;
    }

    public void draftReviewRating(Integer rating, OffsetDateTime now) {
        this.draftReviewRating = rating;
        this.updatedAt = now;
    }

    public void draftReviewComment(String comment, OffsetDateTime now) {
        this.draftReviewComment = comment;
        this.updatedAt = now;
    }

    public void historyPage(int page, OffsetDateTime now) {
        this.historyPage = Math.max(page, 0);
        this.updatedAt = now;
    }

    public List<String> photoFileIds() {
        if (draftPhotoFileIds == null || draftPhotoFileIds.isBlank()) {
            return List.of();
        }
        return List.of(draftPhotoFileIds.split(PHOTO_DELIMITER));
    }

    public void addPhotoFileId(String fileId, int maxPhotos, OffsetDateTime now) {
        List<String> current = new ArrayList<>(photoFileIds());
        if (!current.contains(fileId) && current.size() < maxPhotos) {
            current.add(fileId);
            this.draftPhotoFileIds = String.join(PHOTO_DELIMITER, current);
            this.updatedAt = now;
        }
    }

    public void clearDraft(OffsetDateTime now) {
        this.draftFullName = null;
        this.draftCategoryId = null;
        this.draftDescription = null;
        this.draftAddress = null;
        this.draftLatitude = null;
        this.draftLongitude = null;
        this.draftPhotoFileIds = null;
        this.createdRequest = null;
        this.reviewRequest = null;
        this.draftReviewRating = null;
        this.draftReviewComment = null;
        this.historyPage = 0;
        this.updatedAt = now;
    }

    public void clearReviewDraft(OffsetDateTime now) {
        this.reviewRequest = null;
        this.draftReviewRating = null;
        this.draftReviewComment = null;
        this.updatedAt = now;
    }
}
