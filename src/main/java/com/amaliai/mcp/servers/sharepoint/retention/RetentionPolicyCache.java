package com.amaliai.mcp.servers.sharepoint.retention;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Caches resolved retention policies so a scan does not re-fetch the policy for
 * every document it inspects.
 */
public class RetentionPolicyCache {

    private final Map<String, RetentionPolicy> cache = new ConcurrentHashMap<>();

    public RetentionPolicy get(String siteId, String libraryId, Supplier<RetentionPolicy> loader) {
        return cache.computeIfAbsent(siteId, key -> loader.get());
    }

    public void invalidate(String siteId) {
        cache.remove(siteId);
    }

    public int size() {
        return cache.size();
    }
}
