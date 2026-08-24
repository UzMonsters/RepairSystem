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
        Duration mobileSessionTtl,
        Duration mobileSessionLastSeenUpdateInterval,
        BootstrapAdmin bootstrapAdmin
) {

    @ConstructorBinding
    public AppProperties {
        if (jwt == null) {
            jwt = new Jwt(
                    "",
                    "repair-auto",
                    Duration.ofMinutes(15),
                    "repair-auto-mobile");
        }
        if (mobileRefreshTokenTtl == null) {
            mobileRefreshTokenTtl = Duration.ofDays(30);
        }
        if (mobileSessionTtl == null) {
            mobileSessionTtl = Duration.ofDays(30);
        }
        if (mobileSessionLastSeenUpdateInterval == null) {
            mobileSessionLastSeenUpdateInterval = Duration.ofMinutes(5);
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

    public AppProperties(
            Cors cors,
            Trace trace,
            Jwt jwt,
            Duration refreshTokenTtl,
            Duration rememberMeRefreshTokenTtl,
            Duration mobileRefreshTokenTtl,
            BootstrapAdmin bootstrapAdmin) {
        this(
                cors,
                trace,
                jwt,
                refreshTokenTtl,
                rememberMeRefreshTokenTtl,
                mobileRefreshTokenTtl,
                Duration.ofDays(30),
                Duration.ofMinutes(5),
                bootstrapAdmin);
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
            Duration accessTokenTtl,
            String mobileAudience
    ) {
        public Jwt {
            if (mobileAudience == null || mobileAudience.isBlank()) {
                mobileAudience = "repair-auto-mobile";
            }
        }

        public Jwt(String secret, String issuer, Duration accessTokenTtl) {
            this(secret, issuer, accessTokenTtl, "repair-auto-mobile");
        }
    }

    public record BootstrapAdmin(
            boolean enabled,
            String email,
            String password,
            String fullName
    ) {
    }
}
