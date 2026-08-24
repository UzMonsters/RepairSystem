package com.example.darks.repair_auto.identity.mobile.otp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    private boolean enabled = false;
    private SmsProviderType provider = SmsProviderType.LOGGING;

    @NestedConfigurationProperty
    private EskizProperties eskiz = new EskizProperties();

    public static SmsProperties of(boolean enabled, SmsProviderType provider) {
        SmsProperties props = new SmsProperties();
        props.setEnabled(enabled);
        props.setProvider(provider);
        return props;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public SmsProviderType getProvider() {
        return provider;
    }

    public void setProvider(SmsProviderType provider) {
        this.provider = provider == null ? SmsProviderType.LOGGING : provider;
    }

    public EskizProperties getEskiz() {
        return eskiz;
    }

    public void setEskiz(EskizProperties eskiz) {
        this.eskiz = eskiz == null ? new EskizProperties() : eskiz;
    }
}
