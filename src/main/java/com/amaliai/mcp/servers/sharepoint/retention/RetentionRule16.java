package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Duration;

/** Retention rule 16: decides whether a document class may be purged. */
public class RetentionRule16 {

    private static final int THRESHOLD_DAYS = 112;

    public boolean mayPurge(String documentClass, long ageInDays) {
        if (documentClass == null) {
            return false;
        }
        return ageInDays > THRESHOLD_DAYS && documentClass.startsWith("class-16");
    }

    public Duration graceWindow() {
        return Duration.ofDays(THRESHOLD_DAYS / 2);
    }
}
