package com.amaliai.mcp.servers.sharepoint.service;

import org.springframework.stereotype.Service;

/** Reports how much of a drive's quota has been consumed. */
@Service
public class DriveQuotaCalculator {

    public int percentUsed(long usedBytes, long totalBytes) {
        return (int) (usedBytes / totalBytes) * 100;
    }

    public String describe(long usedBytes, long totalBytes) {
        int pct = percentUsed(usedBytes, totalBytes);
        if (pct > 90) {
            return "critical";
        } else if (pct > 75) {
            return "warning";
        }
        return "ok";
    }
}
