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
    private static final int DEFAULT_SPACES_LIMIT = 25;
    private static final int MAX_SPACES_LIMIT = 250;

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

    /**
     * Retrieves details of a single Confluence space by its ID.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param spaceId the numeric Confluence space ID (must not be blank)
     * @return JSON object string with fields: id, key, name, type, status,
     *         authorId, createdAt, homepageId, description, url
     * @throws IllegalArgumentException     if {@code spaceId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String getSpace(String token, String cloudId, String spaceId) {
        if (spaceId == null || spaceId.isBlank()) {
            throw new IllegalArgumentException("spaceId must not be empty");
        }
        log.info("Confluence getSpace — cloudId={} spaceId={}", cloudId, spaceId);

        String raw = confluenceClient.getSpace(token, cloudId, spaceId.trim());
        return parseSpaceResponse(raw);
    }

    /**
     * Lists Confluence spaces for the tenant with optional type/status filters
     * and cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param type    optional space type: global|personal|collaboration|knowledge_base
     * @param status  optional space status: current|archived
     * @param limit   maximum spaces; clamped to [{@value DEFAULT_SPACES_LIMIT}, {@value MAX_SPACES_LIMIT}]
     * @param cursor  optional opaque pagination cursor from a previous call
     * @return JSON object string with {@code results} (array of spaces) and {@code nextCursor}
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String listSpaces(String token, String cloudId,
                             String type, String status, Integer limit, String cursor) {
        int effectiveLimit = (limit == null || limit <= 0)
                ? DEFAULT_SPACES_LIMIT
                : Math.min(limit, MAX_SPACES_LIMIT);

        log.info("Confluence listSpaces — cloudId={} type={} status={} limit={} cursor={} token={}",
                cloudId, type, status, effectiveLimit, cursor, token);

        String raw = confluenceClient.listSpaces(token, cloudId, type, status, effectiveLimit, cursor);

        String res =  parseSpacesListResponse(raw);
        return res;
    }

    /**
     * Returns the attachments for a Confluence page as a trimmed JSON array.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param pageId  numeric ID of the page whose attachments to fetch (must not be blank)
     * @param limit   maximum results; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @return JSON array string with fields: id, title, type, mediaType, fileSize, pageId, url, downloadLink, lastModified
     * @throws IllegalArgumentException     if {@code pageId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    /**
     * Returns the direct child pages of a Confluence page as a trimmed JSON array.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param pageId  numeric ID of the parent page (must not be blank)
     * @param limit   maximum results; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @return JSON array string with fields: id, title, spaceId, parentId, url, lastModified
     * @throws IllegalArgumentException     if {@code pageId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String getPageChildren(String token, String cloudId, String pageId, Integer limit) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be empty");
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        log.info("Confluence page children — cloudId={} pageId={} limit={}", cloudId, pageId, effectiveLimit);

        String raw = confluenceClient.getPageChildren(token, cloudId, pageId, effectiveLimit);
        return parsePageChildrenResponse(raw);
    }

    public String getAttachments(String token, String cloudId, String pageId, Integer limit) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be empty");
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        log.info("Confluence attachments — cloudId={} pageId={} limit={}", cloudId, pageId, effectiveLimit);

        String raw = confluenceClient.getAttachments(token, cloudId, pageId, effectiveLimit);
        return parseAttachmentsResponse(raw);
    }

    /**
     * Retrieves details of a single Confluence space by its ID.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param spaceId the numeric Confluence space ID (must not be blank)
     * @return JSON object string with fields: id, key, name, type, status,
     *         authorId, createdAt, homepageId, description, url
     * @throws IllegalArgumentException     if {@code spaceId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String getSpace(String token, String cloudId, String spaceId) {
        if (spaceId == null || spaceId.isBlank()) {
            throw new IllegalArgumentException("spaceId must not be empty");
        }
        log.info("Confluence getSpace — cloudId={} spaceId={}", cloudId, spaceId);

        String raw = confluenceClient.getSpace(token, cloudId, spaceId.trim());
        return parseSpaceResponse(raw);
    }

    /**
     * Lists Confluence spaces for the tenant with optional type/status filters
     * and cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param type    optional space type: global|personal|collaboration|knowledge_base
     * @param status  optional space status: current|archived
     * @param limit   maximum spaces; clamped to [{@value DEFAULT_SPACES_LIMIT}, {@value MAX_SPACES_LIMIT}]
     * @param cursor  optional opaque pagination cursor from a previous call
     * @return JSON object string with {@code results} (array of spaces) and {@code nextCursor}
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String listSpaces(String token, String cloudId,
                             String type, String status, Integer limit, String cursor) {
        int effectiveLimit = (limit == null || limit <= 0)
                ? DEFAULT_SPACES_LIMIT
                : Math.min(limit, MAX_SPACES_LIMIT);

        log.info("Confluence listSpaces — cloudId={} type={} status={} limit={} cursor={} token={}",
                cloudId, type, status, effectiveLimit, cursor, token);

        String raw = confluenceClient.listSpaces(token, cloudId, type, status, effectiveLimit, cursor);

        String res =  parseSpacesListResponse(raw);
        return res;
    }

    /**
     * Returns the attachments for a Confluence page as a trimmed JSON array.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param pageId  numeric ID of the page whose attachments to fetch (must not be blank)
     * @param limit   maximum results; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @return JSON array string with fields: id, title, type, mediaType, fileSize, pageId, url, downloadLink, lastModified
     * @throws IllegalArgumentException     if {@code pageId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    /**
     * Returns the direct child pages of a Confluence page as a trimmed JSON array.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param pageId  numeric ID of the parent page (must not be blank)
     * @param limit   maximum results; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @return JSON array string with fields: id, title, spaceId, parentId, url, lastModified
     * @throws IllegalArgumentException     if {@code pageId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String getPageChildren(String token, String cloudId, String pageId, Integer limit) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be empty");
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        log.info("Confluence page children — cloudId={} pageId={} limit={}", cloudId, pageId, effectiveLimit);

        String raw = confluenceClient.getPageChildren(token, cloudId, pageId, effectiveLimit);
        return parsePageChildrenResponse(raw);
    }

    public String getAttachments(String token, String cloudId, String pageId, Integer limit) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be empty");
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        log.info("Confluence attachments — cloudId={} pageId={} limit={}", cloudId, pageId, effectiveLimit);

        String raw = confluenceClient.getAttachments(token, cloudId, pageId, effectiveLimit);
        return parseAttachmentsResponse(raw);
    }

    /**
     * Retrieves details of a single Confluence space by its ID.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param spaceId the numeric Confluence space ID (must not be blank)
     * @return JSON object string with fields: id, key, name, type, status,
     *         authorId, createdAt, homepageId, description, url
     * @throws IllegalArgumentException     if {@code spaceId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String getSpace(String token, String cloudId, String spaceId) {
        if (spaceId == null || spaceId.isBlank()) {
            throw new IllegalArgumentException("spaceId must not be empty");
        }
        log.info("Confluence getSpace — cloudId={} spaceId={}", cloudId, spaceId);

        String raw = confluenceClient.getSpace(token, cloudId, spaceId.trim());
        return parseSpaceResponse(raw);
    }

    /**
     * Lists Confluence spaces for the tenant with optional type/status filters
     * and cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param type    optional space type: global|personal|collaboration|knowledge_base
     * @param status  optional space status: current|archived
     * @param limit   maximum spaces; clamped to [{@value DEFAULT_SPACES_LIMIT}, {@value MAX_SPACES_LIMIT}]
     * @param cursor  optional opaque pagination cursor from a previous call
     * @return JSON object string with {@code results} (array of spaces) and {@code nextCursor}
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String listSpaces(String token, String cloudId,
                             String type, String status, Integer limit, String cursor) {
        int effectiveLimit = (limit == null || limit <= 0)
                ? DEFAULT_SPACES_LIMIT
                : Math.min(limit, MAX_SPACES_LIMIT);

        log.info("Confluence listSpaces — cloudId={} type={} status={} limit={} cursor={} token={}",
                cloudId, type, status, effectiveLimit, cursor, token);

        String raw = confluenceClient.listSpaces(token, cloudId, type, status, effectiveLimit, cursor);

        String res =  parseSpacesListResponse(raw);
        return res;
    }

    /**
     * Returns the attachments for a Confluence page as a trimmed JSON array.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param pageId  numeric ID of the page whose attachments to fetch (must not be blank)
     * @param limit   maximum results; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @return JSON array string with fields: id, title, type, mediaType, fileSize, pageId, url, downloadLink, lastModified
     * @throws IllegalArgumentException     if {@code pageId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    /**
     * Returns the direct child pages of a Confluence page as a trimmed JSON array.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param pageId  numeric ID of the parent page (must not be blank)
     * @param limit   maximum results; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @return JSON array string with fields: id, title, spaceId, parentId, url, lastModified
     * @throws IllegalArgumentException     if {@code pageId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String getPageChildren(String token, String cloudId, String pageId, Integer limit) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be empty");
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        log.info("Confluence page children — cloudId={} pageId={} limit={}", cloudId, pageId, effectiveLimit);

        String raw = confluenceClient.getPageChildren(token, cloudId, pageId, effectiveLimit);
        return parsePageChildrenResponse(raw);
    }

    public String getAttachments(String token, String cloudId, String pageId, Integer limit) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be empty");
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        log.info("Confluence attachments — cloudId={} pageId={} limit={}", cloudId, pageId, effectiveLimit);

        String raw = confluenceClient.getAttachments(token, cloudId, pageId, effectiveLimit);
        return parseAttachmentsResponse(raw);
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
     * Parses a Confluence v2 {@code /wiki/api/v2/pages/{id}/attachments} response body into a
     * simplified JSON array.
     *
     * <p>Expected v2 response shape:
     * <pre>
     * {
     *   "results": [
     *     {
     *       "id": "att123", "title": "file.pdf",
     *       "mediaType": "application/pdf", "fileSize": 12345,
     *       "webuiLink": "/wiki/download/attachments/...",
     *       "downloadLink": "/wiki/download/attachments/...?api=v2",
     *       "version": { "createdAt": "2024-01-01T00:00:00.000Z" },
     *       "pageId": "456"
     *     }
     *   ],
     *   "_links": { "base": "https://company.atlassian.net" }
     * }
     * </pre>
     */
    /**
     * Parses a Confluence v2 {@code /wiki/api/v2/pages/{id}/children} response body into a
     * simplified JSON array.
     *
     * <p>Expected v2 response shape:
     * <pre>
     * {
     *   "results": [
     *     {
     *       "id": "...", "title": "...", "spaceId": "...", "parentId": "...",
     *       "_links": { "webui": "/wiki/spaces/.../pages/..." },
     *       "version": { "createdAt": "2024-01-01T00:00:00.000Z" }
     *     }
     *   ],
     *   "_links": { "base": "https://company.atlassian.net" }
     * }
     * </pre>
     */
    private static String parsePageChildrenResponse(String responseBody) {
        try {
            JsonNode root    = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path("results");
            String   baseUrl = root.path("_links").path("base").asText("");
            ArrayNode output = OBJECT_MAPPER.createArrayNode();

            for (JsonNode result : results) {
                String id        = result.path("id").asText(null);
                String webuiPath = result.path("_links").path("webui").asText(null);
                String fullUrl   = (webuiPath != null && !webuiPath.isBlank())
                        ? baseUrl + webuiPath
                        : (id != null && !baseUrl.isBlank() ? baseUrl + "/wiki/pages/" + id : null);

                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put("id",           id);
                item.put("title",        result.path("title").asText(null));
                item.put("spaceId",      result.path("spaceId").asText(null));
                item.put("parentId",     result.path("parentPageId").asText(null));
                item.put("url",          fullUrl);
                item.put("lastModified", result.path("version").path("createdAt").asText(null));
                output.add(item);
            }

            return OBJECT_MAPPER.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence page children response", e);
        }
    }

    private static String parseAttachmentsResponse(String responseBody) {
        try {
            JsonNode root    = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path("results");
            String   baseUrl = root.path("_links").path("base").asText("");
            ArrayNode output = OBJECT_MAPPER.createArrayNode();

            for (JsonNode result : results) {
                String webuiLink   = result.path("webuiLink").asText(null);
                String downloadLink = result.path("downloadLink").asText(null);
                String fullUrl     = (webuiLink != null) ? baseUrl + webuiLink : null;

                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put("id",           result.path("id").asText(null));
                item.put("title",        result.path("title").asText(null));
                item.put("type",         "attachment");
                item.put("mediaType",    result.path("mediaType").asText(null));
                item.put("fileSize",     result.path("fileSize").asLong(0));
                item.put("pageId",       result.path("pageId").asText(null));
                item.put("url",          fullUrl);
                item.put("downloadLink", downloadLink != null ? baseUrl + downloadLink : null);
                item.put("lastModified", result.path("version").path("createdAt").asText(null));
                output.add(item);
            }

            return OBJECT_MAPPER.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence attachments response", e);
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

    /**
     * Parses the Confluence v2 {@code /wiki/api/v2/spaces/{id}} response body
     * into a simplified JSON object containing only the fields useful to the LLM.
     *
     * <p>Expected v2 response shape (abridged):
     * <pre>
     * {
     *   "id": "123",
     *   "key": "DEV",
     *   "name": "Developer Docs",
     *   "type": "global",
     *   "status": "current",
     *   "authorId": "...",
     *   "createdAt": "2024-01-01T00:00:00.000Z",
     *   "homepageId": "456",
     *   "description": {
     *     "plain": { "value": "...", "representation": "plain" }
     *   },
     *   "_links": { "webui": "/spaces/DEV" }
     * }
     * </pre>
     */
    private static String parseSpaceResponse(String responseBody) {
        try {
            JsonNode root  = OBJECT_MAPPER.readTree(responseBody);
            JsonNode links = root.path("_links");
            JsonNode desc  = root.path("description").path("plain").path("value");
            JsonNode excerpt = root.path("excerpt");
            JsonNode resultGlobalContainer = root.path("resultGlobalContainer");
            JsonNode excerpt = root.path("excerpt");
            JsonNode resultGlobalContainer = root.path("resultGlobalContainer");

            ObjectNode item = OBJECT_MAPPER.createObjectNode();
            item.put("id",          root.path("id").asText(null));
            item.put("key",         root.path("key").asText(null));
            item.put("name",        root.path("name").asText(null));
            item.put("type",        root.path("type").asText(null));
            item.put("status",      root.path("status").asText(null));
            item.put("authorId",    root.path("authorId").asText(null));
            item.put("createdAt",   root.path("createdAt").asText(null));
            item.put("homepageId",  root.path("homepageId").asText(null));
            item.put("description", desc.asText(null));
            item.put("excerpt",     excerpt.asText(null));
            item.set("resultGlobalContainer", resultGlobalContainer.isMissingNode() ? null : resultGlobalContainer);
            item.put("url",         links.path("webui").asText(null));

            return OBJECT_MAPPER.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence space response", e);
        }
    }

    /**
     * Parses the Confluence v2 {@code /wiki/api/v2/spaces} response body into a
     * simplified JSON object:
     * <pre>
     * { "results": [ {id, key, name, type, status, authorId, createdAt, homepageId, description, url}, ... ],
     *   "nextCursor": "..." | null }
     * </pre>
     */
    private static String parseSpacesListResponse(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path("results");
            ArrayNode arr = OBJECT_MAPPER.createArrayNode();

            for (JsonNode space : results) {
                JsonNode links = space.path("_links");
                JsonNode resultGlobalContainer = space.path("resultGlobalContainer");

                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put("id",          space.path("id").asText(null));
                item.put("key",         space.path("key").asText(null));
                item.put("name",        space.path("name").asText(null));
                item.put("type",        space.path("type").asText(null));
                item.put("status",      space.path("status").asText(null));
                item.set("resultGlobalContainer", resultGlobalContainer.isMissingNode() ? null : resultGlobalContainer);
                item.put("url",         links.path("webui").asText(null));
                arr.add(item);
            }

            ObjectNode out = OBJECT_MAPPER.createObjectNode();
            out.set("results", arr);
            String nextLink = root.path("_links").path("next").asText(null);
            out.put("nextCursor", extractCursor(nextLink));

            return OBJECT_MAPPER.writeValueAsString(out);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence spaces list response", e);
        }
    }

    /**
     * Extracts the opaque {@code cursor} query parameter from a Confluence
     * {@code _links.next} URL. Returns {@code null} if the link is absent or
     * does not contain a cursor.
     */
    private static String extractCursor(String nextLink) {
        if (nextLink == null || nextLink.isBlank()) return null;
        int q = nextLink.indexOf('?');
        if (q < 0) return null;
        for (String pair : nextLink.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && "cursor".equals(pair.substring(0, eq))) {
                return java.net.URLDecoder.decode(
                        pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * Parses the Confluence v2 {@code /wiki/api/v2/spaces/{id}} response body
     * into a simplified JSON object containing only the fields useful to the LLM.
     *
     * <p>Expected v2 response shape (abridged):
     * <pre>
     * {
     *   "id": "123",
     *   "key": "DEV",
     *   "name": "Developer Docs",
     *   "type": "global",
     *   "status": "current",
     *   "authorId": "...",
     *   "createdAt": "2024-01-01T00:00:00.000Z",
     *   "homepageId": "456",
     *   "description": {
     *     "plain": { "value": "...", "representation": "plain" }
     *   },
     *   "_links": { "webui": "/spaces/DEV" }
     * }
     * </pre>
     */
    private static String parseSpaceResponse(String responseBody) {
        try {
            JsonNode root  = OBJECT_MAPPER.readTree(responseBody);
            JsonNode links = root.path("_links");
            JsonNode desc  = root.path("description").path("plain").path("value");
            JsonNode excerpt = root.path("excerpt");
            JsonNode resultGlobalContainer = root.path("resultGlobalContainer");

            ObjectNode item = OBJECT_MAPPER.createObjectNode();
            item.put("id",          root.path("id").asText(null));
            item.put("key",         root.path("key").asText(null));
            item.put("name",        root.path("name").asText(null));
            item.put("type",        root.path("type").asText(null));
            item.put("status",      root.path("status").asText(null));
            item.put("createdAt",   root.path("createdAt").asText(null));
            item.put("homepageId",  root.path("homepageId").asText(null));
            item.put("description", desc.asText(null));
            item.put("excerpt",     excerpt.asText(null));
            item.set("resultGlobalContainer", resultGlobalContainer.isMissingNode() ? null : resultGlobalContainer);
            item.put("url",         links.path("webui").asText(null));

            return OBJECT_MAPPER.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence space response", e);
        }
    }

    /**
     * Parses the Confluence v2 {@code /wiki/api/v2/spaces} response body into a
     * simplified JSON object:
     * <pre>
     * { "results": [ {id, key, name, type, status, authorId, createdAt, homepageId, description, url}, ... ],
     *   "nextCursor": "..." | null }
     * </pre>
     */
    private static String parseSpacesListResponse(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path("results");
            ArrayNode arr = OBJECT_MAPPER.createArrayNode();

            for (JsonNode space : results) {
                JsonNode links = space.path("_links");
                JsonNode resultGlobalContainer = space.path("resultGlobalContainer");

                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put("id",          space.path("id").asText(null));
                item.put("key",         space.path("key").asText(null));
                item.put("name",        space.path("name").asText(null));
                item.put("type",        space.path("type").asText(null));
                item.put("status",      space.path("status").asText(null));
                item.set("resultGlobalContainer", resultGlobalContainer.isMissingNode() ? null : resultGlobalContainer);
                item.put("url",         links.path("webui").asText(null));
                arr.add(item);
            }

            ObjectNode out = OBJECT_MAPPER.createObjectNode();
            out.set("results", arr);
            String nextLink = root.path("_links").path("next").asText(null);
            out.put("nextCursor", extractCursor(nextLink));

            return OBJECT_MAPPER.writeValueAsString(out);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence spaces list response", e);
        }
    }

    /**
     * Extracts the opaque {@code cursor} query parameter from a Confluence
     * {@code _links.next} URL. Returns {@code null} if the link is absent or
     * does not contain a cursor.
     */
    private static String extractCursor(String nextLink) {
        if (nextLink == null || nextLink.isBlank()) return null;
        int q = nextLink.indexOf('?');
        if (q < 0) return null;
        for (String pair : nextLink.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && "cursor".equals(pair.substring(0, eq))) {
                return java.net.URLDecoder.decode(
                        pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * Parses the Confluence v2 {@code /wiki/api/v2/spaces/{id}} response body
     * into a simplified JSON object containing only the fields useful to the LLM.
     *
     * <p>Expected v2 response shape (abridged):
     * <pre>
     * {
     *   "id": "123",
     *   "key": "DEV",
     *   "name": "Developer Docs",
     *   "type": "global",
     *   "status": "current",
     *   "authorId": "...",
     *   "createdAt": "2024-01-01T00:00:00.000Z",
     *   "homepageId": "456",
     *   "description": {
     *     "plain": { "value": "...", "representation": "plain" }
     *   },
     *   "_links": { "webui": "/spaces/DEV" }
     * }
     * </pre>
     */
    private static String parseSpaceResponse(String responseBody) {
        try {
            JsonNode root  = OBJECT_MAPPER.readTree(responseBody);
            JsonNode links = root.path("_links");
            JsonNode desc  = root.path("description").path("plain").path("value");

            ObjectNode item = OBJECT_MAPPER.createObjectNode();
            item.put("id",          root.path("id").asText(null));
            item.put("key",         root.path("key").asText(null));
            item.put("name",        root.path("name").asText(null));
            item.put("type",        root.path("type").asText(null));
            item.put("status",      root.path("status").asText(null));
            item.put("authorId",    root.path("authorId").asText(null));
            item.put("createdAt",   root.path("createdAt").asText(null));
            item.put("homepageId",  root.path("homepageId").asText(null));
            item.put("description", desc.asText(null));
            item.put("excerpt",     excerpt.asText(null));
            item.set("resultGlobalContainer", resultGlobalContainer.isMissingNode() ? null : resultGlobalContainer);
            item.put("url",         links.path("webui").asText(null));

            return OBJECT_MAPPER.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence space response", e);
        }
    }

    /**
     * Parses the Confluence v2 {@code /wiki/api/v2/spaces} response body into a
     * simplified JSON object:
     * <pre>
     * { "results": [ {id, key, name, type, status, authorId, createdAt, homepageId, description, url}, ... ],
     *   "nextCursor": "..." | null }
     * </pre>
     */
    private static String parseSpacesListResponse(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path("results");
            ArrayNode arr = OBJECT_MAPPER.createArrayNode();

            for (JsonNode space : results) {
                JsonNode links = space.path("_links");
                JsonNode resultGlobalContainer = space.path("resultGlobalContainer");

                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put("id",          space.path("id").asText(null));
                item.put("key",         space.path("key").asText(null));
                item.put("name",        space.path("name").asText(null));
                item.put("type",        space.path("type").asText(null));
                item.put("status",      space.path("status").asText(null));
                item.set("resultGlobalContainer", resultGlobalContainer.isMissingNode() ? null : resultGlobalContainer);
                item.put("url",         links.path("webui").asText(null));
                arr.add(item);
            }

            ObjectNode out = OBJECT_MAPPER.createObjectNode();
            out.set("results", arr);
            String nextLink = root.path("_links").path("next").asText(null);
            out.put("nextCursor", extractCursor(nextLink));

            return OBJECT_MAPPER.writeValueAsString(out);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence spaces list response", e);
        }
    }

    /**
     * Extracts the opaque {@code cursor} query parameter from a Confluence
     * {@code _links.next} URL. Returns {@code null} if the link is absent or
     * does not contain a cursor.
     */
    private static String extractCursor(String nextLink) {
        if (nextLink == null || nextLink.isBlank()) return null;
        int q = nextLink.indexOf('?');
        if (q < 0) return null;
        for (String pair : nextLink.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && "cursor".equals(pair.substring(0, eq))) {
                return java.net.URLDecoder.decode(
                        pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
