package com.example.darks.repair_auto.shared.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Trace trace,
        Jwt jwt,
        Duration refreshTokenTtl,
        Duration rememberMeRefreshTokenTtl,
        Duration mobileRefreshTokenTtl,
        BootstrapAdmin bootstrapAdmin
) {

    @ConstructorBinding
    public AppProperties {
        if (mobileRefreshTokenTtl == null) {
            mobileRefreshTokenTtl = Duration.ofDays(30);
        }
    }

    public AppProperties(
            Cors cors,
            Trace trace,
            Jwt jwt,
            Duration refreshTokenTtl,
            Duration rememberMeRefreshTokenTtl,
            BootstrapAdmin bootstrapAdmin) {
        this(cors, trace, jwt, refreshTokenTtl, rememberMeRefreshTokenTtl, Duration.ofDays(30), bootstrapAdmin);
    }

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
