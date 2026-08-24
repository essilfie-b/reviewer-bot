package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Duration;

/** Retention rule 12: decides whether a document class may be purged. */
public class RetentionRule12 {

    private static final int THRESHOLD_DAYS = 84;

    public boolean mayPurge(String documentClass, long ageInDays) {
        if (documentClass == null) {
            return false;
        }
        return ageInDays > THRESHOLD_DAYS && documentClass.startsWith("class-12");
    }

    public Duration graceWindow() {
        return Duration.ofDays(THRESHOLD_DAYS / 2);
    }
}
