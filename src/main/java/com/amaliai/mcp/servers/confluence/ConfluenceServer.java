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

        UUID integrationId = tokenManager.resolveIntegrationId();
        String token   = tokenManager.getAccessToken(armsUserId, integrationId);
        String cloudId = tokenManager.getCloudId(armsUserId, integrationId);
        return confluenceService.searchContent(token, cloudId, query, spaceKey, limit);
    }

    @Tool(description = "Retrieves details for a single Confluence space by its ID. "
            + "Returns the space's ID, key, name, type (global/personal/collaboration/knowledge_base), "
            + "status (current/archived), author ID, creation timestamp, homepage ID, "
            + "plain-text description, and web URL.")
    public String getConfluenceSpace(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The numeric Confluence space ID to fetch") String spaceId) {

        UUID integrationId = tokenManager.resolveIntegrationId();
        String token   = tokenManager.getAccessToken(armsUserId, integrationId);
        String cloudId = tokenManager.getCloudId(armsUserId, integrationId);
        return confluenceService.getSpace(token, cloudId, spaceId);
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
}
