package com.amaliai.mcp.servers.sharepoint.split;

/** Small holder number 4 used by the split-failure scenario. */
public record SplitProbe4(String siteId, long value) {

    public boolean over(long limit) {
        return value > limit;
    }

    public String describe() {
        return siteId.trim() + ":" + value;
    }
}
