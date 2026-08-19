package com.example.darks.repair_auto.profile.application;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
import com.example.darks.repair_auto.profile.api.dto.ProfileResponse;
import com.example.darks.repair_auto.profile.api.dto.UpdateProfileRequest;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentValidator;
import com.example.darks.repair_auto.repair.attachment.application.DetectedFile;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentStatus;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageProperties;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageUpload;
import com.example.darks.repair_auto.settings.api.dto.UserSettingsResponse;
import com.example.darks.repair_auto.settings.api.dto.UserSettingsUpdateRequest;
import com.example.darks.repair_auto.settings.application.SettingsService;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.error.ResourceNotFoundException;
import com.example.darks.repair_auto.shared.phone.PhoneNumberNormalizer;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentDownload;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObjectDownload;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileService.class);

    private final UserRepository userRepository;
    private final RepairAttachmentRepository attachmentRepository;
    private final SettingsService settingsService;
    private final ObjectStorageService objectStorageService;
    private final StorageProperties storageProperties;
    private final AttachmentValidator validator;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final Clock clock;

    @Autowired
    public ProfileService(
            UserRepository userRepository,
            RepairAttachmentRepository attachmentRepository,
            SettingsService settingsService,
            ObjectStorageService objectStorageService,
            StorageProperties storageProperties,
            AttachmentValidator validator,
            PhoneNumberNormalizer phoneNumberNormalizer) {
        this(
                userRepository,
                attachmentRepository,
                settingsService,
                objectStorageService,
                storageProperties,
                validator,
                phoneNumberNormalizer,
                Clock.systemUTC());
    }

    ProfileService(
            UserRepository userRepository,
            RepairAttachmentRepository attachmentRepository,
            SettingsService settingsService,
            ObjectStorageService objectStorageService,
            StorageProperties storageProperties,
            AttachmentValidator validator,
            PhoneNumberNormalizer phoneNumberNormalizer,
            Clock clock) {
        this.userRepository = userRepository;
        this.attachmentRepository = attachmentRepository;
        this.settingsService = settingsService;
        this.objectStorageService = objectStorageService;
        this.storageProperties = storageProperties;
        this.validator = validator;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getCurrentProfile(Long userId) {
        User user = findUser(userId);
        UserSettingsResponse settings = settingsService.getUserSettings(userId);
        AvatarResponse avatar = buildAvatarResponse(user.getAvatarAttachment());
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                avatar,
                settings.language(),
                settings.dateFormat(),
                settings.timeFormat(),
                settings.theme(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Transactional
    public ProfileResponse updateCurrentProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found."));
        OffsetDateTime now = now();

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim(), now);
        }

        if (request.phone() != null) {
            if (request.phone().isBlank()) {
                user.setPhone(null, now);
            } else {
                user.setPhone(phoneNumberNormalizer.normalize(request.phone()), now);
            }
        }

        userRepository.save(user);

        UserSettingsResponse currentSettings = settingsService.getUserSettings(userId);
        var updatedLanguage = request.language() != null ? request.language() : currentSettings.language();
        var updatedDateFormat = request.dateFormat() != null ? request.dateFormat() : currentSettings.dateFormat();
        var updatedTimeFormat = request.timeFormat() != null ? request.timeFormat() : currentSettings.timeFormat();
        var updatedTheme = request.theme() != null ? request.theme() : currentSettings.theme();

        settingsService.updateUserSettings(
                userId,
                new UserSettingsUpdateRequest(updatedLanguage, updatedDateFormat, updatedTimeFormat, updatedTheme)
        );

        LOGGER.info("Profile event operation=profile_updated result=success userId={}", userId);
        return getCurrentProfile(userId);
    }

    @Transactional
    public AvatarResponse uploadAvatar(Long userId, MultipartFile file) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found."));

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

        String storageKey = "avatars/%d/%s".formatted(userId, UUID.randomUUID());

        try {
            objectStorageService.upload(new StorageUpload(
                    storageKey,
                    detected.contentType(),
                    bytes.length,
                    new ByteArrayInputStream(bytes)
            ));
        } catch (RuntimeException e) {
            LOGGER.warn("Avatar object storage upload failed userId={} storageKey={}", userId, storageKey, e);
            throw new BusinessRuleException("ATTACHMENT_STORAGE_FAILED", "Avatar storage operation failed.", 503);
        }

        RepairAttachment newAttachment = new RepairAttachment(
                null,
                AttachmentType.AVATAR,
                storageKey,
                originalFileName,
                user,
                now
        );
        newAttachment.markAvailable(detected.contentType(), bytes.length, checksum, now);
        RepairAttachment savedAttachment = attachmentRepository.saveAndFlush(newAttachment);

        RepairAttachment previousAvatar = user.getAvatarAttachment();
        user.setAvatarAttachment(savedAttachment, now);
        userRepository.save(user);

        if (previousAvatar != null && previousAvatar.getStatus() == AttachmentStatus.AVAILABLE) {
            previousAvatar.markDeleted(user, "AVATAR_REPLACED", now);
            attachmentRepository.save(previousAvatar);
        }

        LOGGER.info("Profile event operation=avatar_uploaded result=success userId={} attachmentId={}", userId, savedAttachment.getId());
        return buildAvatarResponse(savedAttachment);
    }

    @Transactional
    public void deleteAvatar(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found."));
        OffsetDateTime now = now();

        RepairAttachment avatar = user.getAvatarAttachment();
        if (avatar != null) {
            user.setAvatarAttachment(null, now);
            userRepository.save(user);
            if (avatar.getStatus() == AttachmentStatus.AVAILABLE) {
                avatar.markDeleted(user, "AVATAR_REMOVED", now);
                attachmentRepository.save(avatar);
            }
            LOGGER.info("Profile event operation=avatar_deleted result=success userId={} attachmentId={}", userId, avatar.getId());
        }
    }

    @Transactional(readOnly = true)
    public AttachmentDownload downloadAvatar(Long userId) {
        User user = findUser(userId);
        RepairAttachment avatar = user.getAvatarAttachment();
        if (avatar == null || !avatar.isAvailable()) {
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

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found."));
    }

    private AvatarResponse buildAvatarResponse(RepairAttachment attachment) {
        if (attachment == null || !attachment.isAvailable()) {
            return null;
        }
        return new AvatarResponse(
                attachment.getId(),
                attachment.getOriginalFileName(),
                attachment.getContentType()
        );
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
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
