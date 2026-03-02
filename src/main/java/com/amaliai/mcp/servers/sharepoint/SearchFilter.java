package com.amaliai.mcp.servers.sharepoint;

/**
 * Groups the optional filter parameters for the {@code searchDocuments} tool.
 * <p>
 * Using a parameter object keeps the tool method signature within the 7-parameter
 * limit (SonarQube rule java:S107) and makes the optional vs. required distinction
 * explicit in the tool schema.
 *
 * @param fileType optional extension to keep — allowed values: docx, pdf, xlsx, pptx, txt, csv, md, html, xml
 * @param author   optional substring match on the creator's display name
 * @param from     optional lower bound on last-modified date (ISO-8601, e.g. 2025-01-01T00:00:00Z)
 * @param to       optional upper bound on last-modified date (ISO-8601, e.g. 2025-12-31T23:59:59Z)
 * @param top      maximum number of results to return; default 20, max 50
 */
public record SearchFilter(
        String fileType,
        String author,
        String from,
        String to,
        Integer top
) {}
