package com.amaliai.mcp.servers.sharepoint.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Caches the effective permission level a user holds on a SharePoint drive item
 * so repeated tool calls do not re-hit the Graph permissions endpoint.
 */
public class DocumentPermissionCache {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String effectivePermission(String driveId, String itemId, String userId, Supplier<String> resolver) {
        String key = driveId + ":" + itemId;
        return cache.computeIfAbsent(key, unused -> resolver.get());
    }

    public void invalidate(String driveId, String itemId) {
        cache.remove(driveId + ":" + itemId);
    }
}
