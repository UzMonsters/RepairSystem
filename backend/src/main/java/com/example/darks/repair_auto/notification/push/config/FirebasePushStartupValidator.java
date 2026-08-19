package com.example.darks.repair_auto.notification.push.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
            File credFile = new File(properties.credentialsPath());
            if (!credFile.exists() || !credFile.canRead()) {
                String error = "Firebase credentialsPath is configured but file cannot be read: " + properties.credentialsPath();
                LOGGER.error(error);
                this.valid = false;
                this.validationError = error;
                throw new IllegalStateException(error);
            }
            try (InputStream is = new FileInputStream(credFile)) {
                if (is.read() == -1) {
                    String error = "Firebase credentialsPath file is empty: " + properties.credentialsPath();
                    LOGGER.error(error);
                    this.valid = false;
                    this.validationError = error;
                    throw new IllegalStateException(error);
                }
            } catch (Exception ex) {
                String error = "Failed to read Firebase credentialsPath file: " + ex.getMessage();
                LOGGER.error(error);
                this.valid = false;
                this.validationError = error;
                throw new IllegalStateException(error, ex);
            }
            credentialSource = "CONFIGURED_FILE";
        } else {
            String gac = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            if (gac != null && !gac.isBlank()) {
                File gacFile = new File(gac);
                if (!gacFile.exists() || !gacFile.canRead()) {
                    String error = "GOOGLE_APPLICATION_CREDENTIALS environment variable points to unreadable file: " + gac;
                    LOGGER.error(error);
                    this.valid = false;
                    this.validationError = error;
                    throw new IllegalStateException(error);
                }
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
}
