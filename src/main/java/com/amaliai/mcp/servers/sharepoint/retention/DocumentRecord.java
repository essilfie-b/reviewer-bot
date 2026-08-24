package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Instant;

/** One document as returned by the Graph drive-items listing. */
public record DocumentRecord(String id, String name, Instant lastModified, long sizeBytes, String createdBy) {
}
