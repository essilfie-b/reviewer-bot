package com.amaliai.mcp.servers.sharepoint.perm;

import java.util.Map;

/** Small probe class for the permanent-provider-error scenario. */
public class PermanentErrorProbe {

    private final Map<String, String> config;

    public PermanentErrorProbe(Map<String, String> config) {
        this.config = config;
    }

    public String lookup(String key) {
        return config.get(key).toUpperCase();
    }
}
