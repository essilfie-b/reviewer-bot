package com.amaliai.mcp.servers.confluence.dto;

/**
 * A Confluence page that has gone stale and may be archived.
 *
 * @param pageId    the Confluence page ID
 * @param title     the page title
 * @param staleDays days since the page was last updated
 */
public record ArchivalCandidate(String pageId, String title, int staleDays) {

    public ArchivalCandidate {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be blank");
        }
        if (staleDays < 0) {
            throw new IllegalArgumentException("staleDays must not be negative");
        }
    }
}
