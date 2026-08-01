package com.example.darks.repair_auto.auth.service;

import com.example.darks.repair_auto.config.AppProperties;
import com.example.darks.repair_auto.user.domain.User;
import com.example.darks.repair_auto.user.domain.UserRepository;
import com.example.darks.repair_auto.user.domain.UserRole;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final EmailNormalizer emailNormalizer;
    private final PasswordPolicy passwordPolicy;
    private final PasswordService passwordService;

    public BootstrapAdminRunner(
            AppProperties properties,
            UserRepository userRepository,
            EmailNormalizer emailNormalizer,
            PasswordPolicy passwordPolicy,
            PasswordService passwordService) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.emailNormalizer = emailNormalizer;
        this.passwordPolicy = passwordPolicy;
        this.passwordService = passwordService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.BootstrapAdmin bootstrap = properties.bootstrapAdmin();
        if (!bootstrap.enabled() || userRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }
        String email = emailNormalizer.normalize(bootstrap.email());
        if (email == null || email.isBlank() || bootstrap.password() == null || bootstrap.password().isBlank()) {
            throw new IllegalStateException(
                    "Bootstrap admin is enabled but APP_BOOTSTRAP_ADMIN_EMAIL and APP_BOOTSTRAP_ADMIN_PASSWORD are missing.");
        }
        passwordPolicy.validate(bootstrap.password(), email);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        User admin = new User(
                bootstrap.fullName() == null || bootstrap.fullName().isBlank()
                        ? "System Administrator" : bootstrap.fullName().trim(),
                email,
                passwordService.hash(bootstrap.password()),
                UserRole.ADMIN,
                true,
                now);
        userRepository.save(admin);
        LOGGER.info("Bootstrap event operation=admin_created result=success email={}", email);
    }
}
