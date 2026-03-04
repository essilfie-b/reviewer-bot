package com.amaliai.mcp.servers.sharepoint.service;

import com.amaliai.mcp.servers.sharepoint.client.DriveItemParser;
import com.amaliai.mcp.servers.sharepoint.client.SharePointGraphClient;
import com.amaliai.mcp.servers.sharepoint.exception.SharePointOperationException;
import com.amaliai.mcp.servers.sharepoint.extractor.SharePointContentExtractor;
import com.amaliai.mcp.servers.sharepoint.util.SharePointResponseUtil;
import com.amaliai.mcp.servers.sharepoint.validator.SharePointValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.amaliai.mcp.servers.sharepoint.SharePointConstants.*;

/**
 * Core business logic for the SharePoint MCP tools.
 * <p>
 * This layer orchestrates the flow between the HTTP client, validation,
 * content extraction, and response assembly.  It has no knowledge of how
 * results are delivered to callers (HTTP, MCP, etc.).
 * <p>
 * <b>Error contract:</b> validation failures and business-rule violations are
 * thrown as {@link IllegalArgumentException} so that the tool layer can convert
 * them to user-facing error responses without inspecting return values.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharePointService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SharePointGraphClient      graphClient;
    private final DriveItemParser            driveItemParser;
    private final SharePointContentExtractor contentExtractor;
    private final SharePointValidator        validator;
    private final SharePointResponseUtil     responseUtil;

    // -------------------------------------------------------------------------
    // Public API — one method per MCP tool
    // -------------------------------------------------------------------------

    /**
     * Lists all files at the root of the user's OneDrive.
     */
    public String listDocuments(String token) {
        String raw    = graphClient.fetchRootChildren(token);
        String parsed = driveItemParser.parse(raw, null, null);
        return responseUtil.trimResponse(parsed, MAX_RESPONSE_BYTES);
    }

    /**
     * Lists files and sub-folders inside a specific folder.
     *
     * @throws IllegalArgumentException if {@code folderId} is blank
     */
    public String getFolderContents(String token, String folderId) {
        if (folderId == null || folderId.isBlank()) {
            throw new IllegalArgumentException("folderId must not be empty");
        }
        String raw    = graphClient.fetchFolderChildren(token, folderId);
        String parsed = driveItemParser.parse(raw, null, null, true);
        return responseUtil.trimResponse(parsed, MAX_RESPONSE_BYTES);
    }

    /**
     * Searches the user's OneDrive and optionally filters by type, author, and date.
     *
     * @throws IllegalArgumentException if {@code query} is blank, {@code fileType} is unknown,
     *                                  or a date string is not valid ISO-8601
     */
    public String searchDocuments(String token, String query, String fileType,
                                  String author, String from, String to, Integer top) {

        String validationError = validator.validateSearchInputs(query, fileType);
        if (validationError != null) throw new IllegalArgumentException(validationError);

        int limit       = (top == null || top <= 0) ? DEFAULT_TOP : Math.min(top, MAX_TOP);
        String safeQuery = query.replace("'", "''");          // OData single-quote escape
        List<String> dateFilters = validator.buildDateFilters(from, to); // may throw IllegalArgumentException

        String raw    = graphClient.searchItems(token, safeQuery, limit, dateFilters);
        String parsed = driveItemParser.parse(raw, fileType, author);
        return responseUtil.trimResponse(parsed, MAX_RESPONSE_BYTES);
    }

    /**
     * Downloads a drive item and returns its extracted text content.
     *
     * @throws IllegalArgumentException for invalid inputs, unsupported types,
     *                                  oversized files, or when no text can be extracted
     */
    public String getDocumentContent(String token, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be empty");
        }

        // Step 1 — fetch metadata to validate type and size before downloading
        JsonNode metadata = parseJson(graphClient.fetchItemMetadata(token, itemId),
                "item metadata for itemId=" + itemId);
        String name      = metadata.path("name").asText("");
        long   sizeBytes = metadata.path("size").asLong(0L);
        String ext       = fileExtension(name);

        String typeError = validator.validateContentType(ext);
        if (typeError != null) throw new IllegalArgumentException(typeError);

        if (sizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "File is too large (" + (sizeBytes / (1_024 * 1_024)) + " MB). Maximum is 10 MB.");
        }

        // Step 2 — download (graphClient follows the 302 → CDN redirect automatically)
        byte[] bytes = graphClient.downloadItemContent(token, itemId);
        if (bytes == null || bytes.length == 0) {
            log.warn("Downloaded file '{}' (itemId={}) returned an empty body", name, itemId);
            throw new IllegalArgumentException("Downloaded file is empty");
        }
        log.info("Downloaded '{}' (itemId={}) — {} bytes", name, itemId, bytes.length);

        // Step 3 — extract text
        String rawText = contentExtractor.extractText(bytes, name);
        log.info("Extracted {} characters from '{}' (stored size: {} bytes)", rawText.length(), name, sizeBytes);

        if (rawText.isBlank()) {
            throw new IllegalArgumentException(
                    "No text could be extracted from '" + name + "'. "
                    + "The file may be a scanned image (no text layer) or the format is unsupported.");
        }

        // Step 4 — truncate if necessary and build response
        boolean truncated = rawText.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES;
        String content = truncated ? responseUtil.trimResponse(rawText, MAX_CONTENT_BYTES) : rawText;

        log.info("Content ready for '{}': {} bytes, truncated={}",
                name, content.getBytes(StandardCharsets.UTF_8).length, truncated);

        ObjectNode result = OBJECT_MAPPER.createObjectNode();
        result.put("name",      name);
        result.put("fileType",  ext);
        result.put("sizeBytes", sizeBytes);
        result.put("truncated", truncated);
        result.put("content",   content);
        try {
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new SharePointOperationException("Failed to serialize document content response", e);
        }
    }

    /**
     * Fetches and returns rich metadata for a single drive item.
     *
     * @throws IllegalArgumentException if {@code itemId} is blank
     */
    public String getFileMetadata(String token, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be empty");
        }

        String raw = graphClient.fetchItemFullMetadata(token, itemId);
        JsonNode item = parseJson(raw, "item metadata for itemId=" + itemId);

        String name = item.path("name").asText("");
        int dot = name.lastIndexOf('.');
        String fileType = dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";

        ObjectNode result = OBJECT_MAPPER.createObjectNode();
        result.put("name",                 name);
        result.put("fileType",             fileType.isEmpty() ? null : fileType);
        result.put("mimeType",             item.path("file").path("mimeType").asText(null));
        result.put("sizeBytes",            item.path("size").asLong(0L));
        result.put("webUrl",               item.path("webUrl").asText(null));
        result.put("createdBy",            item.path("createdBy").path("user").path("displayName").asText(null));
        result.put("createdDateTime",      item.path("createdDateTime").asText(null));
        result.put("lastModifiedBy",       item.path("lastModifiedBy").path("user").path("displayName").asText(null));
        result.put("lastModifiedDateTime", item.path("lastModifiedDateTime").asText(null));

        try {
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new SharePointOperationException("Failed to serialize file metadata response", e);
        }
    }

    // -------------------------------------------------------------------------
    // SharePoint Sites operations
    // -------------------------------------------------------------------------

    /**
     * Lists SharePoint sites the user has access to.
     *
     * @param top maximum number of sites to return (default 20, max 50)
     */
    public String listSites(String token, Integer top) {
        int limit = (top == null || top <= 0) ? DEFAULT_TOP : Math.min(top, MAX_TOP);
        String raw = graphClient.fetchUserSites(token, limit);
        String parsed = parseSitesResponse(raw);
        return responseUtil.trimResponse(parsed, MAX_RESPONSE_BYTES);
    }

    /**
     * Gets details of a specific SharePoint site.
     *
     * @throws IllegalArgumentException if {@code siteId} is blank
     */
    public String getSiteDetails(String token, String siteId) {
        if (siteId == null || siteId.isBlank()) {
            throw new IllegalArgumentException("siteId must not be empty");
        }
        String raw = graphClient.fetchSiteDetails(token, siteId);
        String parsed = parseSiteDetailsResponse(raw);
        return responseUtil.trimResponse(parsed, MAX_RESPONSE_BYTES);
    }

    /**
     * Lists document libraries in a SharePoint site.
     *
     * @param siteId the SharePoint site ID
     * @param top    maximum number of libraries to return (default 20, max 50)
     * @throws IllegalArgumentException if {@code siteId} is blank
     */
    public String listLibraries(String token, String siteId, Integer top) {
        if (siteId == null || siteId.isBlank()) {
            throw new IllegalArgumentException("siteId must not be empty");
        }
        int limit = (top == null || top <= 0) ? DEFAULT_TOP : Math.min(top, MAX_TOP);
        String raw = graphClient.fetchSiteLibraries(token, siteId, limit);
        String parsed = parseLibrariesResponse(raw);
        return responseUtil.trimResponse(parsed, MAX_RESPONSE_BYTES);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String fileExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    /** Parses a JSON string, wrapping any {@link JsonProcessingException} as a {@link SharePointOperationException}. */
    private static JsonNode parseJson(String json, String context) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new SharePointOperationException("Failed to parse JSON for " + context, e);
        }
    }

    /** Parses the Graph API sites response into a simplified JSON array. */
    private String parseSitesResponse(String responseBody) {
        try {
            JsonNode sites = OBJECT_MAPPER.readTree(responseBody).path("value");
            ArrayNode results = OBJECT_MAPPER.createArrayNode();

            for (JsonNode site : sites) {
                ObjectNode parsed = OBJECT_MAPPER.createObjectNode();
                parsed.put("id", site.path("id").asText(null));
                parsed.put("name", site.path("name").asText(null));
                parsed.put("displayName", site.path("displayName").asText(null));
                parsed.put("webUrl", site.path("webUrl").asText(null));
                parsed.put("description", site.path("description").asText(null));
                parsed.put("createdDateTime", site.path("createdDateTime").asText(null));
                parsed.put("lastModifiedDateTime", site.path("lastModifiedDateTime").asText(null));
                results.add(parsed);
            }

            return OBJECT_MAPPER.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            throw new SharePointOperationException("Failed to parse Graph API sites response", e);
        }
    }

    /** Parses the Graph API site details response into a simplified JSON object. */
    private String parseSiteDetailsResponse(String responseBody) {
        try {
            JsonNode site = OBJECT_MAPPER.readTree(responseBody);
            ObjectNode result = OBJECT_MAPPER.createObjectNode();
            result.put("id", site.path("id").asText(null));
            result.put("name", site.path("name").asText(null));
            result.put("displayName", site.path("displayName").asText(null));
            result.put("webUrl", site.path("webUrl").asText(null));
            result.put("description", site.path("description").asText(null));
            result.put("createdDateTime", site.path("createdDateTime").asText(null));
            result.put("lastModifiedDateTime", site.path("lastModifiedDateTime").asText(null));
            result.put("isRoot", site.has("root"));
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new SharePointOperationException("Failed to parse Graph API site details response", e);
        }
    }

    /** Parses the Graph API drives (libraries) response into a simplified JSON array. */
    private String parseLibrariesResponse(String responseBody) {
        try {
            JsonNode drives = OBJECT_MAPPER.readTree(responseBody).path("value");
            ArrayNode results = OBJECT_MAPPER.createArrayNode();

            for (JsonNode drive : drives) {
                ObjectNode parsed = OBJECT_MAPPER.createObjectNode();
                parsed.put("id", drive.path("id").asText(null));
                parsed.put("name", drive.path("name").asText(null));
                parsed.put("description", drive.path("description").asText(null));
                parsed.put("webUrl", drive.path("webUrl").asText(null));
                parsed.put("driveType", drive.path("driveType").asText(null));
                parsed.put("createdDateTime", drive.path("createdDateTime").asText(null));
                parsed.put("lastModifiedDateTime", drive.path("lastModifiedDateTime").asText(null));

                // Include quota information if available
                JsonNode quota = drive.path("quota");
                if (!quota.isMissingNode()) {
                    ObjectNode quotaNode = OBJECT_MAPPER.createObjectNode();
                    quotaNode.put("total", quota.path("total").asLong(0L));
                    quotaNode.put("used", quota.path("used").asLong(0L));
                    quotaNode.put("remaining", quota.path("remaining").asLong(0L));
                    quotaNode.put("state", quota.path("state").asText(null));
                    parsed.set("quota", quotaNode);
                }

                results.add(parsed);
            }

            return OBJECT_MAPPER.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            throw new SharePointOperationException("Failed to parse Graph API libraries response", e);
        }
    }
}