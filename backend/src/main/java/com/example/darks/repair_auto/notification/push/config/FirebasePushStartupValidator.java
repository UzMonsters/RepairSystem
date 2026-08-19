package com.example.darks.repair_auto.notification.push.config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class FirebasePushStartupValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirebasePushStartupValidator.class);

    private final FirebasePushProperties properties;
    private volatile boolean valid = false;
    private volatile String validationError = null;

    public FirebasePushStartupValidator(FirebasePushProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        if (!properties.enabled()) {
            LOGGER.info("Firebase Push is DISABLED (app.firebase.enabled=false). Push notifications will be skipped safely.");
            this.valid = true;
            this.validationError = null;
            return;
        }

        LOGGER.info("Validating Firebase Push production configuration...");

        if (properties.projectId() == null || properties.projectId().isBlank()) {
            String error = "Firebase Push is enabled but app.firebase.project-id is missing or blank.";
            LOGGER.error(error);
            this.valid = false;
            this.validationError = error;
            throw new IllegalStateException(error);
        }

        String credentialSource;
        if (properties.credentialsPath() != null && !properties.credentialsPath().isBlank()) {
            Path credFile = requireReadableCredentialFile(
                    properties.credentialsPath(),
                    "Firebase credentialsPath is configured but file cannot be read");
            assertNonEmptyCredentialFile(credFile, "Firebase credentialsPath file is empty");
            credentialSource = "CONFIGURED_FILE";
        } else {
            String gac = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            if (gac != null && !gac.isBlank()) {
                requireReadableCredentialFile(
                        gac,
                        "GOOGLE_APPLICATION_CREDENTIALS environment variable points to unreadable file");
                credentialSource = "GOOGLE_APPLICATION_CREDENTIALS_ENV";
            } else {
                credentialSource = "APPLICATION_DEFAULT_CREDENTIALS";
            }
        }

        this.valid = true;
        this.validationError = null;
        LOGGER.info("Firebase Push configuration successfully validated. projectId={}, credentialSource={}",
                properties.projectId(), credentialSource);
    }

    public boolean isValid() {
        return valid;
    }

    public String getValidationError() {
        return validationError;
    }

    private Path requireReadableCredentialFile(String configuredPath, String message) {
        return FirebaseCredentialPathResolver.resolveReadablePath(configuredPath)
                .orElseThrow(() -> failValidation(message + ": "
                        + FirebaseCredentialPathResolver.describeSearch(configuredPath)));
    }

    private void assertNonEmptyCredentialFile(Path credentialFile, String message) {
        try (InputStream is = Files.newInputStream(credentialFile)) {
            if (is.read() == -1) {
                throw failValidation(message + ": " + credentialFile);
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception ex) {
            throw failValidation("Failed to read Firebase credential file: " + ex.getMessage(), ex);
        }
    }

    private IllegalStateException failValidation(String error) {
        LOGGER.error(error);
        this.valid = false;
        this.validationError = error;
        return new IllegalStateException(error);
    }

    private IllegalStateException failValidation(String error, Exception cause) {
        LOGGER.error(error);
        this.valid = false;
        this.validationError = error;
        return new IllegalStateException(error, cause);
    }
}
