package com.example.darks.repair_auto.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.identity.infrastructure.persistence.RefreshSessionRepository;
import com.example.darks.repair_auto.shared.config.AppProperties;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.repair.request.infrastructure.RepairRequestRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class BootstrapAdminConcurrencyIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private RepairRequestRepository repairRequestRepository;

    @Autowired
    private EmailNormalizer emailNormalizer;

    @Autowired
    private PasswordPolicy passwordPolicy;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        repairRequestRepository.deleteAll();
        refreshSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void givenTwoConcurrentBootstrapExecutionsWhenNoAdminExistsThenExactlyOneAdminIsCreated() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Callable<Object> bootstrap = () -> {
            start.await(5, TimeUnit.SECONDS);
            try {
                transactionTemplate.executeWithoutResult(status -> runner().run(null));
                return "completed";
            } catch (RuntimeException exception) {
                return exception;
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(bootstrap);
            var second = executor.submit(bootstrap);
            start.countDown();
            List<Object> results = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));

            assertThat(results).containsExactly("completed", "completed");
            assertThat(userRepository.findAll())
                    .filteredOn(user -> user.getRole() == UserRole.ADMIN)
                    .hasSize(1)
                    .allMatch(User::isActive);
        }
    }

    @Test
    void givenExistingManagerWhenBootstrapRunsThenAdministratorIsStillCreated() {
        userRepository.saveAndFlush(new User(
                "Manager",
                "manager@example.com",
                passwordService.hash("ManagerPass123!"),
                UserRole.MANAGER,
                true,
                OffsetDateTime.now(ZoneOffset.UTC)));

        transactionTemplate.executeWithoutResult(status -> runner().run(null));

        assertThat(userRepository.findAll())
                .filteredOn(user -> user.getRole() == UserRole.ADMIN)
                .hasSize(1);
    }

    private BootstrapAdminRunner runner() {
        return new BootstrapAdminRunner(
                properties(),
                userRepository,
                emailNormalizer,
                passwordPolicy,
                passwordService,
                jdbcTemplate);
    }

    private AppProperties properties() {
        return new AppProperties(
                new AppProperties.Cors(List.of(), List.of(), List.of(), List.of(), false),
                new AppProperties.Trace("X-Trace-Id"),
                new AppProperties.Jwt(
                        "test-local-only-jwt-secret-that-is-long-enough",
                        "repair-auto",
                        Duration.ofMinutes(15)),
                Duration.ofDays(1),
                Duration.ofDays(30),
                new AppProperties.BootstrapAdmin(
                        true,
                        "bootstrap@example.com",
                        "BootstrapPass123!",
                        "Bootstrap Admin"));
    }
}
