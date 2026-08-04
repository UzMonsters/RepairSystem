package com.example.darks.repair_auto.repair.attachment.infrastructure.storage;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        boolean enabled,
        URI endpoint,
        @NotBlank String region,
        @NotBlank String bucket,
        String accessKey,
        String secretKey,
        boolean pathStyle,
        boolean createBucket,
        @NotNull Duration downloadUrlTtl,
        @NotNull DataSize maxFileSize,
        @Min(1) @Max(1000) int maxFilesPerRequest,
        @Min(1) @Max(1000) int maxFilesPerType
) {

    private static final Duration MIN_DOWNLOAD_TTL = Duration.ofSeconds(30);
    private static final Duration MAX_DOWNLOAD_TTL = Duration.ofHours(1);

    public StorageProperties {
        if (downloadUrlTtl == null
                || downloadUrlTtl.compareTo(MIN_DOWNLOAD_TTL) < 0
                || downloadUrlTtl.compareTo(MAX_DOWNLOAD_TTL) > 0) {
            throw new IllegalArgumentException("Storage download URL TTL must be between PT30S and PT1H.");
        }
        if (enabled && (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank())) {
            throw new IllegalArgumentException("Storage access key and secret key are required when storage is enabled.");
        }
        if (maxFileSize == null || maxFileSize.toBytes() < 1) {
            throw new IllegalArgumentException("Storage max file size must be positive.");
        }
    }
}
