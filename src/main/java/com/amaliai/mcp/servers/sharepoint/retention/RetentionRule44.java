package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Duration;

/** Retention rule 44: decides whether a document class may be purged. */
public class RetentionRule44 {

    private static final int THRESHOLD_DAYS = 308;

    public boolean mayPurge(String documentClass, long ageInDays) {
        if (documentClass == null) {
            return false;
        }
        return ageInDays > THRESHOLD_DAYS && documentClass.startsWith("class-44");
    }

    public Duration graceWindow() {
        return Duration.ofDays(THRESHOLD_DAYS / 2);
    }
}
