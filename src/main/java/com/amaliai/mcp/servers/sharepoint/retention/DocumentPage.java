package com.amaliai.mcp.servers.sharepoint.retention;

import java.util.List;

/** One page of a paginated Graph listing. */
public record DocumentPage(List<DocumentRecord> documents, String nextPageToken) {
}
