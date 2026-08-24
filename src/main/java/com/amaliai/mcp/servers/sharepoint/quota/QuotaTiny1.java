package com.amaliai.mcp.servers.sharepoint.quota;

/** Tiny holder for the 1 quota dimension. */
public record QuotaTiny1(String siteId, long usedBytes) {

    public boolean over(long limit) {
        return usedBytes > limit;
    }
}

// incremental touch for the partial-failure scenario
