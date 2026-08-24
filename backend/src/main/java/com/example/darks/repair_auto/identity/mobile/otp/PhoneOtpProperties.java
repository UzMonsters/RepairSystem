package com.example.darks.repair_auto.identity.mobile.otp;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.phone-otp")
public class PhoneOtpProperties {

    private boolean smsEnabled = false;
    private Duration ttl = Duration.ofMinutes(5);
    private Duration resendCooldown = Duration.ofSeconds(60);
    private int maxAttempts = 5;
    private boolean exposeCodeInResponse = false;

    public static PhoneOtpProperties of(
            boolean smsEnabled,
            Duration ttl,
            Duration resendCooldown,
            int maxAttempts,
            boolean exposeCodeInResponse) {
        PhoneOtpProperties props = new PhoneOtpProperties();
        props.setSmsEnabled(smsEnabled);
        props.setTtl(ttl);
        props.setResendCooldown(resendCooldown);
        props.setMaxAttempts(maxAttempts);
        props.setExposeCodeInResponse(exposeCodeInResponse);
        return props;
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl == null ? Duration.ofMinutes(5) : ttl;
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

    public boolean isExposeCodeInResponse() {
        return exposeCodeInResponse;
    }

    public void setExposeCodeInResponse(boolean exposeCodeInResponse) {
        this.exposeCodeInResponse = exposeCodeInResponse;
    }
}
