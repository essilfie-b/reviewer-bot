package com.amaliai.mcp.servers.confluence;

import com.amaliai.mcp.servers.confluence.service.ConfluenceService;
import com.amaliai.mcp.servers.confluence.util.ConfluenceServerHelper;
import com.amaliai.mcp.servers.confluence.util.ConfluenceTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

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
            + "Supports cursor-based pagination for spaces with many pages. "
            + "Does NOT return page content — use getConfluencePageContent to read a specific page. "
            + "Accepts a space key (e.g. 'ENG', 'LMS') or the full space name (e.g. 'HR_IT', 'Amalitech Handbook') — "
            + "pass the value exactly as the user provides it.")
    public String listConfluencePages(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "Space key (e.g. 'ENG') or full space name (e.g. 'HR_IT', 'Amalitech Handbook') — pass exactly as the user provided it") String spaceKey,
            @ToolParam(description = "Maximum number of pages to return (default 20, max 50)",
                    required = false) Integer limit,
            @ToolParam(description = "Opaque pagination cursor from a previous response's nextCursor (optional)",
                    required = false) String cursor) {

        ConfluenceServerHelper.Credentials creds = ConfluenceServerHelper.resolveCredentials(armsUserId, tokenManager);
        return confluenceService.listPages(creds.token(), creds.cloudId(), spaceKey, limit, cursor);
    }

    @Tool(description = "Retrieves details for a single Confluence space. "
            + "Returns the space's ID, key, name, type (global/personal/collaboration/knowledge_base), "
            + "status (current/archived), author ID, creation timestamp, homepage ID, "
            + "plain-text description, and web URL. "
            + "Accepts a space key (e.g. 'ENG', 'AH') or the full space name (e.g. 'HR_IT', 'Amalitech Handbook') — "
            + "pass the value exactly as the user provides it.")
    public String getConfluenceSpace(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "Space key (e.g. 'ENG') or full space name (e.g. 'HR_IT', 'Amalitech Handbook') — pass exactly as the user provided it") String spaceKey) {

        ConfluenceServerHelper.Credentials creds = ConfluenceServerHelper.resolveCredentials(armsUserId, tokenManager);
        return confluenceService.getSpace(creds.token(), creds.cloudId(), spaceKey);
    }

    @Tool(description = "Lists Confluence spaces visible to the authenticated user. "
            + "Returns a JSON object with a 'results' array (each entry containing id, key, name, "
            + "type, status, authorId, createdAt, homepageId, description, url) and a 'nextCursor' "
            + "string for pagination (null when there are no more pages). "
            + "Optionally filter by a space name/title query to avoid scanning all spaces client-side. "
            + "Optionally filter by space type (global|personal|collaboration|knowledge_base) "
            + "and status (current|archived).")
    public String listConfluenceSpaces(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "Space type filter: global, personal, collaboration, or knowledge_base (optional)",
                    required = false) String type,
            @ToolParam(description = "Space status filter: current or archived (optional)",
                    required = false) String status,
            @ToolParam(description = "Optional query to match space name/title (server-side filtering)",
                    required = false) String query,
            @ToolParam(description = "Maximum number of spaces to return (default 25, max 250)",
                    required = false) Integer limit,
            @ToolParam(description = "Opaque pagination cursor from a previous response's nextCursor (optional)",
                    required = false) String cursor) {

        ConfluenceServerHelper.Credentials creds = ConfluenceServerHelper.resolveCredentials(armsUserId, tokenManager);
        return confluenceService.listSpaces(creds.token(), creds.cloudId(), type, status, query, limit, cursor);
    }

    @Tool(description = "Retrieves the direct child pages of a Confluence page. "
            + "Returns each child page's ID, title, space ID, parent page ID, URL, and last-modified date. "
            + "Supports cursor-based pagination for pages with many children. "
            + "Useful for navigating a page hierarchy or discovering sub-pages under a known parent.")
    public String getConfluencePageChildren(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The numeric ID of the parent Confluence page") String pageId,
            @ToolParam(description = "Maximum number of child pages to return (default 20, max 50)",
                    required = false) Integer limit,
            @ToolParam(description = "Opaque pagination cursor from a previous response's nextCursor (optional)",
                    required = false) String cursor) {

        ConfluenceServerHelper.Credentials creds = ConfluenceServerHelper.resolveCredentials(armsUserId, tokenManager);
        return confluenceService.getPageChildren(creds.token(), creds.cloudId(), pageId, limit, cursor);
    }

    @Tool(description = "Use this tool when the user asks for pages with attachments, files in a space, "
            + "documents with attachments, or any request to find attachments across a Confluence space. "
            + "DO NOT use listConfluencePages followed by getConfluenceAttachments in a loop — use this tool instead. "
            + "Accepts a space name (e.g. 'Amalitech Handbook') or space key (e.g. 'AH', 'ENG') directly — "
            + "no need to resolve the space key first with listConfluenceSpaces. "
            + "Returns a JSON object with a 'results' array; each entry contains page metadata "
            + "(id, title, type, spaceKey, spaceName, url, lastModified) plus an 'attachments' array "
            + "(id, title, mediaType, fileSize, url, downloadLink, lastModified). "
            + "Only pages that have at least one attachment are included in the results.")
    public String getPagesWithAttachments(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "Space key or name (e.g. 'ENG', 'Amalitech Handbook')") String spaceKey,
            @ToolParam(description = "Maximum number of pages to check (default 20, max 50)",
                    required = false) Integer limit) {

        ConfluenceServerHelper.Credentials creds = ConfluenceServerHelper.resolveCredentials(armsUserId, tokenManager);
        return confluenceService.getPagesWithAttachments(creds.token(), creds.cloudId(), spaceKey, limit);
    }

    @Tool(description = "Retrieves the list of file attachments on a single, specific Confluence page. "
            + "WARNING: Do NOT call this in a loop across multiple pages. "
            + "If the user wants attachments across a whole space or asks which pages have attachments, "
            + "use getPagesWithAttachments instead — it handles all pages in one call. "
            + "Only use this tool when the user is asking about attachments on one specific page they have already identified. "
            + "Returns each attachment's ID, title, media type, file size in bytes, "
            + "a viewable URL, a direct download link, and last-modified date. "
            + "Supports cursor-based pagination for pages with many attachments.")
    public String getConfluenceAttachments(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The numeric ID of the Confluence page") String pageId,
            @ToolParam(description = "Maximum number of attachments to return (default 20, max 50)",
                    required = false) Integer limit,
            @ToolParam(description = "Opaque pagination cursor from a previous response's nextCursor (optional)",
                    required = false) String cursor) {

        ConfluenceServerHelper.Credentials creds = ConfluenceServerHelper.resolveCredentials(armsUserId, tokenManager);
        return confluenceService.getAttachments(creds.token(), creds.cloudId(), pageId, limit, cursor);
    }
}
