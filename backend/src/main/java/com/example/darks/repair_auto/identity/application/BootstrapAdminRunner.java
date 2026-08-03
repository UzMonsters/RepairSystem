package com.example.darks.repair_auto.identity.application;

import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.domain.UserRole;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapAdminRunner.class);
    static final long BOOTSTRAP_ADVISORY_LOCK_KEY = 834_645_201_180_001L;

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final EmailNormalizer emailNormalizer;
    private final PasswordPolicy passwordPolicy;
    private final PasswordService passwordService;
    private final JdbcTemplate jdbcTemplate;

    public BootstrapAdminRunner(
            AppProperties properties,
            UserRepository userRepository,
            EmailNormalizer emailNormalizer,
            PasswordPolicy passwordPolicy,
            PasswordService passwordService,
            JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.emailNormalizer = emailNormalizer;
        this.passwordPolicy = passwordPolicy;
        this.passwordService = passwordService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.BootstrapAdmin bootstrap = properties.bootstrapAdmin();
        if (!bootstrap.enabled()) {
            return;
        }
        jdbcTemplate.query(
                "select pg_advisory_xact_lock(?)",
                statement -> statement.setLong(1, BOOTSTRAP_ADVISORY_LOCK_KEY),
                resultSet -> null);
        if (userRepository.existsByRole(UserRole.ADMIN)) {
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
        try {
            userRepository.saveAndFlush(admin);
            LOGGER.info("Bootstrap event operation=admin_created result=success");
        } catch (DataIntegrityViolationException exception) {
            if (userRepository.existsByRole(UserRole.ADMIN)) {
                LOGGER.info("Bootstrap event operation=admin_created result=skipped reason=admin_exists_after_conflict");
                return;
            }
            throw exception;
        }
    }
}
