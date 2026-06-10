package com.amaliai.mcp.servers.sharepoint.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link SharePointUtil}.
 *
 * Coverage strategy
 * -----------------
 * SharePointUtil is a deprecated utility class with no logic.
 * Its only purpose is to serve as a deprecation marker for code split into
 * other classes (SharePointTokenManager, SharePointResponseUtil).
 * This class is marked for removal.
 */
@SuppressWarnings("deprecation")
class SharePointUtilTest {

    @Test
    void classIsDeprecated() {
        assertThat(SharePointUtil.class.isAnnotationPresent(Deprecated.class))
                .as("SharePointUtil should be marked as @Deprecated")
                .isTrue();
    }

    @Test
    void classCanBeReferenced() {
        // Verify the class itself exists and is accessible
        assertThat(SharePointUtil.class).isNotNull();
        assertThat(SharePointUtil.class.getSimpleName()).isEqualTo("SharePointUtil");
    }
}



