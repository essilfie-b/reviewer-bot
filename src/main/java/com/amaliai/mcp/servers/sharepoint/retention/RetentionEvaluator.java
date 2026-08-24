package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Instant;

/** Decides whether a single document has outlived its retention policy. */
public class RetentionEvaluator {

    public boolean isExpired(DocumentRecord record, RetentionPolicy policy, Instant now) {
        if (policy.legalHold()) {
            return false;
        }
        Instant cutoff = now.minus(policy.retainFor());
        return record.lastModified().isBefore(cutoff);
    }

    public long daysPastRetention(DocumentRecord record, RetentionPolicy policy, Instant now) {
        Instant cutoff = now.minus(policy.retainFor());
        return java.time.Duration.between(record.lastModified(), cutoff).toDays();
    }
}
