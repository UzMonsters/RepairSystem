package com.example.darks.repair_auto.telegram.core.infrastructure;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram")
public class TelegramProperties {

    private boolean enabled;
    private Bot customer = new Bot("repairauto_bot");
    private Bot technician = new Bot("repairauto_staff_bot");
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

    public Bot getCustomer() {
        return customer;
    }

    public void setCustomer(Bot customer) {
        this.customer = customer == null ? new Bot("repairauto_bot") : customer;
    }

    public Bot getTechnician() {
        return technician;
    }

    public void setTechnician(Bot technician) {
        this.technician = technician == null ? new Bot("repairauto_staff_bot") : technician;
    }

    public String getBotToken() {
        return customer.getBotToken();
    }

    public void setBotToken(String botToken) {
        customer.setBotToken(botToken);
    }

    public String getWebhookSecret() {
        return customer.getWebhookSecret();
    }

    public void setWebhookSecret(String webhookSecret) {
        customer.setWebhookSecret(webhookSecret);
    }

    public String getBotUsername() {
        return customer.getBotUsername();
    }

    public void setBotUsername(String botUsername) {
        customer.setBotUsername(botUsername);
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

    public static class Bot {

        private String botToken = "";
        private String webhookSecret = "";
        private String botUsername;

        public Bot() {
            this("repairauto_bot");
        }

        public Bot(String botUsername) {
            this.botUsername = botUsername;
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
            if (botUsername != null && !botUsername.isBlank()) {
                this.botUsername = botUsername.trim();
            }
        }
    }
}
