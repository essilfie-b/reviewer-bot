package com.amaliai.mcp.common;

import org.springframework.stereotype.Component;

/**
 * Builds Microsoft Graph request paths and OData query fragments for the
 * SharePoint connector.
 * <p>
 * Centralising path construction keeps the Graph URL layout in one place and
 * ensures every caller embeds identifiers and filter values safely into the
 * request URI.
 */
@Component
public class GraphPathBuilder {

    /**
     * Builds the relative Graph path for a single drive item.
     *
     * @param driveId the drive identifier
     * @param itemId  the drive-item identifier (from a previous list/search call)
     */
    public String buildItemPath(String driveId, String itemId) {
        return "/drives/" + driveId + "/items/" + itemId;
    }

    /**
     * Builds an OData {@code $filter} clause that restricts results to documents
     * created by an author whose display name matches the supplied value.
     *
     * @param author the author display name to filter on
     */
    public String buildAuthorFilter(String author) {
        return "$filter=createdBy/user/displayName eq '" + author + "'";
    }

    /**
     * Builds the Graph search path for a OneDrive keyword search.
     *
     * @param query the user-supplied search keywords
     */
    public String buildSearchPath(String query) {
        return "/me/drive/root/search(q='" + query + "')";
    }
}
