package com.amaliai.mcp.servers.confluence;

import com.amaliai.mcp.servers.confluence.service.ConfluenceService;
import com.amaliai.mcp.servers.confluence.util.ConfluenceTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * MCP tool entrypoint for Confluence operations.
 * <p>
 * This class is intentionally thin: it resolves credentials and delegates
 * every operation to {@link ConfluenceService}. All exception handling is
 * managed centrally by {@link com.amaliai.mcp.config.McpToolExceptionHandler}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceServer {

    private final ConfluenceService confluenceService;
    private final ConfluenceTokenManager tokenManager;

    // -------------------------------------------------------------------------
    // MCP Tools
    // -------------------------------------------------------------------------

    @Tool(description = "Searches Confluence pages by keyword using CQL (Confluence Query Language). "
            + "Returns a list of matching pages with their ID, title, type, space key, space name, "
            + "URL, a short excerpt, and last-modified date. "
            + "Optionally restrict the search to a specific space by providing its space key. "
            + "Results are ordered by Confluence relevance score.")
    public String searchConfluenceContent(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "Keyword or phrase to search for in Confluence pages") String query,
            @ToolParam(description = "Space key to restrict the search to a specific Confluence space (optional)",
                    required = false) String spaceKey,
            @ToolParam(description = "Maximum number of results to return (default 20, max 50)",
                    required = false) Integer limit) {

        Credentials creds = resolveCredentials(armsUserId);
        return confluenceService.searchContent(creds.token(), creds.cloudId(), query, spaceKey, limit);
    }

    @Tool(description = "Retrieves metadata for a specific Confluence page by its ID. "
            + "Returns the page ID, title, type, space key, parent page title, URL, and last-modified date. "
            + "Does NOT return the page body content — use getConfluencePageContent for that. "
            + "Use the 'id' field from searchConfluenceContent or listConfluencePages as the pageId.")
    public String getConfluencePage(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The Confluence page ID to retrieve") String pageId) {

        Credentials creds = resolveCredentials(armsUserId);
        return confluenceService.getPage(creds.token(), creds.cloudId(), pageId);
    }

    @Tool(description = "Retrieves the full text content of a specific Confluence page by its ID. "
            + "Returns page metadata (title, type, space, URL) plus the plain-text body content. "
            + "HTML is stripped from the body. Content is truncated at 100,000 characters for very long pages. "
            + "Use the 'id' field from searchConfluenceContent or listConfluencePages as the pageId.")
    public String getConfluencePageContent(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The Confluence page ID whose content to retrieve") String pageId) {

        Credentials creds = resolveCredentials(armsUserId);
        return confluenceService.getPageContent(creds.token(), creds.cloudId(), pageId);
    }

    @Tool(description = "Lists pages in a specific Confluence space. "
            + "Returns each page's ID, title, type, space key, space name, and URL. "
            + "Does NOT return page content — use getConfluencePageContent to read a specific page. "
            + "Use the space key visible in Confluence URLs (e.g. 'ENG', 'LMS', 'AED').")
    public String listConfluencePages(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The Confluence space key to list pages from (e.g. 'ENG', 'LMS')") String spaceKey,
            @ToolParam(description = "Maximum number of pages to return (default 20, max 50)",
                    required = false) Integer limit) {

        Credentials creds = resolveCredentials(armsUserId);
        return confluenceService.listPages(creds.token(), creds.cloudId(), spaceKey, limit);
    }

    private Credentials resolveCredentials(int armsUserId) {
        UUID integrationId = tokenManager.resolveIntegrationId();
        return new Credentials(
                tokenManager.getAccessToken(armsUserId, integrationId),
                tokenManager.getCloudId(armsUserId, integrationId));
    }

    private record Credentials(String token, String cloudId) {}
}
