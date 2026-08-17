package com.example.darks.repair_auto.identity.infrastructure.security;

import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.shared.error.SecurityErrorHandler;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final SecurityErrorHandler securityErrorHandler;

    public JwtAuthenticationFilter(
            JwtTokenService jwtTokenService,
            UserRepository userRepository,
            SecurityErrorHandler securityErrorHandler) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
        this.securityErrorHandler = securityErrorHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            authenticate(request);
            filterChain.doFilter(request, response);
        } catch (BusinessException exception) {
            SecurityContextHolder.clearContext();
            securityErrorHandler.writeUnauthorized(request, response, exception.code(), exception.getErrorCode().getMessageKey());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticate(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            return;
        }
        if (!authorization.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        String token = authorization.substring("Bearer ".length());
        if (token.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        JwtTokenService.ValidatedAccessToken validated = jwtTokenService.validate(token);
        Optional<User> user = userRepository.findById(validated.userId());
        if (user.isEmpty() || !user.get().isActive()) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        if (validated.authVersion() != user.get().getAuthVersion()) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        AuthenticatedUser principal = new AuthenticatedUser(user.get());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
