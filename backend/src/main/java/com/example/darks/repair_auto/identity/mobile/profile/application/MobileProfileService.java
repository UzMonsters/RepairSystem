package com.example.darks.repair_auto.identity.mobile.profile.application;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfilePatchRequest;
import com.example.darks.repair_auto.identity.mobile.profile.api.dto.MobileProfileResponse;
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
import com.example.darks.repair_auto.shared.i18n.LanguageCode;
import com.example.darks.repair_auto.shared.i18n.SupportedLanguage;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MobileProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileProfileService.class);

    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;
    private final RepairAttachmentRepository attachmentRepository;
    private final ObjectStorageService objectStorageService;
    private final AttachmentValidator validator;
    private final Clock clock;

    @Autowired
    public MobileProfileService(
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            RepairAttachmentRepository attachmentRepository,
            ObjectStorageService objectStorageService,
            AttachmentValidator validator,
            Clock clock) {
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
        this.attachmentRepository = attachmentRepository;
        this.objectStorageService = objectStorageService;
        this.validator = validator != null ? validator : new AttachmentValidator();
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public MobileProfileService(
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository) {
        this(customerRepository, technicianRepository, null, null, new AttachmentValidator(), Clock.systemUTC());
    }

    public MobileProfileService(
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository,
            Clock clock) {
        this(customerRepository, technicianRepository, null, null, new AttachmentValidator(), clock);
    }

    @Transactional(readOnly = true)
    public MobileProfileResponse getProfile(AuthenticatedMobileActor actor) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        if (actor.isCustomer()) {
            Customer customer = customerRepository.findById(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!customer.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }
            return toCustomerResponse(customer);
        } else if (actor.isTechnician()) {
            Technician technician = technicianRepository.findById(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!technician.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }
            return toTechnicianResponse(technician);
        }

        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    @Transactional
    public MobileProfileResponse updateProfile(AuthenticatedMobileActor actor, MobileProfilePatchRequest request) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        OffsetDateTime now = now();

        if (actor.isCustomer()) {
            Customer customer = customerRepository.findById(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!customer.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }

            String newFullName = customer.getFullName();
            if (request.fullName() != null) {
                if (request.fullName().isBlank()) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
                newFullName = request.fullName().trim();
            }

            LanguageCode newLanguage = customer.getPreferredLanguage();
            if (request.preferredLanguage() != null) {
                SupportedLanguage parsed = SupportedLanguage.fromCode(request.preferredLanguage());
                if (parsed == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
                newLanguage = LanguageCode.valueOf(parsed.name());
            }

            customer.updateProfile(newFullName, customer.getPhone(), newLanguage, now);
            LOGGER.info("Updated mobile customer profile for customerId={}", customer.getId());
            return toCustomerResponse(customer);
        } else if (actor.isTechnician()) {
            Technician technician = technicianRepository.findById(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!technician.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }

            if (request.preferredLanguage() != null) {
                SupportedLanguage parsed = SupportedLanguage.fromCode(request.preferredLanguage());
                if (parsed == null) {
                    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
                }
                LanguageCode newLanguage = LanguageCode.valueOf(parsed.name());
                technician.updateTelegramLanguage(newLanguage, now);
                LOGGER.info("Updated mobile technician preferred language for technicianId={}", technician.getId());
            }

            return toTechnicianResponse(technician);
        }

        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    @Transactional
    public AvatarResponse uploadAvatar(AuthenticatedMobileActor actor, MultipartFile file) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
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

        if (actor.isCustomer()) {
            Customer customer = customerRepository.findByIdForUpdate(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!customer.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }

            String storageKey = "avatars/customers/%d/%s".formatted(actor.actorId(), UUID.randomUUID());
            try {
                objectStorageService.upload(new StorageUpload(
                        storageKey,
                        detected.contentType(),
                        bytes.length,
                        new ByteArrayInputStream(bytes)
                ));
            } catch (RuntimeException e) {
                LOGGER.warn("Mobile customer avatar upload failed customerId={} storageKey={}", actor.actorId(), storageKey, e);
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
            RepairAttachment saved = attachmentRepository.saveAndFlush(newAttachment);

            RepairAttachment previousAvatar = customer.getAvatarAttachment();
            customer.setAvatarAttachment(saved, now);
            customerRepository.save(customer);

            if (previousAvatar != null && previousAvatar.getStatus() == AttachmentStatus.AVAILABLE) {
                previousAvatar.markDeleted("AVATAR_REPLACED", now);
                attachmentRepository.save(previousAvatar);
            }

            LOGGER.info("Mobile customer event operation=avatar_uploaded result=success customerId={} attachmentId={}", customer.getId(), saved.getId());
            return ImageAttachmentUtils.toAvatarResponse(saved, ImageAttachmentUtils.mobileSelfAvatarDownloadUrl());
        } else if (actor.isTechnician()) {
            Technician technician = technicianRepository.findByIdForUpdate(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!technician.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }

            String storageKey = "avatars/technicians/%d/%s".formatted(actor.actorId(), UUID.randomUUID());
            try {
                objectStorageService.upload(new StorageUpload(
                        storageKey,
                        detected.contentType(),
                        bytes.length,
                        new ByteArrayInputStream(bytes)
                ));
            } catch (RuntimeException e) {
                LOGGER.warn("Mobile technician avatar upload failed technicianId={} storageKey={}", actor.actorId(), storageKey, e);
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
            RepairAttachment saved = attachmentRepository.saveAndFlush(newAttachment);

            RepairAttachment previousAvatar = technician.getAvatarAttachment();
            technician.setAvatarAttachment(saved, now);
            technicianRepository.save(technician);

            if (previousAvatar != null && previousAvatar.getStatus() == AttachmentStatus.AVAILABLE) {
                previousAvatar.markDeleted("AVATAR_REPLACED", now);
                attachmentRepository.save(previousAvatar);
            }

            LOGGER.info("Mobile technician event operation=avatar_uploaded result=success technicianId={} attachmentId={}", technician.getId(), saved.getId());
            return ImageAttachmentUtils.toAvatarResponse(saved, ImageAttachmentUtils.mobileSelfAvatarDownloadUrl());
        }

        throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }

    @Transactional(readOnly = true)
    public AttachmentDownload downloadAvatar(AuthenticatedMobileActor actor) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        RepairAttachment avatar = null;
        if (actor.isCustomer()) {
            Customer customer = customerRepository.findById(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!customer.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }
            avatar = customer.getAvatarAttachment();
        } else if (actor.isTechnician()) {
            Technician technician = technicianRepository.findById(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            if (!technician.isActive()) {
                throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE);
            }
            avatar = technician.getAvatarAttachment();
        } else {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

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
    public void deleteAvatar(AuthenticatedMobileActor actor) {
        if (actor == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        OffsetDateTime now = now();
        if (actor.isCustomer()) {
            Customer customer = customerRepository.findByIdForUpdate(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            RepairAttachment avatar = customer.getAvatarAttachment();
            if (avatar != null) {
                customer.setAvatarAttachment(null, now);
                customerRepository.save(customer);
                if (avatar.getStatus() == AttachmentStatus.AVAILABLE) {
                    avatar.markDeleted("AVATAR_REMOVED", now);
                    attachmentRepository.save(avatar);
                }
                LOGGER.info("Mobile customer event operation=avatar_deleted result=success customerId={} attachmentId={}", customer.getId(), avatar.getId());
            }
        } else if (actor.isTechnician()) {
            Technician technician = technicianRepository.findByIdForUpdate(actor.actorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_INACTIVE));
            RepairAttachment avatar = technician.getAvatarAttachment();
            if (avatar != null) {
                technician.setAvatarAttachment(null, now);
                technicianRepository.save(technician);
                if (avatar.getStatus() == AttachmentStatus.AVAILABLE) {
                    avatar.markDeleted("AVATAR_REMOVED", now);
                    attachmentRepository.save(avatar);
                }
                LOGGER.info("Mobile technician event operation=avatar_deleted result=success technicianId={} attachmentId={}", technician.getId(), avatar.getId());
            }
        } else {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private MobileProfileResponse toCustomerResponse(Customer customer) {
        String lang = customer.getPreferredLanguage() != null
                ? customer.getPreferredLanguage().name().toLowerCase(Locale.ROOT)
                : "uz";
        return MobileProfileResponse.forCustomer(
                customer.getId(),
                customer.getFullName(),
                customer.getPhone(),
                customer.getPhoneVerifiedAt() != null,
                customer.getEmail(),
                customer.getEmailVerifiedAt() != null,
                lang,
                customer.isTelegramLinked(),
                ImageAttachmentUtils.toAvatarResponse(customer.getAvatarAttachment(), ImageAttachmentUtils.mobileSelfAvatarDownloadUrl()));
    }

    private MobileProfileResponse toTechnicianResponse(Technician technician) {
        String lang = technician.getPreferredLanguage() != null
                ? technician.getPreferredLanguage().name().toLowerCase(Locale.ROOT)
                : "uz";
        return MobileProfileResponse.forTechnician(
                technician.getId(),
                technician.getFullName(),
                technician.getPhone(),
                technician.getPhoneVerifiedAt() != null,
                technician.getEmail(),
                technician.getEmailVerifiedAt() != null,
                lang,
                technician.isTelegramLinked(),
                technician.getSpecialization(),
                technician.getMaximumConcurrentRequests(),
                technician.isActive(),
                ImageAttachmentUtils.toAvatarResponse(technician.getAvatarAttachment(), ImageAttachmentUtils.mobileSelfAvatarDownloadUrl()));
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

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
