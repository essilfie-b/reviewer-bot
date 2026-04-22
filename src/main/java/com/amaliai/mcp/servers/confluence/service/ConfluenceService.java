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
 * Business logic for Confluence MCP tools.
 * <p>
 * Responsible for input validation, CQL query construction, delegating HTTP
 * calls to {@link ConfluenceGraphClient}, and shaping raw API responses into
 * clean JSON for the LLM.
 * <p>
 * <b>Error contract:</b> validation failures throw {@link IllegalArgumentException};
 * API or serialisation failures throw {@link ConfluenceOperationException}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final int DEFAULT_LIMIT    = 20;
    private static final int MAX_LIMIT        = 50;
    private static final int MAX_QUERY_LENGTH = 1_000;

    /**
     * Maximum number of characters returned in a page body.
     * Confluence pages can be very large; this keeps LLM context manageable.
     */
    private static final int MAX_CONTENT_CHARS = 100_000;

    // JSON field names — Confluence API response paths
    private static final String FIELD_RESULTS       = "results";
    private static final String FIELD_LINKS         = "_links";
    private static final String FIELD_WEBUI         = "webui";
    private static final String FIELD_BASE          = "base";
    private static final String FIELD_ID            = "id";
    private static final String FIELD_TITLE         = "title";
    private static final String FIELD_TYPE          = "type";
    private static final String FIELD_KEY           = "key";
    private static final String FIELD_NAME          = "name";
    private static final String FIELD_SPACE         = "space";
    private static final String FIELD_CONTENT       = "content";
    private static final String FIELD_EXCERPT       = "excerpt";
    private static final String FIELD_LAST_MODIFIED = "lastModified";
    private static final String FIELD_VERSION       = "version";
    private static final String FIELD_CREATED_AT    = "createdAt";
    private static final String FIELD_BODY          = "body";
    private static final String FIELD_VALUE         = "value";
    // JSON field names — output object keys
    private static final String FIELD_SPACE_KEY     = "spaceKey";
    private static final String FIELD_SPACE_NAME    = "spaceName";
    private static final String FIELD_URL           = "url";
    private static final String FIELD_PARENT_TITLE  = "parentTitle";
    private static final String FIELD_TRUNCATED     = "truncated";
    private static final String TYPE_PAGE           = "page";

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
     * Fetches metadata for a single Confluence page by its ID (v2 API).
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param pageId  the Confluence page ID (must not be blank)
     * @return JSON object with fields: id, title, type, spaceKey, url, lastModified
     * @throws IllegalArgumentException     if {@code pageId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String getPage(String token, String cloudId, String pageId) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be empty");
        }
        log.info("Confluence getPage — cloudId={} pageId={}", cloudId, pageId);
        return parseV2PageResponse(confluenceClient.getPage(token, cloudId, pageId));
    }

    /**
     * Fetches metadata and full text content for a single Confluence page (v2 API).
     * The page body is stripped of HTML and truncated at {@value MAX_CONTENT_CHARS}
     * characters if the page is very long.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param pageId  the Confluence page ID (must not be blank)
     * @return JSON object with metadata fields plus {@code content} and {@code truncated}
     * @throws IllegalArgumentException     if {@code pageId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String getPageContent(String token, String cloudId, String pageId) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be empty");
        }
        log.info("Confluence getPageContent — cloudId={} pageId={}", cloudId, pageId);
        return parseV2PageContentResponse(confluenceClient.getPageWithContent(token, cloudId, pageId));
    }

    /**
     * Lists pages in a Confluence space (v2 API, two-step).
     * Step 1: resolves the human-readable {@code spaceKey} to a numeric space ID and display name.
     * Step 2: lists current pages in that space.
     *
     * @param token    the user's Confluence access token
     * @param cloudId  the Atlassian cloud ID for the user's tenant
     * @param spaceKey the space key to list pages from (must not be blank)
     * @param limit    maximum results; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @return JSON array of page objects with fields: id, title, type, spaceKey, spaceName, url, lastModified
     * @throws IllegalArgumentException     if {@code spaceKey} is blank
     * @throws ConfluenceOperationException if the space is not found or the API response cannot be parsed
     */
    public String listPages(String token, String cloudId, String spaceKey, Integer limit) {
        if (spaceKey == null || spaceKey.isBlank()) {
            throw new IllegalArgumentException("spaceKey must not be empty");
        }
        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        log.info("Confluence listPages — cloudId={} spaceKey={} limit={}", cloudId, spaceKey, effectiveLimit);

        SpaceInfo space = parseSpaceResult(
                confluenceClient.getSpaceByKey(token, cloudId, spaceKey), spaceKey);

        return parsePagesListResponse(
                confluenceClient.listPagesBySpaceId(token, cloudId, space.id(), effectiveLimit),
                space.key(), space.name());
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
            JsonNode root    = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path(FIELD_RESULTS);
            String   baseUrl = root.path(FIELD_LINKS).path(FIELD_BASE).asText("");
            ArrayNode output = OBJECT_MAPPER.createArrayNode();

            for (JsonNode result : results) {
                JsonNode content = result.path(FIELD_CONTENT);
                JsonNode space   = content.path(FIELD_SPACE);
                JsonNode links   = content.path(FIELD_LINKS);

                String webuiPath = links.path(FIELD_WEBUI).asText(null);
                String fullUrl   = (webuiPath != null) ? baseUrl + webuiPath : null;

                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put(FIELD_ID,            content.path(FIELD_ID).asText(null));
                item.put(FIELD_TITLE,         content.path(FIELD_TITLE).asText(null));
                item.put(FIELD_TYPE,          content.path(FIELD_TYPE).asText(null));
                item.put(FIELD_SPACE_KEY,     space.path(FIELD_KEY).asText(null));
                item.put(FIELD_SPACE_NAME,    space.path(FIELD_NAME).asText(null));
                item.put(FIELD_URL,           fullUrl);
                item.put(FIELD_EXCERPT,       result.path(FIELD_EXCERPT).asText(null));
                item.put(FIELD_LAST_MODIFIED, result.path(FIELD_LAST_MODIFIED).asText(null));
                output.add(item);
            }

            return OBJECT_MAPPER.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence search response", e);
        }
    }

    /**
     * Parses a {@code GET /wiki/api/v2/spaces?keys=} response and returns the first result.
     * The v2 page endpoints do not embed the space name or key, so this lookup bridges
     * that gap for the {@code listPages} two-step flow.
     *
     * @param responseBody raw JSON from the spaces endpoint
     * @param fallbackKey  the key that was queried, used in the error message if not found
     * @throws ConfluenceOperationException if the space is absent from the results or the JSON cannot be parsed
     */
    private static SpaceInfo parseSpaceResult(String responseBody, String fallbackKey) {
        try {
            JsonNode root    = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path(FIELD_RESULTS);
            if (!results.isArray() || results.isEmpty()) {
                throw new ConfluenceOperationException("No Confluence space found for key: " + fallbackKey, null);
            }
            JsonNode space = results.get(0);
            return new SpaceInfo(
                    space.path(FIELD_ID).asText(null),
                    space.path(FIELD_KEY).asText(fallbackKey),
                    space.path(FIELD_NAME).asText(null)
            );
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence space response", e);
        }
    }

    /**
     * Builds a consistent JSON object for a single v2 page.
     * <p>
     * The v2 API does not embed {@code space} or {@code ancestors} inside the page object,
     * so {@code spaceKey} and {@code spaceName} are supplied by the caller.
     * {@code parentTitle} is always {@code null} because v2 only exposes the parent's numeric ID.
     *
     * @param page      the page JSON node from a v2 API response
     * @param spaceKey  the space key — for single-page lookups this is extracted from {@code _links.webui}
     * @param spaceName the space display name — {@code null} for single-page lookups
     * @param baseUrl   overrides the base URL (used by list responses where {@code base} lives at the root level,
     *                  not inside each page node); pass {@code null} to use the page's own {@code _links.base}
     */
    private static ObjectNode buildV2PageNode(JsonNode page, String spaceKey, String spaceName, String baseUrl) {
        JsonNode links   = page.path(FIELD_LINKS);
        String   webui   = links.path(FIELD_WEBUI).asText(null);
        String   base    = baseUrl != null ? baseUrl : links.path(FIELD_BASE).asText(null);
        String   fullUrl = (base != null && webui != null) ? base + webui : webui;

        ObjectNode item = OBJECT_MAPPER.createObjectNode();
        item.put(FIELD_ID,            page.path(FIELD_ID).asText(null));
        item.put(FIELD_TITLE,         page.path(FIELD_TITLE).asText(null));
        item.put(FIELD_TYPE,          TYPE_PAGE);
        item.put(FIELD_SPACE_KEY,     spaceKey);
        item.put(FIELD_SPACE_NAME,    spaceName);
        item.put(FIELD_URL,           fullUrl);
        item.put(FIELD_PARENT_TITLE,  (String) null);
        item.put(FIELD_LAST_MODIFIED, page.path(FIELD_VERSION).path(FIELD_CREATED_AT).asText(null));
        return item;
    }

    /**
     * Extracts the space key from a v2 {@code _links.webui} path.
     * The path follows the pattern {@code /spaces/{key}/pages/{id}}, so the key
     * sits between {@code /spaces/} and the next slash.
     * Returns {@code null} if the URL does not match the expected pattern.
     */
    private static String extractSpaceKeyFromWebuiUrl(String webuiPath) {
        if (webuiPath == null || webuiPath.isBlank()) return null;
        int spacesIdx = webuiPath.indexOf("/spaces/");
        if (spacesIdx < 0) return null;
        String after    = webuiPath.substring(spacesIdx + 8); // skip "/spaces/"
        int    slashIdx = after.indexOf('/');
        return slashIdx < 0 ? after : after.substring(0, slashIdx);
    }

    /** Parses a single v2 page metadata response (no body content). */
    private static String parseV2PageResponse(String responseBody) {
        try {
            JsonNode root   = OBJECT_MAPPER.readTree(responseBody);
            String spaceKey = extractSpaceKeyFromWebuiUrl(root.path(FIELD_LINKS).path(FIELD_WEBUI).asText(null));
            return OBJECT_MAPPER.writeValueAsString(buildV2PageNode(root, spaceKey, null, null));
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence page response", e);
        }
    }

    /**
     * Parses a single v2 page response that includes the body.
     * Strips HTML from {@code body.value} and truncates at {@value MAX_CONTENT_CHARS} characters.
     * <p>
     * In the v2 API the rendered body lives at {@code body.value}, unlike the v1 API where
     * it was nested at {@code body.view.value}.
     */
    private static String parseV2PageContentResponse(String responseBody) {
        try {
            JsonNode root   = OBJECT_MAPPER.readTree(responseBody);
            String spaceKey = extractSpaceKeyFromWebuiUrl(root.path(FIELD_LINKS).path(FIELD_WEBUI).asText(null));
            ObjectNode item = buildV2PageNode(root, spaceKey, null, null);

            String  plainText = stripHtml(root.path(FIELD_BODY).path(FIELD_VALUE).asText(""));
            boolean truncated = plainText.length() > MAX_CONTENT_CHARS;

            item.put(FIELD_CONTENT,   truncated ? plainText.substring(0, MAX_CONTENT_CHARS) : plainText);
            item.put(FIELD_TRUNCATED, truncated);

            return OBJECT_MAPPER.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence page content response", e);
        }
    }

    /**
     * Parses a v2 {@code GET /wiki/api/v2/pages?space-id=} response into a JSON array.
     * Each result is a direct page object (not wrapped in a {@code content} field as in search).
     * The base URL is taken from the root {@code _links.base} and passed down to each page node.
     */
    private static String parsePagesListResponse(String responseBody, String spaceKey, String spaceName) {
        try {
            JsonNode  root    = OBJECT_MAPPER.readTree(responseBody);
            String    baseUrl = root.path(FIELD_LINKS).path(FIELD_BASE).asText(null);
            ArrayNode output  = OBJECT_MAPPER.createArrayNode();

            for (JsonNode page : root.path(FIELD_RESULTS)) {
                output.add(buildV2PageNode(page, spaceKey, spaceName, baseUrl));
            }

            return OBJECT_MAPPER.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence pages list response", e);
        }
    }

    /**
     * Strips HTML tags and decodes common HTML entities from a Confluence
     * rendered body, producing plain text suitable for LLM consumption.
     */
    private static String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        return html
                .replaceAll("<[^>]+>", " ")
                .replace("&amp;",  "&")
                .replace("&lt;",   "<")
                .replace("&gt;",   ">")
                .replace("&nbsp;", " ")
                .replace("&quot;", "\"")
                .replace("&#39;",  "'")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private record SpaceInfo(String id, String key, String name) {}

}
