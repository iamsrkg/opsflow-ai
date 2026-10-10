package com.opsflow.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Runs before authentication, so a flood of requests is turned away before it costs a
 * password hash or a database query. Login has its own, much lower limit because it is the
 * endpoint someone would hammer to guess passwords.
 */
@Component
@Order(2)   // right after the correlation id filter, so a 429 still carries the id
@ConditionalOnProperty(name = "opsflow.rate-limit.enabled", havingValue = "true")
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimiter rateLimiter;
    private final int loginPerMinute;
    private final int apiPerMinute;

    public RateLimitFilter(RateLimiter rateLimiter,
                           @Value("${opsflow.rate-limit.login-per-minute:10}") int loginPerMinute,
                           @Value("${opsflow.rate-limit.api-per-minute:300}") int apiPerMinute) {
        this.rateLimiter = rateLimiter;
        this.loginPerMinute = loginPerMinute;
        this.apiPerMinute = apiPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean login = request.getRequestURI().startsWith("/api/v1/auth/login");
        int limit = login ? loginPerMinute : apiPerMinute;
        String key = (login ? "login:" : "api:") + request.getRemoteAddr();

        RateLimiter.Decision decision = rateLimiter.check(key, limit, WINDOW);
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        if (!decision.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Try again in "
                    + decision.retryAfterSeconds() + " seconds.\",\"data\":null}");
            return;
        }
        chain.doFilter(request, response);
    }
}
