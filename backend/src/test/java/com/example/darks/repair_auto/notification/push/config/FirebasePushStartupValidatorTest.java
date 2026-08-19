package com.example.darks.repair_auto.notification.push.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FirebasePushStartupValidatorTest {

    @AfterEach
    void clearRenderSecretsOverride() {
        System.clearProperty(FirebaseCredentialPathResolver.RENDER_SECRETS_DIR_PROPERTY);
    }

    @Test
    void givenFirebaseDisabled_whenValidate_thenSuccessfulAndValid() {
        FirebasePushProperties properties = new FirebasePushProperties(
                false, null, null, Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofDays(90), false, Duration.ofDays(1));
        FirebasePushStartupValidator validator = new FirebasePushStartupValidator(properties);

        validator.validate();

        assertThat(validator.isValid()).isTrue();
        assertThat(validator.getValidationError()).isNull();
    }

    @Test
    void givenFirebaseEnabledWithBlankProjectId_whenValidate_thenThrowsIllegalStateException() {
        FirebasePushProperties properties = new FirebasePushProperties(
                true, "  ", null, Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofDays(90), false, Duration.ofDays(1));
        FirebasePushStartupValidator validator = new FirebasePushStartupValidator(properties);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("project-id is missing or blank");

        assertThat(validator.isValid()).isFalse();
        assertThat(validator.getValidationError()).isNotNull();
    }

    @Test
    void givenFirebaseEnabledWithNonExistentCredentialsFile_whenValidate_thenThrowsIllegalStateException() {
        FirebasePushProperties properties = new FirebasePushProperties(
                true, "repairauto-dev", "/non/existent/path/service-account.json", Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofDays(90), false, Duration.ofDays(1));
        FirebasePushStartupValidator validator = new FirebasePushStartupValidator(properties);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("credentialsPath is configured but file cannot be read");

        assertThat(validator.isValid()).isFalse();
    }

    @Test
    void givenFirebaseEnabledWithValidCredentialsFile_whenValidate_thenSuccessful(@TempDir File tempDir) throws IOException {
        File credFile = new File(tempDir, "service-account.json");
        try (FileOutputStream fos = new FileOutputStream(credFile)) {
            fos.write("{\"project_id\": \"repairauto-dev\"}".getBytes(StandardCharsets.UTF_8));
        }

        FirebasePushProperties properties = new FirebasePushProperties(
                true, "repairauto-dev", credFile.getAbsolutePath(), Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofDays(90), false, Duration.ofDays(1));
        FirebasePushStartupValidator validator = new FirebasePushStartupValidator(properties);

        validator.validate();

        assertThat(validator.isValid()).isTrue();
        assertThat(validator.getValidationError()).isNull();
    }

    @Test
    void givenFirebaseEnabledWithRelativeRenderSecretFile_whenValidate_thenSuccessful(@TempDir Path tempDir)
            throws IOException {
        System.setProperty(FirebaseCredentialPathResolver.RENDER_SECRETS_DIR_PROPERTY, tempDir.toString());
        Path credFile = tempDir.resolve("service-account.json");
        java.nio.file.Files.writeString(credFile, "{\"project_id\":\"repairauto-prod\"}", StandardCharsets.UTF_8);

        FirebasePushProperties properties = new FirebasePushProperties(
                true, "repairauto-prod", "service-account.json", Duration.ofSeconds(5), Duration.ofSeconds(5), Duration.ofDays(90), false, Duration.ofDays(1));
        FirebasePushStartupValidator validator = new FirebasePushStartupValidator(properties);

        validator.validate();

        assertThat(validator.isValid()).isTrue();
        assertThat(validator.getValidationError()).isNull();
    }
}
