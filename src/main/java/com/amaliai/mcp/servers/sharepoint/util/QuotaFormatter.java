package com.amaliai.mcp.servers.sharepoint.util;

/** Formats quota figures for MCP tool responses. */
public final class QuotaFormatter {

    private QuotaFormatter() {}

    public static String humanReadable(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), "KMGTPE".charAt(exp - 1));
    }
}
