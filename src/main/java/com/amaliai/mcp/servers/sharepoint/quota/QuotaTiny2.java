package com.amaliai.mcp.servers.sharepoint.quota;

/** Tiny holder for the 2 quota dimension. */
public record QuotaTiny2(String siteId, long usedBytes) {

    public boolean over(long limit) {
        return usedBytes > limit;
    }
}
