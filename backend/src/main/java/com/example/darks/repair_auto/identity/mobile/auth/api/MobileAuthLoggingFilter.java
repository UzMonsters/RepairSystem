package com.example.darks.repair_auto.identity.mobile.auth.api;

import static com.example.darks.repair_auto.identity.mobile.auth.MobileAuthLogSupport.clientIp;
import static com.example.darks.repair_auto.identity.mobile.auth.MobileAuthLogSupport.safeUserAgent;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class MobileAuthLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileAuthLoggingFilter.class);
    private static final String MOBILE_AUTH_PATH = "/api/v1/mobile/auth";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith(MOBILE_AUTH_PATH);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        LOGGER.info(
                "Mobile auth request received method={} path={} ip={} userAgent={}",
                request.getMethod(),
                request.getRequestURI(),
                clientIp(request),
                safeUserAgent(request));
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            LOGGER.info(
                    "Mobile auth request completed method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);
        }
    }
}
