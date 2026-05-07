package com.amaliai.mcp.servers.confluence;

import com.amaliai.mcp.servers.confluence.service.ConfluenceService;
import com.amaliai.mcp.servers.confluence.util.ConfluenceServerHelper;
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

        ConfluenceServerHelper.Credentials creds = ConfluenceServerHelper.resolveCredentials(armsUserId, tokenManager);
        return confluenceService.searchContent(creds.token(), creds.cloudId(), query, spaceKey, limit);
    }

    @Tool(description = "Retrieves metadata for a specific Confluence page by its ID. "
            + "Returns the page ID, title, type, space key, parent page title, URL, and last-modified date. "
            + "Does NOT return the page body content — use getConfluencePageContent for that. "
            + "Use the 'id' field from searchConfluenceContent or listConfluencePages as the pageId.")
    public String getConfluencePage(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The Confluence page ID to retrieve") String pageId) {

        ConfluenceServerHelper.Credentials creds = ConfluenceServerHelper.resolveCredentials(armsUserId, tokenManager);
        return confluenceService.getPage(creds.token(), creds.cloudId(), pageId);
    }

    @Tool(description = "Retrieves the full text content of a specific Confluence page by its ID. "
            + "Returns page metadata (title, type, space, URL) plus the plain-text body content. "
            + "HTML is stripped from the body. Content is truncated at 100,000 characters for very long pages. "
            + "Use the 'id' field from searchConfluenceContent or listConfluencePages as the pageId.")
    public String getConfluencePageContent(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The Confluence page ID whose content to retrieve") String pageId) {

        ConfluenceServerHelper.Credentials creds = ConfluenceServerHelper.resolveCredentials(armsUserId, tokenManager);
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

        ConfluenceServerHelper.Credentials creds = ConfluenceServerHelper.resolveCredentials(armsUserId, tokenManager);
        return confluenceService.listPages(creds.token(), creds.cloudId(), spaceKey, limit);
    }

    @Tool(description = "Retrieves details for a single Confluence space by its key. "
            + "Returns the space's ID, key, name, type (global/personal/collaboration/knowledge_base), "
            + "status (current/archived), author ID, creation timestamp, homepage ID, "
            + "plain-text description, and web URL.")
    public String getConfluenceSpace(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The Confluence space key to fetch (e.g. 'ENG', 'LMS')") String spaceKey) {

        UUID integrationId = tokenManager.resolveIntegrationId();
        String token   = tokenManager.getAccessToken(armsUserId, integrationId);
        String cloudId = tokenManager.getCloudId(armsUserId, integrationId);
        return confluenceService.getSpace(token, cloudId, spaceKey);
    }

    @Tool(description = "Lists Confluence spaces visible to the authenticated user. "
            + "Returns a JSON object with a 'results' array (each entry containing id, key, name, "
            + "type, status, authorId, createdAt, homepageId, description, url) and a 'nextCursor' "
            + "string for pagination (null when there are no more pages). "
            + "Optionally filter by space type (global|personal|collaboration|knowledge_base) "
            + "and status (current|archived).")
    public String listConfluenceSpaces(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "Space type filter: global, personal, collaboration, or knowledge_base (optional)",
                    required = false) String type,
            @ToolParam(description = "Space status filter: current or archived (optional)",
                    required = false) String status,
            @ToolParam(description = "Maximum number of spaces to return (default 25, max 250)",
                    required = false) Integer limit,
            @ToolParam(description = "Opaque pagination cursor from a previous response's nextCursor (optional)",
                    required = false) String cursor) {

        UUID integrationId = tokenManager.resolveIntegrationId();
        String token   = tokenManager.getAccessToken(armsUserId, integrationId);
        String cloudId = tokenManager.getCloudId(armsUserId, integrationId);
        return confluenceService.listSpaces(token, cloudId, type, status, limit, cursor);
    }

    @Tool(description = "Retrieves the direct child pages of a Confluence page. "
            + "Returns each child page's ID, title, space ID, parent page ID, URL, and last-modified date. "
            + "Useful for navigating a page hierarchy or discovering sub-pages under a known parent.")
    public String getConfluencePageChildren(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The numeric ID of the parent Confluence page") String pageId,
            @ToolParam(description = "Maximum number of child pages to return (default 20, max 50)",
                    required = false) Integer limit) {

        UUID integrationId = tokenManager.resolveIntegrationId();
        String token   = tokenManager.getAccessToken(armsUserId, integrationId);
        String cloudId = tokenManager.getCloudId(armsUserId, integrationId);
        return confluenceService.getPageChildren(token, cloudId, pageId, limit);
    }

    @Tool(description = "Retrieves the list of file attachments on a Confluence page. "
            + "Returns each attachment's ID, title, media type, file size in bytes, "
            + "the page it belongs to, a viewable URL, a direct download link, and last-modified date.")
    public String getConfluenceAttachments(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The numeric ID of the Confluence page") String pageId,
            @ToolParam(description = "Maximum number of attachments to return (default 20, max 50)",
                    required = false) Integer limit) {

        UUID integrationId = tokenManager.resolveIntegrationId();
        String token   = tokenManager.getAccessToken(armsUserId, integrationId);
        String cloudId = tokenManager.getCloudId(armsUserId, integrationId);
        return confluenceService.getAttachments(token, cloudId, pageId, limit);
    }
}
