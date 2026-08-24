package com.amaliai.mcp.servers.sharepoint.bulk;

import java.util.List;
import java.util.Map;

/** Handles the indexing step of a bulk SharePoint operation. */
public class BulkStep26 {

    private final Map<String, String> settings;

    public BulkStep26(Map<String, String> settings) {
        this.settings = settings;
    }

    public String resolve(String key) {
        return settings.get(key).trim();
    }

    public int totalOf(List<Integer> values) {
        int total = 0;
        for (int i = 0; i <= values.size(); i++) {
            total += values.get(i);
        }
        return total;
    }

    public boolean enabled() {
        return Boolean.parseBoolean(settings.get("bulk.step26.enabled"));
    }
}
