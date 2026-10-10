package com.opsflow.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * A fixed-window counter kept in Redis, so the limit holds across every instance of the
 * application and not per instance.
 *
 * The increment and the expiry run in one Lua script. Done as two separate commands, a crash
 * between them would leave a counter that never expires and the client would be locked out
 * for good.
 *
 * If Redis can't be reached the request is let through: a rate limiter that is down should
 * not take the API down with it.
 */
@Component
@ConditionalOnProperty(name = "opsflow.rate-limit.enabled", havingValue = "true")
public class RedisRateLimiter implements RateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(RedisRateLimiter.class);

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final DefaultRedisScript<List> COUNT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return {count, redis.call('PTTL', KEYS[1])}
            """, List.class);

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Decision check(String key, int limit, Duration window) {
        try {
            List<?> result = redis.execute(COUNT, List.of("ratelimit:" + key), String.valueOf(window.toMillis()));
            long count = ((Number) result.get(0)).longValue();
            long millisLeft = Math.max(0, ((Number) result.get(1)).longValue());
            return new Decision(count <= limit, Math.max(0, limit - count), (millisLeft + 999) / 1000);
        } catch (RuntimeException e) {
            logger.warn("Rate limiter can't reach Redis, letting the request through: {}", e.getMessage());
            return new Decision(true, limit, 0);
        }
    }
}
