package com.amaliai.mcp.servers.confluence.service;

import com.amaliai.mcp.servers.confluence.client.ConfluenceGraphClient;
import com.amaliai.mcp.servers.confluence.exception.ConfluenceOperationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Business logic for the Confluence MCP search tool.
 * <p>
 * Responsible for input validation, CQL query construction, delegating the
 * HTTP call to {@link ConfluenceGraphClient}, and formatting the raw API
 * response into a clean JSON array for the LLM.
 * <p>
 * <b>Error contract:</b> validation failures throw {@link IllegalArgumentException};
 * API or serialisation failures throw {@link ConfluenceOperationException}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_QUERY_LENGTH = 1_000;

    private final ConfluenceGraphClient confluenceClient;

    /**
     * Searches Confluence pages by keyword using CQL and returns a trimmed JSON
     * array of matching results.
     *
     * @param token    the user's Confluence access token
     * @param cloudId  the Atlassian cloud ID for the user's tenant
     * @param query    keyword or phrase to search for (must not be blank)
     * @param spaceKey optional space key to restrict the search scope
     * @param limit    maximum results; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @return JSON array string with fields: id, title, type, spaceKey, spaceName, url, excerpt, lastModified
     * @throws IllegalArgumentException     if {@code query} is blank or exceeds the length limit
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String searchContent(String token, String cloudId,
                                String query, String spaceKey, Integer limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be empty");
        }
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "query exceeds maximum length of " + MAX_QUERY_LENGTH + " characters");
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        String cql = buildCql(query, spaceKey);
        log.info("Confluence search — cloudId={} cql='{}' limit={}", cloudId, cql, effectiveLimit);

        String raw = confluenceClient.search(token, cloudId, cql, effectiveLimit);
        return parseSearchResponse(raw);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a CQL expression for a full-text page search, optionally scoped to
     * a specific space.
     * <p>
     * Double-quotes inside the query are escaped to prevent CQL injection.
     */
    private static String buildCql(String query, String spaceKey) {
        String safeQuery = query.replace("\"", "\\\"");
        StringBuilder cql = new StringBuilder("type=page AND text~\"").append(safeQuery).append('"');
        if (spaceKey != null && !spaceKey.isBlank()) {
            String safeSpace = spaceKey.replace("\"", "\\\"");
            cql.append(" AND space.key=\"").append(safeSpace).append('"');
        }
        return cql.toString();
    }

    /**
     * Parses the Confluence v1 {@code /wiki/rest/api/search} response body into a
     * simplified JSON array, extracting only the fields useful to the LLM.
     *
     * <p>Expected v1 response shape:
     * <pre>
     * {
     *   "results": [
     *     {
     *       "content": {
     *         "id": "...", "type": "page", "title": "...",
     *         "space": { "key": "...", "name": "..." },
     *         "_links": { "webui": "/wiki/spaces/.../pages/..." }
     *       },
     *       "excerpt": "...",
     *       "lastModified": "2024-01-01T00:00:00.000Z"
     *     }
     *   ]
     * }
     * </pre>
     */
    private static String parseSearchResponse(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path("results");
            ArrayNode output = OBJECT_MAPPER.createArrayNode();

            for (JsonNode result : results) {
                JsonNode content = result.path("content");
                JsonNode space   = content.path("space");
                JsonNode links   = content.path("_links");

                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put("id",           content.path("id").asText(null));
                item.put("title",        content.path("title").asText(null));
                item.put("type",         content.path("type").asText(null));
                item.put("spaceKey",     space.path("key").asText(null));
                item.put("spaceName",    space.path("name").asText(null));
                item.put("url",          links.path("webui").asText(null));
                item.put("excerpt",      result.path("excerpt").asText(null));
                item.put("lastModified", result.path("lastModified").asText(null));
                output.add(item);
            }

            return OBJECT_MAPPER.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence search response", e);
        }
    }

    /**
     * Parses a Confluence v2 content-list response (e.g. {@code /wiki/api/v2/pages},
     * {@code /wiki/api/v2/spaces/{id}/pages}) into a simplified JSON array.
     *
     * <p>Expected v2 response shape:
     * <pre>
     * {
     *   "results": [
     *     {
     *       "content": {
     *         "id": "...", "type": "page", "title": "...",
     *         "spaceId": "...",
     *         "_links": { "webui": "..." }
     *       },
     *       "resultGlobalContainer": { "title": "Space Name", "displayUrl": "..." },
     *       "excerpt": "...",
     *       "lastModified": "2024-01-01T00:00:00.000Z"
     *     }
     *   ]
     * }
     * </pre>
     */
    static String parseV2SearchResponse(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path("results");
            ArrayNode output = OBJECT_MAPPER.createArrayNode();

            for (JsonNode result : results) {
                JsonNode content = result.path("content");
                JsonNode links   = content.path("_links");

                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put("id",           content.path("id").asText(null));
                item.put("title",        content.path("title").asText(null));
                item.put("type",         content.path("type").asText(null));
                item.put("spaceId",      content.path("spaceId").asText(null));
                item.put("spaceName",    result.path("resultGlobalContainer").path("title").asText(null));
                item.put("url",          links.path("webui").asText(null));
                item.put("excerpt",      result.path("excerpt").asText(null));
                item.put("lastModified", result.path("lastModified").asText(null));
                output.add(item);
            }

            return OBJECT_MAPPER.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence v2 response", e);
        }
    }
}
