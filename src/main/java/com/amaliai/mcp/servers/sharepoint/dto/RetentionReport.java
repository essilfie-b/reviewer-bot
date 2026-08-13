package com.amaliai.mcp.servers.sharepoint.dto;

import java.util.List;
import java.util.Objects;

/**
 * Immutable result of a retention audit over a single SharePoint site.
 *
 * @param siteId          the audited site
 * @param averageAgeDays  mean age of the documents inspected, in days
 * @param staleDocuments  names of documents past the retention threshold
 */
public record RetentionReport(String siteId, int averageAgeDays, List<String> staleDocuments) {

    public RetentionReport {
        Objects.requireNonNull(siteId, "siteId must not be null");
        if (averageAgeDays < 0) {
            throw new IllegalArgumentException("averageAgeDays must not be negative");
        }
        staleDocuments = List.copyOf(Objects.requireNonNullElse(staleDocuments, List.of()));
    }

    /**
     * Returns true when the site has at least one document past the retention threshold.
     */
    public boolean hasStaleDocuments() {
        return !staleDocuments.isEmpty();
    }
}
