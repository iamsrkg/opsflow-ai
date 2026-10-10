package com.opsflow.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RateLimitFilterTest {

    /** Counts in a map, where the real one counts in Redis. */
    private static class CountingLimiter implements RateLimiter {
        private final Map<String, Integer> counts = new HashMap<>();

        @Override
        public Decision check(String key, int limit, Duration window) {
            int count = counts.merge(key, 1, Integer::sum);
            return new Decision(count <= limit, Math.max(0, limit - count), 42);
        }
    }

    private final RateLimitFilter filter = new RateLimitFilter(new CountingLimiter(), 2, 5);

    private MockHttpServletResponse call(String path, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void loginIsCutOffAfterItsOwnLowerLimit() throws Exception {
        assertEquals(200, call("/api/v1/auth/login", "10.0.0.1").getStatus());
        assertEquals(200, call("/api/v1/auth/login", "10.0.0.1").getStatus());

        MockHttpServletResponse third = call("/api/v1/auth/login", "10.0.0.1");
        assertEquals(429, third.getStatus());
        assertEquals("42", third.getHeader("Retry-After"));
        assertEquals("0", third.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    void oneClientRunningOutDoesNotAffectAnother() throws Exception {
        call("/api/v1/auth/login", "10.0.0.1");
        call("/api/v1/auth/login", "10.0.0.1");
        assertEquals(429, call("/api/v1/auth/login", "10.0.0.1").getStatus());

        assertEquals(200, call("/api/v1/auth/login", "10.0.0.2").getStatus());
    }

    @Test
    void theRestOfTheApiHasItsOwnBudget() throws Exception {
        call("/api/v1/auth/login", "10.0.0.1");
        call("/api/v1/auth/login", "10.0.0.1");
        call("/api/v1/auth/login", "10.0.0.1");

        MockHttpServletResponse other = call("/api/v1/requests", "10.0.0.1");
        assertEquals(200, other.getStatus());
        assertNotNull(other.getHeader("X-RateLimit-Limit"));
    }

    @Test
    void pathsOutsideTheApiAreNotCounted() throws Exception {
        MockHttpServletResponse health = call("/actuator/health", "10.0.0.1");
        assertEquals(200, health.getStatus());
        assertNull(health.getHeader("X-RateLimit-Limit"));
    }
}
