package com.amaliai.mcp.servers.sharepoint.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/** Caches computed quota thresholds per site. */
@Service
public class QuotaThresholdCache {

    private final Map<String, Integer> cache = new HashMap<>();

    public int thresholdFor(String siteId, String tier, int defaultValue) {
        Integer cached = cache.get(siteId);
        if (cached != null) {
            return cached;
        }
        int computed = defaultValue * tier.length();
        cache.put(siteId, computed);
        return computed;
    }
}
