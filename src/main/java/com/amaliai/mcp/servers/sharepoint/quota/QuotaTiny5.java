package com.amaliai.mcp.servers.sharepoint.quota;

/** Tiny holder for the 5 quota dimension. */
public record QuotaTiny5(String siteId, long usedBytes) {

    public boolean over(long limit) {
        return usedBytes > limit;
    }
}
