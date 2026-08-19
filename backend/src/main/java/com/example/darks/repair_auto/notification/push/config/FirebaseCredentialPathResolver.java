package com.example.darks.repair_auto.notification.push.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

final class FirebaseCredentialPathResolver {

    static final String RENDER_SECRETS_DIR_PROPERTY = "repairauto.firebase.render-secrets-dir";
    private static final String DEFAULT_RENDER_SECRETS_DIR = "/etc/secrets";

    private FirebaseCredentialPathResolver() {
    }

    static Optional<Path> resolveReadablePath(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return Optional.empty();
        }
        Path directPath = Path.of(configuredPath.trim());
        if (Files.isRegularFile(directPath) && Files.isReadable(directPath)) {
            return Optional.of(directPath);
        }
        if (!directPath.isAbsolute()) {
            Path renderSecretPath = renderSecretsDir().resolve(directPath).normalize();
            if (Files.isRegularFile(renderSecretPath) && Files.isReadable(renderSecretPath)) {
                return Optional.of(renderSecretPath);
            }
        }
        return Optional.empty();
    }

    static String describeSearch(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return "";
        }
        Path directPath = Path.of(configuredPath.trim());
        if (directPath.isAbsolute()) {
            return directPath.toString();
        }
        return directPath + " or " + renderSecretsDir().resolve(directPath).normalize();
    }

    private static Path renderSecretsDir() {
        return Path.of(System.getProperty(RENDER_SECRETS_DIR_PROPERTY, DEFAULT_RENDER_SECRETS_DIR));
    }
}
