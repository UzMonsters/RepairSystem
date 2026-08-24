package com.example.darks.repair_auto.identity.mobile.otp;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sms.eskiz")
public class EskizProperties {

    private String baseUrl = "https://notify.eskiz.uz";
    private String email;
    private String password;
    private String fromName = "4546";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(10);

    public static EskizProperties of(
            String baseUrl,
            String email,
            String password,
            String fromName,
            Duration connectTimeout,
            Duration readTimeout) {
        EskizProperties props = new EskizProperties();
        props.setBaseUrl(baseUrl);
        props.setEmail(email);
        props.setPassword(password);
        props.setFromName(fromName);
        props.setConnectTimeout(connectTimeout);
        props.setReadTimeout(readTimeout);
        return props;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://notify.eskiz.uz" : baseUrl.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName == null || fromName.isBlank() ? "4546" : fromName.trim();
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout == null ? Duration.ofSeconds(10) : readTimeout;
    }
}
