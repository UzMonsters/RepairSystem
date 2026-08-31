package com.example.darks.repair_auto.technician.application;

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
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.error.ResourceNotFoundException;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import com.example.darks.repair_auto.shared.phone.PhoneNumberNormalizer;
import com.example.darks.repair_auto.technician.api.dto.TechnicianCreateRequest;
import com.example.darks.repair_auto.technician.api.dto.TechnicianDetailResponse;
import com.example.darks.repair_auto.technician.api.dto.TechnicianMapper;
import com.example.darks.repair_auto.technician.api.dto.TechnicianSummaryResponse;
import com.example.darks.repair_auto.technician.api.dto.TechnicianUpdateRequest;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
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
public class TechnicianService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TechnicianService.class);

    private final TechnicianRepository technicianRepository;
    private final RepairAttachmentRepository attachmentRepository;
    private final ObjectStorageService objectStorageService;
    private final AttachmentValidator validator;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final EmailNormalizer emailNormalizer;
    private final ActorAccessLifecycleService actorAccessLifecycleService;
    private final Clock clock;

    @Autowired
    public TechnicianService(
            TechnicianRepository technicianRepository,
            RepairAttachmentRepository attachmentRepository,
            ObjectStorageService objectStorageService,
            AttachmentValidator validator,
            PhoneNumberNormalizer phoneNumberNormalizer,
            EmailNormalizer emailNormalizer,
            ActorAccessLifecycleService actorAccessLifecycleService,
            Clock clock) {
        this.technicianRepository = technicianRepository;
        this.attachmentRepository = attachmentRepository;
        this.objectStorageService = objectStorageService;
        this.validator = validator != null ? validator : new AttachmentValidator();
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.emailNormalizer = emailNormalizer;
        this.actorAccessLifecycleService = actorAccessLifecycleService;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public TechnicianService(
            TechnicianRepository technicianRepository,
            PhoneNumberNormalizer phoneNumberNormalizer,
            EmailNormalizer emailNormalizer,
            ActorAccessLifecycleService actorAccessLifecycleService) {
        this(technicianRepository, null, null, new AttachmentValidator(), phoneNumberNormalizer, emailNormalizer, actorAccessLifecycleService, Clock.systemUTC());
    }

    public TechnicianService(
            TechnicianRepository technicianRepository,
            PhoneNumberNormalizer phoneNumberNormalizer) {
        this(technicianRepository, phoneNumberNormalizer, new EmailNormalizer(), null);
    }

    @Transactional(readOnly = true)
    public PageResponse<TechnicianSummaryResponse> list(
            String search,
            String phone,
            String specialization,
            Boolean active,
            Boolean telegramLinked,
            Pageable pageable) {
        String normalizedPhone = phone == null || phone.isBlank() ? null : phoneNumberNormalizer.normalize(phone);
        String normalizedSearchPhone = normalizeSearchPhone(search);
        return PageResponse.from(technicianRepository.findAll(filters(
                blankToNull(search),
                normalizedSearchPhone,
                normalizedPhone,
                blankToNull(specialization),
                active,
                telegramLinked), pageable).map(TechnicianMapper::summary));
    }

    @Transactional(readOnly = true)
    public TechnicianDetailResponse get(Long id) {
        return TechnicianMapper.details(find(id));
    }

    @Transactional
    public TechnicianDetailResponse create(TechnicianCreateRequest request) {
        Technician technician = new Technician(
                request.fullName().trim(),
                phoneNumberNormalizer.normalize(request.phone()),
                normalizeEmailOrNull(request.email()),
                blankToNull(request.specialization()),
                blankToNull(request.notes()),
                request.maximumConcurrentRequests(),
                request.preferredLanguage(),
                request.active(),
                now());
        validateMaximum(technician.getMaximumConcurrentRequests());
        try {
            Technician saved = technicianRepository.saveAndFlush(technician);
            LOGGER.info("Technician event operation=technician_created result=success technicianId={}", saved.getId());
            return TechnicianMapper.details(saved);
        } catch (DataIntegrityViolationException exception) {
            throw technicianConflict(exception);
        }
    }

    @Transactional
    public TechnicianDetailResponse update(Long id, TechnicianUpdateRequest request) {
        Technician technician = technicianRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
        validateMaximum(request.maximumConcurrentRequests());
        technician.updateProfile(
                request.fullName().trim(),
                phoneNumberNormalizer.normalize(request.phone()),
                normalizeEmailOrNull(request.email()),
                blankToNull(request.specialization()),
                blankToNull(request.notes()),
                request.maximumConcurrentRequests(),
                request.preferredLanguage(),
                now());
        try {
            return TechnicianMapper.details(technicianRepository.saveAndFlush(technician));
        } catch (DataIntegrityViolationException exception) {
            throw technicianConflict(exception);
        }
    }

    @Transactional
    public TechnicianDetailResponse changeActivation(Long id, boolean active, String reason) {
        Technician technician = technicianRepository.findByIdForUpdate(id).orElseThrow(this::notFound);
        boolean deactivated = technician.isActive() && !active;
        technician.setActive(active, now());
        if (deactivated && actorAccessLifecycleService != null) {
            actorAccessLifecycleService.onTechnicianDeactivated(id);
        }
        LOGGER.info(
                "Technician event operation=technician_activation_changed result=success technicianId={} active={} reason={}",
                id,
                active,
                reason == null ? "" : reason.trim());
        return TechnicianMapper.details(technician);
    }

    @Transactional
    public AvatarResponse uploadAvatar(Long technicianId, MultipartFile file) {
        Technician technician = technicianRepository.findByIdForUpdate(technicianId)
                .orElseThrow(this::notFound);
        if (!technician.isActive()) {
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

        String storageKey = "avatars/technicians/%d/%s".formatted(technicianId, UUID.randomUUID());

        try {
            objectStorageService.upload(new StorageUpload(
                    storageKey,
                    detected.contentType(),
                    bytes.length,
                    new ByteArrayInputStream(bytes)
            ));
        } catch (RuntimeException e) {
            LOGGER.warn("Technician avatar object storage upload failed technicianId={} storageKey={}", technicianId, storageKey, e);
            throw new BusinessRuleException("ATTACHMENT_STORAGE_FAILED", "Avatar storage operation failed.", 503);
        }

        RepairAttachment newAttachment = RepairAttachment.technicianUpload(
                null,
                AttachmentType.AVATAR,
                storageKey,
                originalFileName,
                technician,
                now
        );
        newAttachment.markAvailable(detected.contentType(), bytes.length, checksum, now);
        RepairAttachment savedAttachment = attachmentRepository.saveAndFlush(newAttachment);

        RepairAttachment previousAvatar = technician.getAvatarAttachment();
        technician.setAvatarAttachment(savedAttachment, now);
        technicianRepository.save(technician);

        if (previousAvatar != null && previousAvatar.getStatus() == AttachmentStatus.AVAILABLE) {
            previousAvatar.markDeleted("AVATAR_REPLACED", now);
            attachmentRepository.save(previousAvatar);
        }

        LOGGER.info("Technician event operation=avatar_uploaded result=success technicianId={} attachmentId={}", technicianId, savedAttachment.getId());
        return ImageAttachmentUtils.toAvatarResponse(savedAttachment, ImageAttachmentUtils.technicianAvatarDownloadUrl(technicianId));
    }

    @Transactional(readOnly = true)
    public AttachmentDownload downloadAvatar(Long technicianId) {
        Technician technician = find(technicianId);
        RepairAttachment avatar = technician.getAvatarAttachment();
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
    public void deleteAvatar(Long technicianId) {
        Technician technician = technicianRepository.findByIdForUpdate(technicianId)
                .orElseThrow(this::notFound);
        OffsetDateTime now = now();

        RepairAttachment avatar = technician.getAvatarAttachment();
        if (avatar != null) {
            technician.setAvatarAttachment(null, now);
            technicianRepository.save(technician);
            if (avatar.getStatus() == AttachmentStatus.AVAILABLE) {
                avatar.markDeleted("AVATAR_REMOVED", now);
                attachmentRepository.save(avatar);
            }
            LOGGER.info("Technician event operation=avatar_deleted result=success technicianId={} attachmentId={}", technicianId, avatar.getId());
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

    private Technician find(Long id) {
        return technicianRepository.findById(id).orElseThrow(this::notFound);
    }

    private void validateMaximum(int maximumConcurrentRequests) {
        if (maximumConcurrentRequests < 1) {
            throw new BusinessException(ErrorCode.INVALID_MAXIMUM_CONCURRENT_REQUESTS);
        }
    }

    private BusinessRuleException notFound() {
        return new BusinessRuleException(ErrorCode.TECHNICIAN_NOT_FOUND);
    }

    private BusinessRuleException technicianConflict(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause() != null ? exception.getMostSpecificCause().getMessage() : "";
        if (message != null && message.contains("telegram_user_id")) {
            return new BusinessRuleException(ErrorCode.TECHNICIAN_TELEGRAM_ID_ALREADY_EXISTS);
        }
        return new BusinessRuleException(ErrorCode.TECHNICIAN_PHONE_ALREADY_EXISTS);
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

    private Specification<Technician> filters(
            String search,
            String normalizedSearchPhone,
            String phone,
            String specialization,
            Boolean active,
            Boolean telegramLinked) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("fullName")), pattern),
                        builder.like(root.get("phone"), "%" + search + "%"),
                        normalizedSearchPhone == null
                                ? builder.disjunction()
                                : builder.equal(root.get("phone"), normalizedSearchPhone),
                        builder.like(builder.lower(root.get("specialization")), pattern)));
            }
            if (phone != null) {
                predicate = builder.and(predicate, builder.equal(root.get("phone"), phone));
            }
            if (specialization != null) {
                String pattern = "%" + specialization.toLowerCase(Locale.ROOT) + "%";
                predicate = builder.and(predicate, builder.like(builder.lower(root.get("specialization")), pattern));
            }
            if (active != null) {
                predicate = builder.and(predicate, builder.equal(root.get("active"), active));
            }
            if (telegramLinked != null && telegramLinked) {
                predicate = builder.and(predicate, builder.isNotNull(root.get("telegramUserId")));
            }
            if (telegramLinked != null && !telegramLinked) {
                predicate = builder.and(predicate, builder.isNull(root.get("telegramUserId")));
            }
            return predicate;
        };
    }
}
