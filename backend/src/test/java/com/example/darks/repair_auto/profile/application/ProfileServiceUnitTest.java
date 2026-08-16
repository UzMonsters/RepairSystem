package com.example.darks.repair_auto.profile.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.profile.api.dto.ProfileResponse;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentDownload;
import com.example.darks.repair_auto.repair.attachment.application.AttachmentValidator;
import com.example.darks.repair_auto.repair.attachment.domain.AttachmentType;
import com.example.darks.repair_auto.repair.attachment.domain.RepairAttachment;
import com.example.darks.repair_auto.repair.attachment.infrastructure.persistence.RepairAttachmentRepository;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageProperties;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObjectDownload;
import com.example.darks.repair_auto.settings.api.dto.UserSettingsResponse;
import com.example.darks.repair_auto.settings.application.SettingsService;
import com.example.darks.repair_auto.settings.domain.DateFormat;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.settings.domain.Theme;
import com.example.darks.repair_auto.settings.domain.TimeFormat;
import com.example.darks.repair_auto.shared.error.ResourceNotFoundException;
import com.example.darks.repair_auto.shared.phone.PhoneNumberNormalizer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfileServiceUnitTest {

    private UserRepository userRepository;
    private RepairAttachmentRepository attachmentRepository;
    private SettingsService settingsService;
    private ObjectStorageService objectStorageService;
    private StorageProperties storageProperties;
    private AttachmentValidator validator;
    private PhoneNumberNormalizer phoneNumberNormalizer;
    private Clock clock;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        attachmentRepository = mock(RepairAttachmentRepository.class);
        settingsService = mock(SettingsService.class);
        objectStorageService = mock(ObjectStorageService.class);
        storageProperties = mock(StorageProperties.class);
        validator = mock(AttachmentValidator.class);
        phoneNumberNormalizer = mock(PhoneNumberNormalizer.class);
        clock = Clock.systemUTC();

        when(storageProperties.downloadUrlTtl()).thenReturn(Duration.ofMinutes(10));

        profileService = new ProfileService(
                userRepository,
                attachmentRepository,
                settingsService,
                objectStorageService,
                storageProperties,
                validator,
                phoneNumberNormalizer,
                clock
        );
    }

    @Test
    void shouldReturnProfileWithAvatarMetadataAndNoPresignedUrl() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        User user = new User("System Admin", "admin@example.com", "pass", UserRole.ADMIN, true, now);
        RepairAttachment avatar = new RepairAttachment(null, AttachmentType.AVATAR, "avatars/1/uuid", "avatar.png", user, now);
        avatar.markAvailable("image/png", 1024L, "checksum", now);
        user.setAvatarAttachment(avatar, now);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(settingsService.getUserSettings(1L)).thenReturn(new UserSettingsResponse(
                Language.RU, DateFormat.DD_MM_YYYY, TimeFormat.HOUR_12, Theme.DARK
        ));

        ProfileResponse profile = profileService.getCurrentProfile(1L);

        assertThat(profile.avatar()).isNotNull();
        assertThat(profile.avatar().fileName()).isEqualTo("avatar.png");
        assertThat(profile.avatar().contentType()).isEqualTo("image/png");

        verify(objectStorageService, never()).createDownloadUrl(any(), any(), any());
    }

    @Test
    void shouldStreamAvatarContentViaDownloadAvatar() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        User user = new User("System Admin", "admin@example.com", "pass", UserRole.ADMIN, true, now);
        RepairAttachment avatar = new RepairAttachment(null, AttachmentType.AVATAR, "avatars/1/uuid", "avatar.png", user, now);
        avatar.markAvailable("image/png", 8L, "checksum", now);
        user.setAvatarAttachment(avatar, now);

        InputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        StoredObjectDownload storedDownload = new StoredObjectDownload("image/png", 8L, stream);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(objectStorageService.download("avatars/1/uuid")).thenReturn(storedDownload);

        AttachmentDownload download = profileService.downloadAvatar(1L);

        assertThat(download.contentType()).isEqualTo("image/png");
        assertThat(download.sizeBytes()).isEqualTo(8L);
        assertThat(download.inputStream()).isNotNull();
    }

    @Test
    void shouldThrowNotFoundWhenAvatarNotAvailable() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        User user = new User("System Admin", "admin@example.com", "pass", UserRole.ADMIN, true, now);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.downloadAvatar(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Avatar was not found.");
    }
}
