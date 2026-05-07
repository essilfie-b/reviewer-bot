package com.amaliai.mcp.servers.confluence.util;

import com.amaliai.mcp.servers.confluence.dto.SpaceInfo;
import com.amaliai.mcp.servers.confluence.exception.ConfluenceOperationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;


@Component
public class ConfluenceServiceUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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
    private static final String FIELD_STATUS        = "status";
    private static final String FIELD_DESCRIPTION   = "description";
    private static final String FIELD_AUTHOR_ID    = "authorId";
    private static final String FIELD_HOMEPAGE_ID  = "homepageId";
    // JSON field names — output object keys
    private static final String FIELD_SPACE_KEY     = "spaceKey";
    private static final String FIELD_SPACE_NAME    = "spaceName";
    private static final String FIELD_SPACE_ID      = "spaceId";
    private static final String FIELD_PARENT_ID     = "parentId";
    private static final String FIELD_URL           = "url";
    private static final String FIELD_PARENT_TITLE  = "parentTitle";
    private static final String FIELD_TRUNCATED     = "truncated";
    private static final String TYPE_PAGE           = "page";
    private static final String FIELD_MEDIA_TYPE    = "mediaType";
    private static final String FIELD_FILE_SIZE     = "fileSize";

    /**
     * Builds a CQL expression for a full-text page search, optionally scoped to
     * a specific space.
     * <p>
     * Double-quotes inside the query are escaped to prevent CQL injection.
     */
    public static String buildCql(String query, String spaceKey) {
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
    public static String parseSearchResponse(String responseBody) {
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
    public static String parsePageChildrenResponse(String responseBody) {
        try {
            JsonNode root    = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path(FIELD_RESULTS);
            String   baseUrl = root.path(FIELD_LINKS).path(FIELD_BASE).asText("");
            ArrayNode output = OBJECT_MAPPER.createArrayNode();

            for (JsonNode result : results) {
                String id        = result.path(FIELD_ID).asText(null);
                String webuiPath = result.path(FIELD_LINKS).path(FIELD_WEBUI).asText(null);
                String fullUrl   = (webuiPath != null && !webuiPath.isBlank())
                        ? baseUrl + webuiPath
                        : (id != null && !baseUrl.isBlank() ? baseUrl + "/wiki/pages/" + id : null);

                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put(FIELD_ID,           id);
                item.put(FIELD_TITLE,        result.path(FIELD_TITLE).asText(null));
                item.put(FIELD_SPACE_ID,     result.path(FIELD_SPACE_ID).asText(null));
                item.put(FIELD_PARENT_ID,    result.path(FIELD_PARENT_ID).asText(null));
                item.put(FIELD_URL,          fullUrl);
                item.put(FIELD_LAST_MODIFIED, result.path(FIELD_VERSION).path(FIELD_CREATED_AT).asText(null));
                output.add(item);
            }

            return OBJECT_MAPPER.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence page children response", e);
        }
    }

    /**
     * Parses a Confluence v2 {@code /wiki/api/v2/pages/{id}/attachments} response body into a
     * simplified JSON array.
     * @param responseBody
     * @return
     */

    public static String parseAttachmentsResponse(String responseBody) {
        try {
            JsonNode root    = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path(FIELD_RESULTS);
            String   baseUrl = root.path(FIELD_LINKS).path(FIELD_BASE).asText("");
            ArrayNode output = OBJECT_MAPPER.createArrayNode();

            for (JsonNode result : results) {
                String webuiLink   = result.path("webuiLink").asText(null);
                String downloadLink = result.path("downloadLink").asText(null);
                String fullUrl     = (webuiLink != null) ? baseUrl + webuiLink : null;

                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put(FIELD_ID,           result.path(FIELD_ID).asText(null));
                item.put(FIELD_TITLE,        result.path(FIELD_TITLE).asText(null));
                item.put(FIELD_TYPE,         "attachment");
                item.put(FIELD_MEDIA_TYPE,    result.path(FIELD_MEDIA_TYPE).asText(null));
                item.put(FIELD_FILE_SIZE,     result.path(FIELD_FILE_SIZE).asLong(0));
                item.put(FIELD_PARENT_ID,       result.path(FIELD_PARENT_ID).asText(null));
                item.put(FIELD_URL,          fullUrl);
                item.put("downloadLink", downloadLink != null ? baseUrl + downloadLink : null);
                item.put(FIELD_LAST_MODIFIED, result.path(FIELD_VERSION).path(FIELD_CREATED_AT).asText(null));
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
     * @param responseBody raw JSON from the spaces endpoint
     * @param fallbackKey  the key that was queried, used in the error message if not found
     * @throws ConfluenceOperationException if the space is absent from the results or the JSON cannot be parsed
     */
    public static SpaceInfo parseSpaceResult(String responseBody, String fallbackKey) {
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
    public static String parseV2SearchResponse(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path(FIELD_RESULTS);
            ArrayNode output = OBJECT_MAPPER.createArrayNode();

            for (JsonNode result : results) {
                JsonNode content = result.path(FIELD_CONTENT);
                JsonNode links   = content.path(FIELD_LINKS);

                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put(FIELD_ID,           content.path(FIELD_ID).asText(null));
                item.put(FIELD_TITLE,        content.path(FIELD_TITLE).asText(null));
                item.put(FIELD_TYPE,         content.path(FIELD_TYPE).asText(null));
                item.put(FIELD_SPACE_ID,     content.path(FIELD_SPACE_ID).asText(null));
                item.put(FIELD_SPACE_NAME,   result.path("resultGlobalContainer").path(FIELD_TITLE).asText(null));
                item.put(FIELD_URL,          links.path(FIELD_WEBUI).asText(null));
                item.put(FIELD_EXCERPT,      result.path(FIELD_EXCERPT).asText(null));
                item.put(FIELD_LAST_MODIFIED, result.path(FIELD_LAST_MODIFIED).asText(null));
                output.add(item);
            }

            return OBJECT_MAPPER.writeValueAsString(output);
        } catch (JsonProcessingException e) {
            throw new ConfluenceOperationException("Failed to parse Confluence v2 response", e);
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
    public static ObjectNode buildV2PageNode(JsonNode page, String spaceKey, String spaceName, String baseUrl) {
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
    public static String extractSpaceKeyFromWebuiUrl(String webuiPath) {
        if (webuiPath == null || webuiPath.isBlank()) return null;
        int spacesIdx = webuiPath.indexOf("/spaces/");
        if (spacesIdx < 0) return null;
        String after    = webuiPath.substring(spacesIdx + 8); // skip "/spaces/"
        int    slashIdx = after.indexOf('/');
        return slashIdx < 0 ? after : after.substring(0, slashIdx);
    }

    /** Parses a single v2 page metadata response (no body content). */
    public static String parseV2PageResponse(String responseBody) {
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
    public static String parseV2PageContentResponse(String responseBody) {
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
    public static String parsePagesListResponse(String responseBody, String spaceKey, String spaceName) {
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
    public static String stripHtml(String html) {
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
    public static String parseSpaceResponse(String responseBody) {
        try {
            JsonNode root  = OBJECT_MAPPER.readTree(responseBody);
            JsonNode links = root.path(FIELD_LINKS);
            JsonNode desc  = root.path(FIELD_DESCRIPTION).path("plain").path(FIELD_VALUE);
            JsonNode excerpt = root.path(FIELD_EXCERPT);
            JsonNode resultGlobalContainer = root.path("resultGlobalContainer");

            ObjectNode item = OBJECT_MAPPER.createObjectNode();
            item.put(FIELD_ID,          root.path(FIELD_ID).asText(null));
            item.put(FIELD_KEY,         root.path(FIELD_KEY).asText(null));
            item.put(FIELD_NAME,        root.path(FIELD_NAME).asText(null));
            item.put(FIELD_TYPE,        root.path(FIELD_TYPE).asText(null));
            item.put(FIELD_STATUS,      root.path(FIELD_STATUS).asText(null));
            item.put(FIELD_AUTHOR_ID,    root.path(FIELD_AUTHOR_ID).asText(null));
            item.put(FIELD_CREATED_AT,   root.path(FIELD_CREATED_AT).asText(null));
            item.put(FIELD_HOMEPAGE_ID,  root.path(FIELD_HOMEPAGE_ID).asText(null));
            item.put(FIELD_DESCRIPTION, desc.asText(null));
            item.put(FIELD_EXCERPT,     excerpt.asText(null));
            item.set("resultGlobalContainer", resultGlobalContainer.isMissingNode() ? null : resultGlobalContainer);
            String webui   = links.path(FIELD_WEBUI).asText(null);
            String base    = links.path(FIELD_BASE).asText(null);
            String fullUrl = (base != null && webui != null) ? base + webui : webui;
            item.put(FIELD_URL, fullUrl);

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
    public static String parseSpacesListResponse(String responseBody) {
        try {
            JsonNode root    = OBJECT_MAPPER.readTree(responseBody);
            JsonNode results = root.path(FIELD_RESULTS);
            String   baseUrl = root.path(FIELD_LINKS).path(FIELD_BASE).asText(null);
            ArrayNode arr    = OBJECT_MAPPER.createArrayNode();

            for (JsonNode space : results) {
                JsonNode links  = space.path(FIELD_LINKS);
                JsonNode resultGlobalContainer = space.path("resultGlobalContainer");

                String webui   = links.path(FIELD_WEBUI).asText(null);
                String fullUrl = (baseUrl != null && webui != null) ? baseUrl + webui : webui;

                ObjectNode item = OBJECT_MAPPER.createObjectNode();
                item.put(FIELD_ID,     space.path(FIELD_ID).asText(null));
                item.put(FIELD_KEY,    space.path(FIELD_KEY).asText(null));
                item.put(FIELD_NAME,   space.path(FIELD_NAME).asText(null));
                item.put(FIELD_TYPE,   space.path(FIELD_TYPE).asText(null));
                item.put(FIELD_STATUS, space.path(FIELD_STATUS).asText(null));
                item.set("resultGlobalContainer", resultGlobalContainer.isMissingNode() ? null : resultGlobalContainer);
                item.put(FIELD_URL,    fullUrl);
                arr.add(item);
            }

            ObjectNode out = OBJECT_MAPPER.createObjectNode();
            out.set(FIELD_RESULTS, arr);
            String nextLink = root.path(FIELD_LINKS).path("next").asText(null);
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
    public static String extractCursor(String nextLink) {
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
