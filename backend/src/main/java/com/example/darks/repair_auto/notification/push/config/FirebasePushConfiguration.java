package com.example.darks.repair_auto.notification.push.config;

import com.example.darks.repair_auto.notification.push.gateway.FirebaseMessagingClient;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FirebasePushProperties.class)
public class FirebasePushConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FirebasePushConfiguration.class);

    @Bean
    @ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true")
    public FirebaseApp firebaseApp(FirebasePushProperties properties) throws IOException {
        LOGGER.info("Initializing Firebase App for project: {}", properties.projectId());
        GoogleCredentials credentials;
        if (properties.credentialsPath() != null && !properties.credentialsPath().isBlank()) {
            Path credentialsPath = resolveCredentialsPath(properties.credentialsPath(), "APP_FIREBASE_CREDENTIALS_PATH");
            LOGGER.info("Loading Google credentials from configured path");
            try (InputStream is = Files.newInputStream(credentialsPath)) {
                credentials = GoogleCredentials.fromStream(is);
            }
        } else if (System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null
                && !System.getenv("GOOGLE_APPLICATION_CREDENTIALS").isBlank()) {
            String googleApplicationCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            Path credentialsPath = resolveCredentialsPath(googleApplicationCredentials, "GOOGLE_APPLICATION_CREDENTIALS");
            LOGGER.info("Loading Google credentials from GOOGLE_APPLICATION_CREDENTIALS");
            try (InputStream is = Files.newInputStream(credentialsPath)) {
                credentials = GoogleCredentials.fromStream(is);
            }
        } else {
            LOGGER.info("Loading Google credentials from Application Default Credentials");
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setConnectTimeout((int) properties.connectTimeout().toMillis())
                .setReadTimeout((int) properties.readTimeout().toMillis());

        if (properties.projectId() != null && !properties.projectId().isBlank()) {
            builder.setProjectId(properties.projectId());
        }

        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(builder.build());
        }
        return FirebaseApp.getInstance();
    }

    private Path resolveCredentialsPath(String configuredPath, String sourceName) {
        return FirebaseCredentialPathResolver.resolveReadablePath(configuredPath)
                .orElseThrow(() -> new IllegalStateException(sourceName
                        + " points to an unreadable Firebase credential file. Checked: "
                        + FirebaseCredentialPathResolver.describeSearch(configuredPath)
                        + ". On Render, mount the service-account JSON as a secret file and set "
                        + sourceName + " to /etc/secrets/<filename>, or set APP_FIREBASE_ENABLED=false."));
    }

    @Bean
    @ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true")
    public FirebaseMessagingClient productionFirebaseMessagingClient(FirebaseApp firebaseApp) {
        return message -> FirebaseMessaging.getInstance(firebaseApp).send(message);
    }

    @Bean
    @ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "false", matchIfMissing = true)
    public FirebaseMessagingClient disabledFirebaseMessagingClient() {
        return new FirebaseMessagingClient() {
            @Override
            public String send(Message message) {
                LOGGER.debug("Firebase push is disabled. Simulated message send.");
                return "mock-fcm-message-" + UUID.randomUUID();
            }
        };
    }
}
