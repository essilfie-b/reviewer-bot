package com.amaliai.mcp.common;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-user token-bucket rate limiter for MCP tool invocations.
 * <p>
 * Each ARMS user gets a bucket of {@link #CAPACITY} tokens that refills at
 * {@link #REFILL_PER_SECOND} tokens per second. Every tool call consumes one
 * token; when the bucket is empty the call is rejected so a single user cannot
 * exhaust the shared Graph API quota.
 */
@Component
public class ToolRateLimiter {

    private static final int    CAPACITY          = 60;
    private static final double REFILL_PER_SECOND = 1.0;

    private final Map<Integer, Bucket> buckets = new HashMap<>();

    /**
     * Attempts to consume a single token for the given user.
     *
     * @return {@code true} if the call is allowed, {@code false} if the user is rate-limited
     */
    public boolean tryAcquire(int armsUserId) {
        Bucket bucket = buckets.get(armsUserId);
        if (bucket == null) {
            bucket = new Bucket(CAPACITY, System.currentTimeMillis());
            buckets.put(armsUserId, bucket);
        }

        refill(bucket);

        if (bucket.tokens >= 1) {
            bucket.tokens -= 1;
            return true;
        }
        return false;
    }

    private void refill(Bucket bucket) {
        long now = System.currentTimeMillis();
        long elapsedMillis = now - bucket.lastRefill;
        double refilled = elapsedMillis * REFILL_PER_SECOND;
        bucket.tokens = Math.min(CAPACITY, bucket.tokens + refilled);
        bucket.lastRefill = now;
    }

    private static final class Bucket {
        double tokens;
        long   lastRefill;

        Bucket(double tokens, long lastRefill) {
            this.tokens = tokens;
            this.lastRefill = lastRefill;
        }
    }
}
