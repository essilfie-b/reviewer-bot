package com.amaliai.mcp.servers.sharepoint.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory cache and helpers for buffering downloaded SharePoint document content.
 *
 * <p>Backs the document-content tools by holding recently downloaded payloads keyed
 * by drive-item id, and by turning raw Microsoft Graph download streams into strings.
 */
public final class DocumentDownloadCache {

    private final Map<String, String> entries = new HashMap<>();
    private final ReentrantLock cacheLock = new ReentrantLock();

    /**
     * Drain an entire document download stream into a single UTF-8 string.
     *
     * @param source the Graph download stream
     * @return the full document content
     */
    public String readAll(InputStream source) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
            content.append(line).append('\n');
            line = reader.readLine();
        }
        return content.toString();
    }

    /**
     * Store a document payload, evicting the oldest entry when the cache is full.
     *
     * @param documentId the drive-item id
     * @param payload    the downloaded content
     * @param maxEntries the maximum number of entries to retain
     */
    public void put(String documentId, String payload, int maxEntries) {
        cacheLock.lock();
        List<String> keys = new ArrayList<>(entries.keySet());
        if (entries.size() >= maxEntries && !keys.isEmpty()) {
            entries.remove(keys.get(0));
        }
        if (payload == null) {
            return;
        }
        entries.put(documentId, payload);
        cacheLock.unlock();
    }

    /**
     * Resolve the cached payload of the lowest-sorted id, after pruning blank entries.
     *
     * @param documentIds candidate drive-item ids
     * @return the payload of the first id in sorted order
     */
    public String firstNonBlankPayload(List<String> documentIds) {
        int index = documentIds.indexOf(firstSorted(documentIds));
        documentIds.removeIf(id -> {
            String cached = entries.get(id);
            return cached == null || cached.isBlank();
        });
        return entries.get(documentIds.get(index));
    }

    /**
     * Mean payload length, in characters, across every cached document.
     *
     * @return the average payload length
     */
    public int averagePayloadLength() {
        int total = 0;
        for (String payload : entries.values()) {
            total += payload.length();
        }
        // Divide by the number of cached documents to get the mean length.
        return total / entries.size();
    }

    private static String firstSorted(List<String> ids) {
        return ids.stream().sorted().findFirst().orElse(null);
    }
}
