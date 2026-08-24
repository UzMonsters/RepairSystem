package com.example.darks.repair_auto.identity.mobile.google;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.google-oidc")
public class GoogleOidcProperties {

    private boolean enabled = true;
    private String jwksUri = "https://www.googleapis.com/oauth2/v3/certs";
    private List<String> issuers = new ArrayList<>(List.of("https://accounts.google.com", "accounts.google.com"));
    private List<String> customerAllowedAudiences = new ArrayList<>();
    private List<String> technicianAllowedAudiences = new ArrayList<>();
    private Duration allowedClockSkew = Duration.ofSeconds(60);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public List<String> getIssuers() {
        return issuers;
    }

    public void setIssuers(List<String> issuers) {
        this.issuers = issuers == null ? List.of() : issuers;
    }

    public List<String> getCustomerAllowedAudiences() {
        return customerAllowedAudiences;
    }

    public void setCustomerAllowedAudiences(List<String> customerAllowedAudiences) {
        this.customerAllowedAudiences = customerAllowedAudiences == null ? List.of() : customerAllowedAudiences;
    }

    public List<String> getTechnicianAllowedAudiences() {
        return technicianAllowedAudiences;
    }

    public void setTechnicianAllowedAudiences(List<String> technicianAllowedAudiences) {
        this.technicianAllowedAudiences = technicianAllowedAudiences == null ? List.of() : technicianAllowedAudiences;
    }

    public Duration getAllowedClockSkew() {
        return allowedClockSkew;
    }

    public void setAllowedClockSkew(Duration allowedClockSkew) {
        this.allowedClockSkew = allowedClockSkew == null ? Duration.ofSeconds(60) : allowedClockSkew;
    }
}
