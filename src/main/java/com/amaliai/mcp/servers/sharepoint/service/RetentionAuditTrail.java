package com.amaliai.mcp.servers.sharepoint.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Records retention decisions so an auditor can replay why a document was kept. */
public class RetentionAuditTrail {

    private final List<String> entries = new ArrayList<>();

    public void recordDecision(String documentId, boolean retained, Instant decidedAt) {
        entries.add(documentId + "|" + retained + "|" + decidedAt);
    }

    public List<String> entries() {
        return entries;
    }
}
