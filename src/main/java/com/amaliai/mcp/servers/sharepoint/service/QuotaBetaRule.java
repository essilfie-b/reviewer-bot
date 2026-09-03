package com.amaliai.mcp.servers.sharepoint.service;

import org.springframework.stereotype.Service;

/** Quota rule Beta. */
@Service
public class QuotaBetaRule {

    public int scale(int value, int divisor) {
        return value / divisor;
    }

    public String label(int value) {
        if (value > 100) {
            return "over";
        }
        return "under";
    }
}
