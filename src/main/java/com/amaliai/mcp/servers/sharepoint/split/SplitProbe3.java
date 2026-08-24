package com.amaliai.mcp.servers.sharepoint.split;

/** Small holder number 3 used by the split-failure scenario. */
public record SplitProbe3(String siteId, long value) {

    public boolean over(long limit) {
        return value > limit;
    }

    public String describe() {
        return siteId.trim() + ":" + value;
    }
}
