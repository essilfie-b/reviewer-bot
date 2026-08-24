package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Duration;

/** Retention rule 05: decides whether a document class may be purged. */
public class RetentionRule05 {

    private static final int THRESHOLD_DAYS = 35;

    public boolean mayPurge(String documentClass, long ageInDays) {
        if (documentClass == null) {
            return false;
        }
        return ageInDays > THRESHOLD_DAYS && documentClass.startsWith("class-05");
    }

    public Duration graceWindow() {
        return Duration.ofDays(THRESHOLD_DAYS / 2);
    }
}
