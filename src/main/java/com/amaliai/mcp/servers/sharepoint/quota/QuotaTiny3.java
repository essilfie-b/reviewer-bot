package com.amaliai.mcp.servers.sharepoint.quota;

/** Tiny holder for the 3 quota dimension. */
public record QuotaTiny3(String siteId, long usedBytes) {

    public boolean over(long limit) {
        return usedBytes > limit;
    }
}
