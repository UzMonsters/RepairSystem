package com.example.darks.repair_auto.identity.application;

import com.example.darks.repair_auto.identity.api.dto.ResetPasswordRequest;
import com.example.darks.repair_auto.identity.api.dto.UserCreateRequest;
import com.example.darks.repair_auto.identity.api.dto.UserDetailsResponse;
import com.example.darks.repair_auto.identity.api.dto.UserMapper;
import com.example.darks.repair_auto.identity.api.dto.UserSummaryResponse;
import com.example.darks.repair_auto.identity.api.dto.UserUpdateRequest;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.pagination.PageResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserManagementService.class);

    private final UserRepository userRepository;
    private final EmailNormalizer emailNormalizer;
    private final PasswordPolicy passwordPolicy;
    private final PasswordService passwordService;
    private final RefreshSessionService refreshSessionService;

    public UserManagementService(
            UserRepository userRepository,
            EmailNormalizer emailNormalizer,
            PasswordPolicy passwordPolicy,
            PasswordService passwordService,
            RefreshSessionService refreshSessionService) {
        this.userRepository = userRepository;
        this.emailNormalizer = emailNormalizer;
        this.passwordPolicy = passwordPolicy;
        this.passwordService = passwordService;
        this.refreshSessionService = refreshSessionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> list(String search, UserRole role, Boolean active, Pageable pageable) {
        return PageResponse.from(userRepository.findAll(filters(blankToNull(search), role, active), pageable)
                .map(UserMapper::summary));
    }

    @Transactional(readOnly = true)
    public UserDetailsResponse get(Long id) {
        return UserMapper.details(find(id));
    }

    @Transactional
    public UserDetailsResponse create(UserCreateRequest request) {
        String email = emailNormalizer.normalize(request.email());
        passwordPolicy.validate(request.password(), email);
        User user = new User(
                request.fullName().trim(),
                email,
                passwordService.hash(request.password()),
                request.role(),
                request.active() == null || request.active(),
                now());
        try {
            User saved = userRepository.saveAndFlush(user);
            LOGGER.info("User management event operation=user_created result=success userId={}", saved.getId());
            return UserMapper.details(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }
    }

    @Transactional
    public UserDetailsResponse update(Long id, UserUpdateRequest request) {
        User user = userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> notFound());
        String email = emailNormalizer.normalize(request.email());
        user.setFullName(request.fullName().trim(), now());
        user.setEmail(email, now());
        if (request.phone() != null) {
            user.setPhone(request.phone().isBlank() ? null : request.phone().trim(), now());
        }
        try {
            return UserMapper.details(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
        }
    }

    @Transactional
    public UserDetailsResponse changeRole(Long id, UserRole role) {
        lockActiveAdmins();
        User user = userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> notFound());
        if (user.getRole() == UserRole.ADMIN && role != UserRole.ADMIN && user.isActive()
                && userRepository.countActiveAdmins() <= 1) {
            throw new BusinessException(ErrorCode.LAST_ACTIVE_ADMIN_REQUIRED);
        }
        boolean changed = user.getRole() != role;
        user.setRole(role, now());
        if (changed) {
            refreshSessionService.revokeAllForUser(id, "ROLE_CHANGED");
            userRepository.incrementAuthVersion(id, now());
        }
        LOGGER.info("User management event operation=user_role_changed result=success userId={}", id);
        return UserMapper.details(user);
    }

    @Transactional
    public UserDetailsResponse changeActivation(Long id, boolean active, Long currentUserId) {
        lockActiveAdmins();
        User user = userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> notFound());
        if (!active && id.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.SELF_DISABLE_NOT_ALLOWED);
        }
        if (!active && user.getRole() == UserRole.ADMIN && user.isActive() && userRepository.countActiveAdmins() <= 1) {
            throw new BusinessException(ErrorCode.LAST_ACTIVE_ADMIN_REQUIRED);
        }
        boolean deactivated = user.isActive() && !active;
        user.setActive(active, now());
        if (deactivated) {
            refreshSessionService.revokeAllForUser(id, "USER_DISABLED");
            userRepository.incrementAuthVersion(id, now());
        }
        LOGGER.info("User management event operation=user_activation_changed result=success userId={}", id);
        return UserMapper.details(user);
    }

    @Transactional
    public void revokeSessions(Long id) {
        userRepository.findByIdForUpdate(id).orElseThrow(() -> notFound());
        refreshSessionService.revokeAllForUser(id, "ADMIN_REVOKED");
        userRepository.incrementAuthVersion(id, now());
    }

    @Transactional
    public void resetPassword(Long targetUserId, ResetPasswordRequest request, Long actorUserId) {
        if (request.newPassword() == null || !request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
        User user = userRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(this::notFound);
        if (passwordService.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
        }
        passwordPolicy.validate(request.newPassword(), user.getEmail());
        user.changePassword(passwordService.hash(request.newPassword()), now());
        refreshSessionService.revokeAllForUser(targetUserId, "ADMIN_RESET");
        userRepository.incrementAuthVersion(targetUserId, now());
        LOGGER.info("User management event operation=admin_password_reset result=success actorUserId={} targetUserId={}", actorUserId, targetUserId);
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(this::notFound);
    }

    private void lockActiveAdmins() {
        userRepository.findActiveAdminsForUpdate();
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Specification<User> filters(String search, UserRole role, Boolean active) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            if (search != null) {
                String pattern = "%" + search.toLowerCase(java.util.Locale.ROOT) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("fullName")), pattern),
                        builder.like(builder.lower(root.get("email")), pattern)));
            }
            if (role != null) {
                predicate = builder.and(predicate, builder.equal(root.get("role"), role));
            }
            if (active != null) {
                predicate = builder.and(predicate, builder.equal(root.get("active"), active));
            }
            return predicate;
        };
    }
}
