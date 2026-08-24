package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Duration;

/** Retention rule 06: decides whether a document class may be purged. */
public class RetentionRule06 {

    private static final int THRESHOLD_DAYS = 42;

    public boolean mayPurge(String documentClass, long ageInDays) {
        if (documentClass == null) {
            return false;
        }
        return ageInDays > THRESHOLD_DAYS && documentClass.startsWith("class-06");
    }

    public Duration graceWindow() {
        return Duration.ofDays(THRESHOLD_DAYS / 2);
    }
}
