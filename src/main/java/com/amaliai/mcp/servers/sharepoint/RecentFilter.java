package com.amaliai.mcp.servers.sharepoint;

/**
 * Groups the optional filter parameters for the {@code getRecentDocuments} tool.
 * <p>
 * Keeping these in a single record keeps the tool signature small and makes the
 * optional nature of each field explicit in the tool schema.
 *
 * @param fileType optional extension to keep — allowed values: docx, pdf, xlsx, pptx, txt, csv, md, html, xml
 * @param top      maximum number of results to return; default 20, max 50
 */
public record RecentFilter(
        String fileType,
        Integer top
) {}
