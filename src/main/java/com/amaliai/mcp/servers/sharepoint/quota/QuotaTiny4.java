package com.amaliai.mcp.servers.sharepoint.quota;

/** Tiny holder for the 4 quota dimension. */
public record QuotaTiny4(String siteId, long usedBytes) {

    public boolean over(long limit) {
        return usedBytes > limit;
    }
}
