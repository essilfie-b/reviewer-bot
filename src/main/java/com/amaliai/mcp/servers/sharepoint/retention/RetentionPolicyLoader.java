package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Duration;
import java.util.Map;

/** Reads retention policies out of the tenant configuration. */
public class RetentionPolicyLoader {

    private final Map<String, String> tenantConfig;

    public RetentionPolicyLoader(Map<String, String> tenantConfig) {
        this.tenantConfig = tenantConfig;
    }

    public RetentionPolicy load(String siteId, String libraryId) {
        String days = tenantConfig.get("retention.days." + siteId);
        String hold = tenantConfig.get("retention.hold." + siteId);
        return new RetentionPolicy(siteId, libraryId,
                Duration.ofDays(Long.parseLong(days)), Boolean.parseBoolean(hold));
    }
}
