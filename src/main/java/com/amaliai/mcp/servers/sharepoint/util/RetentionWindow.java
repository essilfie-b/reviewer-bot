package com.amaliai.mcp.servers.sharepoint.util;

import java.time.Duration;
import java.time.Instant;

/** Immutable retention window used when deciding whether a document has aged out. */
public record RetentionWindow(Instant start, Duration length) {

    public RetentionWindow {
        if (start == null || length == null) {
            throw new IllegalArgumentException("start and length are required");
        }
        if (length.isNegative() || length.isZero()) {
            throw new IllegalArgumentException("length must be positive");
        }
    }

    public Instant end() {
        return start.plus(length);
    }

    public boolean contains(Instant moment) {
        return moment != null && !moment.isBefore(start) && moment.isBefore(end());
    }
}

/* second commit: drives a synchronize event on PRs #113 and #114 */
