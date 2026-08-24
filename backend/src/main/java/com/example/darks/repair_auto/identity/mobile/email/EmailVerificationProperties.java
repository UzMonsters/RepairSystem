package com.example.darks.repair_auto.identity.mobile.email;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email-verification")
public class EmailVerificationProperties {

    private boolean enabled = false;
    private Duration ttl = Duration.ofMinutes(10);
    private Duration resendCooldown = Duration.ofSeconds(60);
    private int maxAttempts = 5;
    private String fromAddress = "no-reply@example.com";
    private String fromName = "RepairAuto";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl == null ? Duration.ofMinutes(10) : ttl;
    }

    public Duration getResendCooldown() {
        return resendCooldown;
    }

    public void setResendCooldown(Duration resendCooldown) {
        this.resendCooldown = resendCooldown == null ? Duration.ofSeconds(60) : resendCooldown;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts <= 0 ? 5 : maxAttempts;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }
}
