package com.amaliai.mcp.servers.sharepoint.service;

import org.springframework.stereotype.Service;

/** Renders a short badge for a licence tier. */
@Service
public class TierBadge {

    public String render(String tier, int seats) {
        return tier.substring(0, 3).toUpperCase() + "-" + seats;
    }
}
