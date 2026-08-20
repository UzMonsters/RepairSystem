package com.example.darks.repair_auto.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.darks.repair_auto.PostgreSqlIntegrationTest;
import com.example.darks.repair_auto.customer.domain.Customer;
import com.example.darks.repair_auto.customer.infrastructure.CustomerRepository;
import com.example.darks.repair_auto.identity.application.PasswordService;
import com.example.darks.repair_auto.identity.domain.ActorType;
import com.example.darks.repair_auto.identity.domain.User;
import com.example.darks.repair_auto.identity.domain.UserRole;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedMobileActor;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtTokenService;
import com.example.darks.repair_auto.realtime.auth.StompAuthChannelInterceptor;
import com.example.darks.repair_auto.realtime.auth.WebSocketAuthenticator;
import com.example.darks.repair_auto.technician.domain.Technician;
import com.example.darks.repair_auto.technician.infrastructure.TechnicianRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

@SpringBootTest
class WebSocketAuthIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private WebSocketAuthenticator webSocketAuthenticator;

    @Autowired
    private StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private PasswordService passwordService;

    private User adminUser;
    private Customer customer;
    private Technician technician;

    @BeforeEach
    void setUp() {
        OffsetDateTime now = OffsetDateTime.now(Clock.systemUTC()).withOffsetSameInstant(ZoneOffset.UTC);

        adminUser = userRepository.findByEmail("ws_admin@test.com").orElseGet(() ->
                userRepository.save(new User(
                        "WS Admin",
                        "ws_admin@test.com",
                        passwordService.hash("Password123!"),
                        UserRole.ADMIN,
                        true,
                        now)));

        customer = customerRepository.findByPhone("+998901234567").orElseGet(() ->
                customerRepository.save(new Customer(
                        "WS Customer",
                        "+998901234567",
                        com.example.darks.repair_auto.shared.i18n.LanguageCode.UZ,
                        now)));

        technician = technicianRepository.findByPhone("+998909876543").orElseGet(() ->
                technicianRepository.save(new Technician(
                        "WS Technician",
                        "+998909876543",
                        null,
                        null,
                        null,
                        com.example.darks.repair_auto.shared.i18n.LanguageCode.UZ,
                        true,
                        now)));
    }

    @Test
    void authenticateToken_withValidStaffToken_authenticatesSuccessfully() {
        String token = jwtTokenService.issue(adminUser);

        Authentication auth = webSocketAuthenticator.authenticate(token);

        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);
        AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
        assertThat(user.id()).isEqualTo(adminUser.getId());
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    void authenticateToken_withValidCustomerToken_authenticatesSuccessfully() {
        String token = jwtTokenService.issueMobile(ActorType.CUSTOMER, customer.getId());

        Authentication auth = webSocketAuthenticator.authenticate(token);

        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedMobileActor.class);
        AuthenticatedMobileActor actor = (AuthenticatedMobileActor) auth.getPrincipal();
        assertThat(actor.actorType()).isEqualTo(ActorType.CUSTOMER);
        assertThat(actor.actorId()).isEqualTo(customer.getId());
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
    }

    @Test
    void authenticateToken_withValidTechnicianToken_authenticatesSuccessfully() {
        String token = jwtTokenService.issueMobile(ActorType.TECHNICIAN, technician.getId());

        Authentication auth = webSocketAuthenticator.authenticate(token);

        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedMobileActor.class);
        AuthenticatedMobileActor actor = (AuthenticatedMobileActor) auth.getPrincipal();
        assertThat(actor.actorType()).isEqualTo(ActorType.TECHNICIAN);
        assertThat(actor.actorId()).isEqualTo(technician.getId());
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIAN"));
    }

    @Test
    void authenticateToken_withInvalidToken_throwsException() {
        assertThatThrownBy(() -> webSocketAuthenticator.authenticate("invalid.token.here"))
                .isInstanceOf(com.example.darks.repair_auto.shared.error.BusinessException.class);
    }

    @Test
    void interceptConnect_withValidToken_setsUserPrincipal() {
        String token = jwtTokenService.issue(adminUser);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = stompAuthChannelInterceptor.preSend(message, (MessageChannel) null);

        assertThat(result).isNotNull();
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getUser().getName()).isEqualTo(adminUser.getEmail());
    }

    @Test
    void interceptSubscribe_customerAccessingStaffEventsTopic_isRejected() {
        String token = jwtTokenService.issueMobile(ActorType.CUSTOMER, customer.getId());
        Authentication auth = webSocketAuthenticator.authenticate(token);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/staff.events");
        accessor.setUser(auth);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> stompAuthChannelInterceptor.preSend(message, (MessageChannel) null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void interceptSubscribe_staffAccessingStaffEventsTopic_isAllowed() {
        String token = jwtTokenService.issue(adminUser);
        Authentication auth = webSocketAuthenticator.authenticate(token);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/staff.events");
        accessor.setUser(auth);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = stompAuthChannelInterceptor.preSend(message, (MessageChannel) null);

        assertThat(result).isNotNull();
    }
}
