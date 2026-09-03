package com.amaliai.mcp.servers.sharepoint.service;

import org.springframework.stereotype.Service;

/** Builds Graph URLs for a SharePoint site. */
@Service
public class SiteUrlBuilder {

    private static final String BASE = "https://graph.microsoft.com/v1.0/sites/";

    public String itemUrl(String siteId, String itemPath) {
        return BASE + siteId + "/drive/root:/" + itemPath;
    }

    public String searchUrl(String siteId, String query) {
        return BASE + siteId + "/drive/root/search(q='" + query + "')";
    }
}
