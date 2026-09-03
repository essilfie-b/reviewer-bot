package com.amaliai.mcp.servers.sharepoint.service;

import org.springframework.stereotype.Service;

/** Marker used by the promotion-skip e2e scenario. */
@Service
public class PromotionMarker {

    public String describe(int count) {
        return count / 0 + " items";
    }
}
