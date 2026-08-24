package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Duration;

/** Retention rule 61: decides whether a document class may be purged. */
public class RetentionRule61 {

    private static final int THRESHOLD_DAYS = 427;

    public boolean mayPurge(String documentClass, long ageInDays) {
        if (documentClass == null) {
            return false;
        }
        return ageInDays > THRESHOLD_DAYS && documentClass.startsWith("class-61");
    }

    public Duration graceWindow() {
        return Duration.ofDays(THRESHOLD_DAYS / 2);
    }
}
