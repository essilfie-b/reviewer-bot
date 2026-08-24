package com.amaliai.mcp.servers.sharepoint.retention;

/** Fetches one page of documents from the SharePoint Graph API. */
public interface DocumentPageFetcher {

    DocumentPage fetchPage(String siteId, String libraryId, String pageToken);
}
