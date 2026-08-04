package com.example.darks.repair_auto.notification.infrastructure.worker;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    private boolean workerEnabled = true;
    private Duration pollInterval = Duration.ofSeconds(5);
    private int batchSize = 50;
    private Duration processingLease = Duration.ofMinutes(2);
    private int maxAttempts = 8;
    private Duration initialBackoff = Duration.ofSeconds(10);
    private Duration maxBackoff = Duration.ofMinutes(30);

    public boolean isWorkerEnabled() {
        return workerEnabled;
    }

    public void setWorkerEnabled(boolean workerEnabled) {
        this.workerEnabled = workerEnabled;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = Math.max(1, batchSize);
    }

    public Duration getProcessingLease() {
        return processingLease;
    }

    public void setProcessingLease(Duration processingLease) {
        this.processingLease = processingLease;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public void setInitialBackoff(Duration initialBackoff) {
        this.initialBackoff = initialBackoff;
    }

    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    public void setMaxBackoff(Duration maxBackoff) {
        this.maxBackoff = maxBackoff;
    }
}
