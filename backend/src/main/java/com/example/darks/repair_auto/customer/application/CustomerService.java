package com.example.darks.repair_auto.customer.application;

import com.example.darks.repair_auto.customer.api.dto.CustomerCreateRequest;
import com.example.darks.repair_auto.customer.api.dto.CustomerDetailResponse;
import com.example.darks.repair_auto.customer.api.dto.CustomerMapper;
import com.example.darks.repair_auto.customer.api.dto.CustomerSummaryResponse;
import com.example.darks.repair_auto.customer.api.dto.CustomerUpdateRequest;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.domain.CustomerRegistrationSource;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.ActorAccessLifecycleService;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentDownload;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentValidator;
import com.example.darks.repair_auto.repair.attachment.application.DetectedFile;
import com.example.darks.repair_auto.repair.attachment.application.ImageAttachmentUtils;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageUpload;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObjectDownload;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.error.ResourceNotFoundException;
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.example.darks.repair_auto.shared.phone.PhoneNumberNormalizer;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CustomerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final RepairAttachmentRepository attachmentRepository;
    private final ObjectStorageService objectStorageService;
    private final AttachmentValidator validator;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final EmailNormalizer emailNormalizer;
    private final ActorAccessLifecycleService actorAccessLifecycleService;
    private final Clock clock;

    @Autowired
    public CustomerService(
            CustomerRepository customerRepository,
            RepairAttachmentRepository attachmentRepository,
            ObjectStorageService objectStorageService,
            AttachmentValidator validator,
            PhoneNumberNormalizer phoneNumberNormalizer,
            EmailNormalizer emailNormalizer,
            ActorAccessLifecycleService actorAccessLifecycleService,
            Clock clock) {
        this.customerRepository = customerRepository;
        this.attachmentRepository = attachmentRepository;
        this.objectStorageService = objectStorageService;
        this.validator = validator != null ? validator : new AttachmentValidator();
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.emailNormalizer = emailNormalizer;
        this.actorAccessLifecycleService = actorAccessLifecycleService;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public CustomerService(
            CustomerRepository customerRepository,
            PhoneNumberNormalizer phoneNumberNormalizer,
            EmailNormalizer emailNormalizer,
            ActorAccessLifecycleService actorAccessLifecycleService) {
        this(customerRepository, null, null, new AttachmentValidator(), phoneNumberNormalizer, emailNormalizer, actorAccessLifecycleService, Clock.systemUTC());
    }

    public CustomerService(CustomerRepository customerRepository, PhoneNumberNormalizer phoneNumberNormalizer) {
        this(customerRepository, phoneNumberNormalizer, new EmailNormalizer(), null);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerSummaryResponse> list(
            String search,
            String phone,
            LanguageCode language,
            Boolean active,
            CustomerRegistrationSource registrationSource,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo,
            Pageable pageable) {
        String normalizedPhone = phone == null || phone.isBlank() ? null : phoneNumberNormalizer.normalize(phone);
        String normalizedSearchPhone = normalizeSearchPhone(search);
        return PageResponse.from(customerRepository.findAll(filters(
                blankToNull(search),
                normalizedSearchPhone,
                normalizedPhone,
                language,
                active,
                registrationSource,
                createdFrom,
                createdTo), pageable).map(CustomerMapper::summary));
    }

    @Transactional(readOnly = true)
    public CustomerDetailResponse get(Long id) {
        return CustomerMapper.details(find(id));
    }

    @Transactional
    public CustomerDetailResponse create(CustomerCreateRequest request) {
        Customer customer = new Customer(
                request.fullName().trim(),
                normalizePhoneOrNull(request.phone()),
                request.preferredLanguage(),
                now());
        customer.setEmail(normalizeEmailOrNull(request.email()), null, now());
        try {
            Customer saved = customerRepository.saveAndFlush(customer);
            LOGGER.info("Customer event operation=customer_created result=success customerId={}", saved.getId());
            return CustomerMapper.details(saved);
        } catch (DataIntegrityViolationException exception) {
            throw customerConflict(exception);
        }
    }

    @Transactional
    public CustomerDetailResponse update(Long id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
        customer.updateProfile(
                request.fullName().trim(),
                normalizePhoneOrNull(request.phone()),
                request.preferredLanguage(),
                now());
        customer.setEmail(normalizeEmailOrNull(request.email()), null, now());
        try {
            return CustomerMapper.details(customerRepository.saveAndFlush(customer));
        } catch (DataIntegrityViolationException exception) {
            throw customerConflict(exception);
        }
    }

    @Transactional
    public CustomerDetailResponse changeActivation(Long id, boolean active, String reason) {
        Customer customer = customerRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
        boolean deactivated = customer.isActive() && !active;
        customer.setActive(active, now());
        if (deactivated && actorAccessLifecycleService != null) {
            actorAccessLifecycleService.onCustomerDeactivated(id);
        }
        LOGGER.info(
                "Customer event operation=customer_activation_changed result=success customerId={} active={} reason={}",
                id,
                active,
                reason == null ? "" : reason.trim());
        return CustomerMapper.details(customer);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public Customer linkOrCreateTelegramCustomer(
            Long telegramUserId,
            Long telegramChatId,
            String fullName,
            String phone,
            LanguageCode language) {
        String normalizedPhone = phoneNumberNormalizer.normalize(phone);
        OffsetDateTime now = now();
        customerRepository.findByTelegramUserIdForUpdate(telegramUserId)
                .ifPresent(existing -> {
                    if (!normalizedPhone.equals(existing.getPhone())) {
                        throw telegramLinkConflict();
                    }
                    if (!existing.isActive()) {
                        throw archivedCustomer();
                    }
                    existing.updateTelegramChat(telegramChatId, language, now);
                });
        var linked = customerRepository.findByTelegramUserId(telegramUserId);
        if (linked.isPresent()) {
            return linked.get();
        }
        Customer byPhone = customerRepository.findByPhoneForUpdate(normalizedPhone).orElse(null);
        if (byPhone != null) {
            if (!byPhone.isActive()) {
                throw archivedCustomer();
            }
            if (byPhone.getTelegramUserId() != null && !byPhone.getTelegramUserId().equals(telegramUserId)) {
                throw telegramLinkConflict();
            }
            byPhone.linkTelegram(telegramUserId, telegramChatId, language, now);
            return byPhone;
        }
        try {
            return customerRepository.saveAndFlush(Customer.telegram(
                    fullName.trim(),
                    normalizedPhone,
                    telegramUserId,
                    telegramChatId,
                    language,
                    now));
        } catch (DataIntegrityViolationException exception) {
            throw telegramLinkConflict();
        }
    }

    @Transactional
    public Customer updateTelegramProfileName(Long customerId, String fullName) {
        Customer customer = customerRepository.findByIdForUpdate(customerId).orElseThrow(this::notFound);
        customer.updateProfile(fullName.trim(), customer.getPhone(), customer.getPreferredLanguage(), now());
        return customer;
    }

    @Transactional
    public Customer updateTelegramLanguage(Long customerId, LanguageCode language) {
        Customer customer = customerRepository.findByIdForUpdate(customerId).orElseThrow(this::notFound);
        customer.updateProfile(customer.getFullName(), customer.getPhone(), language, now());
        return customer;
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public Customer updateTelegramPhone(
            Long customerId,
            Long telegramUserId,
            Long telegramChatId,
            String phone,
            LanguageCode language) {
        String normalizedPhone = phoneNumberNormalizer.normalize(phone);
        OffsetDateTime now = now();
        Customer customer = customerRepository.findByIdForUpdate(customerId).orElseThrow(this::notFound);
        if (!customer.isActive()
                || customer.getTelegramUserId() == null
                || !customer.getTelegramUserId().equals(telegramUserId)) {
            throw telegramLinkConflict();
        }
        Customer phoneOwner = customerRepository.findByPhoneForUpdate(normalizedPhone).orElse(null);
        if (phoneOwner != null && !phoneOwner.getId().equals(customerId)) {
            throw telegramLinkConflict();
        }
        customer.updateProfile(customer.getFullName(), normalizedPhone, language, now);
        customer.updateTelegramChat(telegramChatId, language, now);
        return customer;
    }

    @Transactional
    public AvatarResponse uploadAvatar(Long customerId, MultipartFile file) {
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(this::notFound);
        if (!customer.isActive()) {
            throw new BusinessRuleException(ErrorCode.ACCOUNT_INACTIVE);
        }

        String originalFileName = validator.validateOriginalFileName(file);
        OffsetDateTime now = now();

        byte[] bytes;
        DetectedFile detected;
        String checksum;
        try (BufferedInputStream input = new BufferedInputStream(file.getInputStream())) {
            input.mark(64);
            detected = validator.detectAndValidate(AttachmentType.AVATAR, input);
            input.reset();
            bytes = input.readAllBytes();
            checksum = sha256Hex(bytes);
        } catch (IOException e) {
            throw new BusinessRuleException("ATTACHMENT_STORAGE_FAILED", "Avatar upload failed.", 503);
        }

        String storageKey = "avatars/customers/%d/%s".formatted(customerId, UUID.randomUUID());

        try {
            objectStorageService.upload(new StorageUpload(
                    storageKey,
                    detected.contentType(),
                    bytes.length,
                    new ByteArrayInputStream(bytes)
            ));
        } catch (RuntimeException e) {
            LOGGER.warn("Customer avatar object storage upload failed customerId={} storageKey={}", customerId, storageKey, e);
            throw new BusinessRuleException("ATTACHMENT_STORAGE_FAILED", "Avatar storage operation failed.", 503);
        }

        RepairAttachment newAttachment = RepairAttachment.customerUpload(
                null,
                AttachmentType.AVATAR,
                storageKey,
                originalFileName,
                customer,
                now
        );
        newAttachment.markAvailable(detected.contentType(), bytes.length, checksum, now);
        RepairAttachment savedAttachment = attachmentRepository.saveAndFlush(newAttachment);

        RepairAttachment previousAvatar = customer.getAvatarAttachment();
        customer.setAvatarAttachment(savedAttachment, now);
        customerRepository.save(customer);

        if (previousAvatar != null && previousAvatar.getStatus() == AttachmentStatus.AVAILABLE) {
            previousAvatar.markDeleted("AVATAR_REPLACED", now);
            attachmentRepository.save(previousAvatar);
        }

        LOGGER.info("Customer event operation=avatar_uploaded result=success customerId={} attachmentId={}", customerId, savedAttachment.getId());
        return ImageAttachmentUtils.toAvatarResponse(savedAttachment, ImageAttachmentUtils.customerAvatarDownloadUrl(customerId));
    }

    @Transactional(readOnly = true)
    public AttachmentDownload downloadAvatar(Long customerId) {
        Customer customer = find(customerId);
        RepairAttachment avatar = customer.getAvatarAttachment();
        if (avatar == null || !avatar.isAvailable()) {
            throw new ResourceNotFoundException("Avatar was not found.");
        }
        if (objectStorageService == null) {
            throw new ResourceNotFoundException("Avatar was not found.");
        }
        StoredObjectDownload object = objectStorageService.download(avatar.getStorageKey());
        String contentType = object.contentType() == null || object.contentType().isBlank()
                ? "application/octet-stream"
                : object.contentType();
        return new AttachmentDownload(
                safeDownloadFileName(avatar),
                contentType,
                object.sizeBytes(),
                object.inputStream());
    }

    @Transactional
    public void deleteAvatar(Long customerId) {
        Customer customer = customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(this::notFound);
        OffsetDateTime now = now();

        RepairAttachment avatar = customer.getAvatarAttachment();
        if (avatar != null) {
            customer.setAvatarAttachment(null, now);
            customerRepository.save(customer);
            if (avatar.getStatus() == AttachmentStatus.AVAILABLE) {
                avatar.markDeleted("AVATAR_REMOVED", now);
                attachmentRepository.save(avatar);
            }
            LOGGER.info("Customer event operation=avatar_deleted result=success customerId={} attachmentId={}", customerId, avatar.getId());
        }
    }

    private String safeDownloadFileName(RepairAttachment attachment) {
        String extension = validator.extensionFor(attachment.getContentType());
        String fallback = "avatar-" + attachment.getId() + (extension == null ? "" : extension);
        String original = attachment.getOriginalFileName();
        if (original == null || original.isBlank() || original.indexOf('"') >= 0 || original.indexOf('\\') >= 0) {
            return fallback;
        }
        return original;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    private Customer find(Long id) {
        return customerRepository.findById(id).orElseThrow(this::notFound);
    }

    private BusinessRuleException notFound() {
        return new BusinessRuleException(ErrorCode.CUSTOMER_NOT_FOUND);
    }

    private BusinessRuleException customerConflict(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause() != null ? exception.getMostSpecificCause().getMessage() : "";
        if (message != null && message.contains("telegram_user_id")) {
            return new BusinessRuleException(ErrorCode.CUSTOMER_TELEGRAM_ID_ALREADY_EXISTS);
        }
        return new BusinessRuleException(ErrorCode.CUSTOMER_PHONE_ALREADY_EXISTS);
    }

    private BusinessRuleException telegramLinkConflict() {
        return new BusinessRuleException(ErrorCode.CUSTOMER_TELEGRAM_ID_ALREADY_EXISTS);
    }

    private BusinessRuleException archivedCustomer() {
        return new BusinessRuleException("TELEGRAM_CUSTOMER_ARCHIVED", "Archived customer profile cannot be linked.", 409);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizePhoneOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return phoneNumberNormalizer.normalize(value);
    }

    private String normalizeEmailOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return emailNormalizer.normalize(value);
    }

    private String normalizeSearchPhone(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        try {
            return phoneNumberNormalizer.normalize(search);
        } catch (BusinessRuleException exception) {
            return null;
        }
    }

    private Specification<Customer> filters(
            String search,
            String normalizedSearchPhone,
            String phone,
            LanguageCode language,
            Boolean active,
            CustomerRegistrationSource registrationSource,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("fullName")), pattern),
                        builder.like(root.get("phone"), "%" + search + "%"),
                        normalizedSearchPhone == null
                                ? builder.disjunction()
                                : builder.equal(root.get("phone"), normalizedSearchPhone)));
            }
            if (phone != null) {
                predicate = builder.and(predicate, builder.equal(root.get("phone"), phone));
            }
            if (language != null) {
                predicate = builder.and(predicate, builder.equal(root.get("preferredLanguage"), language));
            }
            if (active != null) {
                predicate = builder.and(predicate, builder.equal(root.get("active"), active));
            }
            if (registrationSource != null) {
                predicate = builder.and(predicate, builder.equal(root.get("registrationSource"), registrationSource));
            }
            if (createdFrom != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }
            return predicate;
        };
    }
}
