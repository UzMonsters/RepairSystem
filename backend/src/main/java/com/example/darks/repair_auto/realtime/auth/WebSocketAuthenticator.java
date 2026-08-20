package com.example.darks.repair_auto.realtime.auth;

import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.shared.error.BusinessException;
import com.example.darks.repair_auto.shared.error.ErrorCode;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthenticator {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final TechnicianRepository technicianRepository;

    public WebSocketAuthenticator(
            JwtTokenService jwtTokenService,
            UserRepository userRepository,
            CustomerRepository customerRepository,
            TechnicianRepository technicianRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.technicianRepository = technicianRepository;
    }

    public Authentication authenticate(String rawHeader) {
        if (rawHeader == null || rawHeader.isBlank()) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        String token = rawHeader.trim();
        if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
            token = token.substring(7).trim();
        }

        if (token.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        JwtTokenService.ValidatedAccessToken validated = jwtTokenService.validate(token);

        switch (validated.actorType()) {
            case STAFF -> {
                Optional<User> user = userRepository.findById(validated.userId());
                if (user.isEmpty() || !user.get().isActive()) {
                    throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
                }
                if (validated.authVersion() == null || !validated.authVersion().equals(user.get().getAuthVersion())) {
                    throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
                }
                AuthenticatedUser principal = new AuthenticatedUser(user.get());
                return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
            }
            case CUSTOMER -> {
                Optional<Customer> customer = customerRepository.findById(validated.actorId());
                if (customer.isEmpty() || !customer.get().isActive()) {
                    throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
                }
                AuthenticatedMobileActor principal = new AuthenticatedMobileActor(
                        ActorType.CUSTOMER,
                        customer.get().getId(),
                        customer.get().getPhone(),
                        customer.get().isActive());
                return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
            }
            case TECHNICIAN -> {
                Optional<Technician> technician = technicianRepository.findById(validated.actorId());
                if (technician.isEmpty() || !technician.get().isActive()) {
                    throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
                }
                AuthenticatedMobileActor principal = new AuthenticatedMobileActor(
                        ActorType.TECHNICIAN,
                        technician.get().getId(),
                        technician.get().getPhone(),
                        technician.get().isActive());
                return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
            }
            default -> throw new BusinessException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }
}
