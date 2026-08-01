package com.example.darks.repair_auto.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Trace trace,
        Jwt jwt,
        Duration refreshTokenTtl,
        BootstrapAdmin bootstrapAdmin
) {

    public record Cors(
            List<String> allowedOrigins,
            List<String> allowedMethods,
            List<String> allowedHeaders,
            List<String> exposedHeaders,
            boolean allowCredentials
    ) {
    }

    public record Trace(
            String headerName
    ) {
    }

    public record Jwt(
            String secret,
            String issuer,
            Duration accessTokenTtl
    ) {
    }

    public record BootstrapAdmin(
            boolean enabled,
            String email,
            String password,
            String fullName
    ) {
    }
}
