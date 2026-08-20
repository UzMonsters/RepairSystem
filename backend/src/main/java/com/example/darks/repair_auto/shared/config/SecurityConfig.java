package com.example.darks.repair_auto.shared.config;

import com.example.darks.repair_auto.shared.error.SecurityErrorHandler;
import com.example.darks.repair_auto.identity.application.EmailNormalizer;
import com.example.darks.repair_auto.identity.infrastructure.security.AuthenticatedUser;
import com.example.darks.repair_auto.identity.infrastructure.security.JwtAuthenticationFilter;
import com.example.darks.repair_auto.identity.infrastructure.persistence.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityErrorHandler securityErrorHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new RequestAttributeSecurityContextRepository()))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .cacheControl(Customizer.withDefaults())
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/ws",
                                "/ws/**")
                        .permitAll()
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/mobile/auth/telegram/customer",
                                "/api/v1/mobile/auth/telegram/technician",
                                "/api/v1/mobile/auth/refresh",
                                "/api/v1/mobile/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/mobile/auth/logout-all")
                        .hasAnyRole("CUSTOMER", "TECHNICIAN")
                        .requestMatchers(
                                "/api/v1/mobile/me/repair-requests",
                                "/api/v1/mobile/me/repair-requests/**")
                        .hasRole("CUSTOMER")
                        .requestMatchers(
                                "/api/v1/mobile/me/jobs",
                                "/api/v1/mobile/me/jobs/**",
                                "/api/v1/mobile/me/schedule")
                        .hasRole("TECHNICIAN")
                        .requestMatchers(
                                "/api/v1/mobile/me/attachments/**")
                        .hasAnyRole("CUSTOMER", "TECHNICIAN")
                        .requestMatchers(
                                "/api/v1/mobile/me/conversations",
                                "/api/v1/mobile/me/conversations/**")
                        .hasAnyRole("CUSTOMER", "TECHNICIAN")
                        .requestMatchers(
                                "/api/v1/mobile/me",
                                "/api/v1/mobile/me/**")
                        .hasAnyRole("CUSTOMER", "TECHNICIAN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/telegram/webhook",
                                "/api/v1/telegram/webhook/customer",
                                "/api/v1/telegram/webhook/technician")
                        .permitAll()
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/notification-deliveries/*/retry").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/notification-deliveries/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/notifications/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/reviews/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/dashboard/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/requests/**", "/api/v1/repair-requests/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/conversations/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/attachments/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/customers/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/technicians/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/categories/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/settings/me").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/settings/system").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/settings/system").hasRole("ADMIN")
                        .requestMatchers("/api/v1/auth/me", "/api/v1/auth/password", "/api/v1/auth/change-password", "/api/v1/auth/logout-all", "/api/v1/me/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .anyRequest().denyAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository userRepository, EmailNormalizer emailNormalizer) {
        return username -> userRepository.findByEmail(emailNormalizer.normalize(username))
                .filter(user -> user.isActive())
                .map(AuthenticatedUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("User was not found."));
    }
}
