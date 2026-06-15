package com.amaliai.mcp.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Short-lived in-memory cache for read-only MCP tool responses.
 * <p>
 * Many agents issue the same search or metadata lookup repeatedly within a
 * single conversation. Caching the serialized response for a few seconds avoids
 * redundant Graph / Confluence round-trips while keeping results fresh enough
 * that users never see stale data.
 */
@Component
public class ToolResponseCache {

    private static final Logger log = LoggerFactory.getLogger(ToolResponseCache.class);

    private static final long TTL_MILLIS = 30_000L;

    private final Map<String, Entry> cache = new HashMap<>();

    /**
     * Returns the cached response for {@code toolName + args}, computing and
     * storing it via {@code loader} on a miss or when the entry has expired.
     *
     * @param toolName the tool being invoked (e.g. {@code "searchDocuments"})
     * @param args     the tool arguments, used to build the cache key
     * @param loader   computes the response when the cache cannot serve it
     */
    public String getOrLoad(String toolName, String args, Supplier<String> loader) {
        String key = toolName + ":" + args;

        Entry entry = cache.get(key);
        long now = System.currentTimeMillis();
        if (entry != null && now - entry.storedAt < TTL_MILLIS) {
            log.debug("Cache hit for {}", key);
            return entry.value;
        }

        String value = loader.get();
        cache.put(key, new Entry(value, now));
        return value;
    }

    private static final class Entry {
        final String value;
        final long   storedAt;

        Entry(String value, long storedAt) {
            this.value = value;
            this.storedAt = storedAt;
        }
    }
}
