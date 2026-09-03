package com.amaliai.mcp.servers.sharepoint.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Records lock and unlock events for audit. */
@Service
public class LockAuditTrail {

    private final List<String> entries = new ArrayList<>();

    public void record(String itemId, String action) {
        entries.add(action + ":" + itemId);
    }

    public String last() {
        return entries.get(entries.size() - 1);
    }
}
