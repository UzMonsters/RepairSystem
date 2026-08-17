package com.example.darks.repair_auto.identity.application;

import com.example.darks.repair_auto.identity.api.dto.LoginResponse;
import com.example.darks.repair_auto.identity.api.dto.TokenResponse;
import com.example.darks.repair_auto.identity.api.dto.UserMapper;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.BusinessRuleException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOhiXa/8YVSGg8sE7T/xOeJxZL9gRgHwS";

    private final UserRepository userRepository;
    private final EmailNormalizer emailNormalizer;
    private final PasswordService passwordService;
    private final PasswordPolicy passwordPolicy;
    private final JwtTokenService jwtTokenService;
    private final RefreshSessionService refreshSessionService;

    public AuthenticationService(
            UserRepository userRepository,
            EmailNormalizer emailNormalizer,
            PasswordService passwordService,
            PasswordPolicy passwordPolicy,
            JwtTokenService jwtTokenService,
            RefreshSessionService refreshSessionService) {
        this.userRepository = userRepository;
        this.emailNormalizer = emailNormalizer;
        this.passwordService = passwordService;
        this.passwordPolicy = passwordPolicy;
        this.jwtTokenService = jwtTokenService;
        this.refreshSessionService = refreshSessionService;
    }

    @Transactional
    public LoginResponse login(String email, String password, boolean rememberMe, String ip, String userAgent) {
        String normalizedEmail = emailNormalizer.normalize(email);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElse(null);
        if (user == null) {
            passwordService.matches(password, DUMMY_PASSWORD_HASH);
            throw invalidCredentials();
        }
        if (!passwordService.matches(password, user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (!user.isActive()) {
            throw invalidCredentials();
        }
        user.markLoggedIn(now());
        RefreshSessionService.IssuedRefreshToken refresh = refreshSessionService.create(user, rememberMe, ip, userAgent);
        LOGGER.info("Authentication event operation=login result=success userId={} rememberMe={}", user.getId(), rememberMe);
        return new LoginResponse(
                jwtTokenService.issue(user),
                refresh.rawToken(),
                "Bearer",
                jwtTokenService.accessTokenTtlSeconds(),
                refreshSessionService.remainingTtlSeconds(refresh.session()),
                refresh.session().isRememberMe(),
                UserMapper.summary(user));
    }

    @Transactional
    public LoginResponse login(String email, String password, String ip, String userAgent) {
        return login(email, password, false, ip, userAgent);
    }

    @Transactional(noRollbackFor = {BusinessRuleException.class, BusinessException.class})
    public TokenResponse refresh(String refreshToken, String ip, String userAgent) {
        RefreshSessionService.RotationResult rotation = refreshSessionService.rotate(refreshToken, ip, userAgent);
        LOGGER.info("Authentication event operation=refresh result=success userId={}", rotation.user().getId());
        return new TokenResponse(
                jwtTokenService.issue(rotation.user()),
                rotation.rawToken(),
                "Bearer",
                jwtTokenService.accessTokenTtlSeconds(),
                refreshSessionService.remainingTtlSeconds(rotation.session()),
                rotation.session().isRememberMe());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshSessionService.revokeByRawToken(refreshToken, "LOGOUT");
    }

    @Transactional
    public void logoutAll(Long userId) {
        refreshSessionService.revokeAllForUser(userId, "LOGOUT_ALL");
        userRepository.incrementAuthVersion(userId, now());
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword) {
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (!passwordService.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        if (passwordService.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
        }
        passwordPolicy.validate(newPassword, user.getEmail());
        user.changePassword(passwordService.hash(newPassword), now());
        refreshSessionService.revokeAllForUser(userId, "PASSWORD_CHANGED");
        userRepository.incrementAuthVersion(userId, now());
        LOGGER.info("Authentication event operation=password_changed result=success userId={}", userId);
    }

    private BusinessException invalidCredentials() {
        LOGGER.info("Authentication event operation=login result=failure reason=INVALID_CREDENTIALS");
        return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
