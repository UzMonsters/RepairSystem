package com.example.darks.repair_auto.identity.mobile.telegram;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram-login")
public class TelegramLoginProperties {

    private String issuer = "https://oauth.telegram.org";
    private String jwksUri = "https://oauth.telegram.org/.well-known/jwks.json";
    private AppClient customer = new AppClient();
    private AppClient technician = new AppClient();
    private Duration allowedClockSkew = Duration.ofSeconds(60);

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = (issuer != null && !issuer.isBlank()) ? issuer.trim() : "https://oauth.telegram.org";
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = (jwksUri != null && !jwksUri.isBlank())
                ? jwksUri.trim()
                : "https://oauth.telegram.org/.well-known/jwks.json";
    }

    public AppClient getCustomer() {
        return customer;
    }

    public void setCustomer(AppClient customer) {
        this.customer = customer != null ? customer : new AppClient();
    }

    public AppClient getTechnician() {
        return technician;
    }

    public void setTechnician(AppClient technician) {
        this.technician = technician != null ? technician : new AppClient();
    }

    public Duration getAllowedClockSkew() {
        return allowedClockSkew;
    }

    public void setAllowedClockSkew(Duration allowedClockSkew) {
        this.allowedClockSkew = allowedClockSkew != null ? allowedClockSkew : Duration.ofSeconds(60);
    }

    public static class AppClient {
        private String clientId = "";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId != null ? clientId.trim() : "";
        }
    }
}
