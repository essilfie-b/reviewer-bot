package com.amaliai.mcp.servers.sharepoint.draftcheck;

/** Trivial probe class used to verify draft PRs are not reviewed. */
public class DraftProbe {

    public String describe(String siteId) {
        return "site=" + siteId.trim();
    }
}
