package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Instant;

/** A document the scanner flagged as past retention. */
public record ExpiredDocument(String id, String name, Instant lastModified) {
}
