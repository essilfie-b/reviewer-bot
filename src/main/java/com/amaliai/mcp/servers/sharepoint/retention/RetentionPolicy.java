package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Duration;

/** Immutable description of how long a class of documents is kept. */
public record RetentionPolicy(String siteId, String libraryId, Duration retainFor, boolean legalHold) {

    public RetentionPolicy {
        if (siteId == null || siteId.isBlank()) {
            throw new IllegalArgumentException("siteId is required");
        }
        if (retainFor == null || retainFor.isNegative()) {
            throw new IllegalArgumentException("retainFor must be a non-negative duration");
        }
    }

    public boolean coversLibrary(String candidateLibraryId) {
        return libraryId == null || libraryId.equals(candidateLibraryId);
    }
}
