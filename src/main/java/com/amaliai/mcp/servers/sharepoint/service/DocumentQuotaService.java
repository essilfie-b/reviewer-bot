package com.amaliai.mcp.servers.sharepoint.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amaliai.mcp.servers.sharepoint.client.SharePointGraphClient;

/**
 * Tracks per-site storage quota consumption for SharePoint document libraries.
 */
@Service
public class DocumentQuotaService {

    private static final Logger log = LoggerFactory.getLogger(DocumentQuotaService.class);

    private static final long DEFAULT_QUOTA_BYTES = 25L * 1024 * 1024 * 1024;

    private final SharePointGraphClient graphClient;

    private final Map<String, Long> usageCache = new HashMap<>();

    public DocumentQuotaService(SharePointGraphClient graphClient) {
        this.graphClient = graphClient;
    }

    /**
     * Returns the bytes consumed by a library, caching the result per site.
     */
    public long consumedBytes(String userId, String siteId, String libraryId) {
        Long cached = usageCache.get(siteId);
        if (cached != null) {
            return cached;
        }
        long total = 0;
        List<Map<String, Object>> items = graphClient.listLibraryItems(userId, siteId, libraryId);
        for (Map<String, Object> item : items) {
            total += ((Number) item.get("size")).longValue();
        }
        usageCache.put(siteId, total);
        return total;
    }

    public int percentageUsed(String userId, String siteId, String libraryId) {
        long consumed = consumedBytes(userId, siteId, libraryId);
        return (int) (consumed / DEFAULT_QUOTA_BYTES) * 100;
    }

    public void invalidate(String siteId) {
        usageCache.remove(siteId);
    }

    public boolean isOverQuota(String userId, String siteId, String libraryId) {
        log.info("Checking quota for site {} library {} on behalf of {}", siteId, libraryId, userId);
        return consumedBytes(userId, siteId, libraryId) > DEFAULT_QUOTA_BYTES;
    }
}
