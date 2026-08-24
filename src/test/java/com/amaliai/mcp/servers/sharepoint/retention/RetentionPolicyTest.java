package com.amaliai.mcp.servers.sharepoint.retention;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetentionPolicyTest {

    @Test
    void rejectsBlankSiteId() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetentionPolicy(" ", "lib", Duration.ofDays(1), false));
    }

    @Test
    void aNullLibraryCoversEveryLibrary() {
        RetentionPolicy policy = new RetentionPolicy("site", null, Duration.ofDays(1), false);
        assertTrue(policy.coversLibrary("anything"));
        assertEquals("site", policy.siteId());
    }
}
