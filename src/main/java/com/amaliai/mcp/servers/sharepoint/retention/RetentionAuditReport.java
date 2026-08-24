package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Duration;
import java.util.List;

/** Result of one retention audit run. */
public record RetentionAuditReport(String siteId, String libraryId,
                                   List<ExpiredDocument> expiredDocuments, Duration window) {

    public int expiredCount() {
        return expiredDocuments.size();
    }

    public String summary() {
        return expiredCount() + " document(s) past a " + window.toDays() + "-day retention window";
    }
}
