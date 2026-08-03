package com.example.darks.repair_auto.telegram.core.infrastructure;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram")
public class TelegramProperties {

    private boolean enabled;
    private String botToken = "";
    private String webhookSecret = "";
    private String botUsername = "repairauto_bot";
    private URI apiBaseUrl = URI.create("https://api.telegram.org");
    private URI fileBaseUrl = URI.create("https://api.telegram.org");
    private Duration requestTimeout = Duration.ofSeconds(10);
    private int maxPendingPhotos = 3;
    private int maxWebhookBodySize = 262_144;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken == null ? "" : botToken;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(String botUsername) {
        this.botUsername = botUsername == null || botUsername.isBlank() ? "repairauto_bot" : botUsername.trim();
    }

    public URI getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(URI apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public URI getFileBaseUrl() {
        return fileBaseUrl;
    }

    public void setFileBaseUrl(URI fileBaseUrl) {
        this.fileBaseUrl = fileBaseUrl;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public int getMaxPendingPhotos() {
        return maxPendingPhotos;
    }

    public void setMaxPendingPhotos(int maxPendingPhotos) {
        this.maxPendingPhotos = maxPendingPhotos;
    }

    public int getMaxWebhookBodySize() {
        return maxWebhookBodySize;
    }

    public void setMaxWebhookBodySize(int maxWebhookBodySize) {
        this.maxWebhookBodySize = maxWebhookBodySize;
    }
}
