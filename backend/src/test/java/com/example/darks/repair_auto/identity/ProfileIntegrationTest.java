package com.example.darks.repair_auto.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.profile.api.dto.AvatarResponse;
import com.example.darks.repair_auto.profile.api.dto.ProfileResponse;
import com.example.darks.repair_auto.profile.api.dto.UpdateProfileRequest;
import com.example.darks.repair_auto.profile.application.ProfileService;
import com.example.darks.repair_auto.settings.domain.DateFormat;
import com.example.darks.repair_auto.settings.domain.Language;
import com.example.darks.repair_auto.settings.domain.Theme;
import com.example.darks.repair_auto.settings.domain.TimeFormat;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.error.ResourceNotFoundException;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.ObjectStorageService;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StorageUpload;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObject;
import com.example.darks.repair_auto.repair.attachment.infrastructure.storage.StoredObjectDownload;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
class ProfileIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordService passwordService;

    private User managerUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        managerUser = userRepository.saveAndFlush(new User(
                "Akmal Karimov",
                "manager_profile@example.com",
                passwordService.hash("ManagerPass123!"),
                UserRole.MANAGER,
                true,
                now
        ));
    }

    @Test
    void shouldFetchCurrentProfileWithoutAvatar() {
        ProfileResponse response = profileService.getCurrentProfile(managerUser.getId());

        assertThat(response.id()).isEqualTo(managerUser.getId());
        assertThat(response.username()).isEqualTo("manager_profile@example.com");
        assertThat(response.fullName()).isEqualTo("Akmal Karimov");
        assertThat(response.role()).isEqualTo(UserRole.MANAGER);
        assertThat(response.active()).isTrue();
        assertThat(response.avatar()).isNull();
        assertThat(response.language()).isEqualTo(Language.UZ);
    }

    @Test
    void shouldUpdateSelfProfileAndPreferences() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Akmal Karimov Updated",
                "+998901234567",
                Language.RU,
                DateFormat.YYYY_MM_DD,
                TimeFormat.HOUR_24,
                Theme.DARK
        );

        ProfileResponse response = profileService.updateCurrentProfile(managerUser.getId(), request);

        assertThat(response.fullName()).isEqualTo("Akmal Karimov Updated");
        assertThat(response.phone()).isEqualTo("+998901234567");
        assertThat(response.language()).isEqualTo(Language.RU);
        assertThat(response.dateFormat()).isEqualTo(DateFormat.YYYY_MM_DD);
        assertThat(response.theme()).isEqualTo(Theme.DARK);
    }

    @Test
    void shouldUploadReplaceAndDeleteAvatar() {
        // 1. Upload JPEG avatar
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
        MockMultipartFile file1 = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", jpegBytes);

        AvatarResponse avatar1 = profileService.uploadAvatar(managerUser.getId(), file1);
        assertThat(avatar1).isNotNull();
        assertThat(avatar1.fileName()).isEqualTo("avatar.jpg");
        assertThat(avatar1.contentType()).isEqualTo("image/jpeg");

        ProfileResponse profileWithAvatar = profileService.getCurrentProfile(managerUser.getId());
        assertThat(profileWithAvatar.avatar()).isNotNull();
        assertThat(profileWithAvatar.avatar().attachmentId()).isEqualTo(avatar1.attachmentId());

        // 2. Replace with PNG avatar
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file2 = new MockMultipartFile("file", "avatar.png", "image/png", pngBytes);

        AvatarResponse avatar2 = profileService.uploadAvatar(managerUser.getId(), file2);
        assertThat(avatar2).isNotNull();
        assertThat(avatar2.attachmentId()).isNotEqualTo(avatar1.attachmentId());

        ProfileResponse profileWithReplacedAvatar = profileService.getCurrentProfile(managerUser.getId());
        assertThat(profileWithReplacedAvatar.avatar().attachmentId()).isEqualTo(avatar2.attachmentId());

        // 3. Delete avatar
        profileService.deleteAvatar(managerUser.getId());
        ProfileResponse profileAfterDelete = profileService.getCurrentProfile(managerUser.getId());
        assertThat(profileAfterDelete.avatar()).isNull();
    }

    @Test
    void shouldThrowNotFoundWhenDownloadingNonExistentAvatar() {
        assertThatThrownBy(() -> profileService.downloadAvatar(managerUser.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Avatar was not found");
    }

    @Test
    void shouldRejectUnsupportedAvatarFileType() {
        byte[] invalidBytes = "Not an image file content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "document.txt", "text/plain", invalidBytes);

        assertThatThrownBy(() -> profileService.uploadAvatar(managerUser.getId(), file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("supported");
    }

    @TestConfiguration
    static class ProfileTestConfiguration {
        @Bean
        @Primary
        ObjectStorageService objectStorageService() {
            return new FakeObjectStorageService();
        }
    }

    static final class FakeObjectStorageService implements ObjectStorageService {
        private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

        @Override
        public StoredObject upload(StorageUpload command) {
            try {
                objects.put(command.storageKey(), command.inputStream().readAllBytes());
                return new StoredObject(command.storageKey(), command.contentType(), command.sizeBytes());
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public StoredObjectDownload download(String storageKey) {
            byte[] bytes = objects.get(storageKey);
            if (bytes == null) {
                throw new ResourceNotFoundException("Storage object not found.");
            }
            return new StoredObjectDownload("image/png", bytes.length, new ByteArrayInputStream(bytes));
        }

        @Override
        public URI createDownloadUrl(String storageKey, String downloadFileName, Duration ttl) {
            return URI.create("https://storage.test/download");
        }

        @Override
        public void delete(String storageKey) {
            objects.remove(storageKey);
        }

        @Override
        public boolean exists(String storageKey) {
            return objects.containsKey(storageKey);
        }
    }
}
