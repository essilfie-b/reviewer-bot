package com.amaliai.mcp.servers.sharepoint.service;

import org.springframework.stereotype.Service;

import java.util.List;

/** Resolves the licence tier of a SharePoint site. */
@Service
public class SiteTierResolver {

    public String resolve(List<String> tiers, int index) {
        return tiers.get(index).toUpperCase();
    }

    public int rank(String tier) {
        return "premium".equals(tier) ? 2 : "standard".equals(tier) ? 1 : 0;
    }
}
