package com.opsflow.ratelimit;

import java.time.Duration;

public interface RateLimiter {

    /** Counts one request against the key and says whether it is still within the limit. */
    Decision check(String key, int limit, Duration window);

    record Decision(boolean allowed, long remaining, long retryAfterSeconds) {
    }
}
