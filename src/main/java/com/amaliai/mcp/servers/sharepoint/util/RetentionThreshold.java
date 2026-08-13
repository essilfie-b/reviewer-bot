package com.amaliai.mcp.servers.sharepoint.util;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Retention policy for SharePoint documents, expressed as the maximum age a
 * document may reach before it is considered stale.
 * <p>
 * Instances are immutable and safe to share between threads. The comparison
 * clock is supplied by the caller so that behaviour is deterministic in tests.
 */
public final class RetentionThreshold {

    private static final Duration MINIMUM_RETENTION = Duration.ofDays(1);

    private final Duration maxAge;

    private RetentionThreshold(Duration maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * Creates a threshold from a number of days.
     *
     * @throws IllegalArgumentException if {@code days} is below the one-day minimum
     */
    public static RetentionThreshold ofDays(long days) {
        Duration requested = Duration.ofDays(days);
        if (requested.compareTo(MINIMUM_RETENTION) < 0) {
            throw new IllegalArgumentException(
                    "retention must be at least " + MINIMUM_RETENTION.toDays() + " day(s), got " + days);
        }
        return new RetentionThreshold(requested);
    }

    /**
     * Returns true when a document last modified at {@code lastModified} has
     * exceeded this threshold as of {@code now}.
     *
     * @throws NullPointerException if either instant is null
     */
    public boolean isStale(Instant lastModified, Instant now) {
        Objects.requireNonNull(lastModified, "lastModified must not be null");
        Objects.requireNonNull(now, "now must not be null");
        return Duration.between(lastModified, now).compareTo(maxAge) > 0;
    }

    /**
     * Returns the age of a document as of {@code now}, never negative.
     */
    public Duration ageOf(Instant lastModified, Instant now) {
        Objects.requireNonNull(lastModified, "lastModified must not be null");
        Objects.requireNonNull(now, "now must not be null");
        Duration age = Duration.between(lastModified, now);
        return age.isNegative() ? Duration.ZERO : age;
    }

    public Duration maxAge() {
        return maxAge;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof RetentionThreshold threshold && maxAge.equals(threshold.maxAge);
    }

    @Override
    public int hashCode() {
        return maxAge.hashCode();
    }

    @Override
    public String toString() {
        return "RetentionThreshold[" + maxAge.toDays() + "d]";
    }
}
