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
    private static final String METADATA_CONTEXT_PREFIX = "item metadata for itemId=";
    private static final String FIELD_WEB_URL = "webUrl";
    private static final String FIELD_FILE_TYPE = "fileType";
    private static final String FIELD_SIZE_BYTES = "sizeBytes";
    private static final String FIELD_MIME_TYPE = "mimeType";
    private static final String FIELD_DISPLAY_NAME = "displayName";
    private static final String FIELD_CREATED_DATE_TIME = "createdDateTime";
    private static final String FIELD_LAST_MODIFIED_DATE_TIME = "lastModifiedDateTime";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";

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
        validateItemId(itemId);

        ExtractedDocumentMetadata metadata = fetchDocumentMetadata(token, itemId);
        validateDocumentForExtraction(metadata);

        byte[] bytes = downloadDocumentContent(token, itemId, metadata.name());
        String rawText = extractTextOrThrow(bytes, metadata.name(), metadata.sizeBytes());

        boolean truncated = isTruncated(rawText);
        String content = truncated ? responseUtil.trimResponse(rawText, MAX_CONTENT_BYTES) : rawText;

        log.info("Content ready for '{}': {} bytes, truncated={}",
                metadata.name(), content.getBytes(StandardCharsets.UTF_8).length, truncated);

        return buildDocumentContentResponse(metadata, content, truncated);
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
        JsonNode item = parseJson(raw, METADATA_CONTEXT_PREFIX + itemId);

        String name = item.path("name").asText("");
        int dot = name.lastIndexOf('.');
        String fileType = dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";

        ObjectNode result = OBJECT_MAPPER.createObjectNode();
        result.put(FIELD_NAME,             name);
        result.put(FIELD_FILE_TYPE,         fileType.isEmpty() ? null : fileType);
        result.put(FIELD_MIME_TYPE,         item.path("file").path(FIELD_MIME_TYPE).asText(null));
        result.put(FIELD_SIZE_BYTES,        item.path("size").asLong(0L));
        result.put(FIELD_WEB_URL,           item.path(FIELD_WEB_URL).asText(null));
        result.put("createdBy",            item.path("createdBy").path("user").path(FIELD_DISPLAY_NAME).asText(null));
        result.put(FIELD_CREATED_DATE_TIME, item.path(FIELD_CREATED_DATE_TIME).asText(null));
        result.put("lastModifiedBy",       item.path("lastModifiedBy").path("user").path(FIELD_DISPLAY_NAME).asText(null));
        result.put(FIELD_LAST_MODIFIED_DATE_TIME, item.path(FIELD_LAST_MODIFIED_DATE_TIME).asText(null));

        try {
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new SharePointOperationException("Failed to serialize file metadata response", e);
        }
    }

    /**
     * Returns a time-limited CDN download URL for a drive item so the caller
     * can fetch the raw file bytes without going through the Graph API again.
     *
     * <p>The URL is a pre-signed Microsoft CDN link (typically valid for ~1 hour).
     *
     * @throws IllegalArgumentException if {@code itemId} is blank or no downloadable URL is found
     */
    public String getFileDownloadUrl(String token, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be empty");
        }

        // Fetch metadata so we can include humanly useful info alongside the URL
        JsonNode metadata = parseJson(graphClient.fetchItemFullMetadata(token, itemId),
                METADATA_CONTEXT_PREFIX + itemId);

        String name     = metadata.path("name").asText("");
        String mimeType = metadata.path("file").path(FIELD_MIME_TYPE).asText(null);
        long   size     = metadata.path("size").asLong(0L);
        String ext      = fileExtension(name);

        // Resolve the CDN download URL (Graph returns 302 → pre-signed URL)
        String downloadUrl = graphClient.fetchDownloadUrl(token, itemId);
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Could not obtain a download URL for file '" + name + "'. "
                    + "The file may not be downloadable or access is restricted.");
        }

        // Append download=1 so the CDN sends Content-Disposition: attachment,
        // which causes browsers to save the file rather than open it in a tab.
        String forceDownloadUrl = downloadUrl + (downloadUrl.contains("?") ? "&" : "?") + "download=1";

        ObjectNode result = OBJECT_MAPPER.createObjectNode();
        result.put(FIELD_NAME, name);
        result.put(FIELD_FILE_TYPE, ext.isEmpty() ? null : ext);
        result.put(FIELD_MIME_TYPE, mimeType);
        result.put(FIELD_SIZE_BYTES, size);
        result.put("downloadUrl", forceDownloadUrl);

        try {
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new SharePointOperationException("Failed to serialize download URL response", e);
        }
    }

    /**
     * Moves a drive item to a different folder, optionally renaming it.
     *
     * @throws IllegalArgumentException if {@code itemId} or {@code targetFolderId} is blank
     */
    public String moveItem(String token, String itemId, String targetFolderId, String newName) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be empty");
        }
        if (targetFolderId == null || targetFolderId.isBlank()) {
            throw new IllegalArgumentException("targetFolderId must not be empty");
        }

        JsonNode moved = parseJson(
                graphClient.moveItem(token, itemId, targetFolderId, newName),
                "move result for itemId=" + itemId);

        ObjectNode result = OBJECT_MAPPER.createObjectNode();
        result.put(FIELD_ID, moved.path(FIELD_ID).asText(null));
        result.put(FIELD_NAME, moved.path(FIELD_NAME).asText(null));
        result.put(FIELD_WEB_URL, moved.path(FIELD_WEB_URL).asText(null));
        result.put("parentId", moved.path("parentReference").path(FIELD_ID).asText(null));
        try {
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new SharePointOperationException("Failed to serialize move item response", e);
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

    private static void validateItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be empty");
        }
    }

    private ExtractedDocumentMetadata fetchDocumentMetadata(String token, String itemId) {
        JsonNode metadata = parseJson(graphClient.fetchItemMetadata(token, itemId),
                METADATA_CONTEXT_PREFIX + itemId);
        String name = metadata.path("name").asText("");
        long sizeBytes = metadata.path("size").asLong(0L);
        String extension = fileExtension(name);
        String webUrl = metadata.path(FIELD_WEB_URL).asText(null);
        DocumentLocation location = extractDocumentLocation(metadata.path("parentReference"), webUrl);
        return new ExtractedDocumentMetadata(name, sizeBytes, extension, webUrl, location);
    }

    private static DocumentLocation extractDocumentLocation(JsonNode parentReference, String webUrl) {
        if (parentReference.isMissingNode()) {
            return new DocumentLocation(null, null, null);
        }

        String library = parentReference.path("name").asText(null);
        String folder = extractFolder(parentReference.path("path").asText(""));
        String site = extractSite(webUrl);
        return new DocumentLocation(site, library, folder);
    }

    private static String extractFolder(String path) {
        if (!path.contains("/root:")) {
            return null;
        }
        String[] parts = path.split("/root:", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return null;
        }
        return parts[1].startsWith("/") ? parts[1].substring(1) : parts[1];
    }

    private static String extractSite(String webUrl) {
        if (webUrl == null || !webUrl.contains("/sites/")) {
            return null;
        }
        String[] urlParts = webUrl.split("/sites/", 2);
        if (urlParts.length < 2 || urlParts[1].isBlank()) {
            return null;
        }
        return urlParts[1].split("/")[0];
    }

    private void validateDocumentForExtraction(ExtractedDocumentMetadata metadata) {
        String typeError = validator.validateContentType(metadata.extension());
        if (typeError != null) {
            throw new IllegalArgumentException(typeError);
        }
        if (metadata.sizeBytes() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "File is too large (" + (metadata.sizeBytes() / (1_024 * 1_024)) + " MB). Maximum is 10 MB.");
        }
    }

    private byte[] downloadDocumentContent(String token, String itemId, String name) {
        byte[] bytes = graphClient.downloadItemContent(token, itemId);
        if (bytes == null || bytes.length == 0) {
            log.warn("Downloaded file '{}' (itemId={}) returned an empty body", name, itemId);
            throw new IllegalArgumentException("Downloaded file is empty");
        }
        log.info("Downloaded '{}' (itemId={}) — {} bytes", name, itemId, bytes.length);
        return bytes;
    }

    private String extractTextOrThrow(byte[] bytes, String name, long sizeBytes) {
        String rawText = contentExtractor.extractText(bytes, name);
        log.info("Extracted {} characters from '{}' (stored size: {} bytes)", rawText.length(), name, sizeBytes);

        if (rawText.isBlank()) {
            throw new IllegalArgumentException(
                    "No text could be extracted from '" + name + "'. "
                            + "The file may be a scanned image (no text layer) or the format is unsupported.");
        }
        return rawText;
    }

    private static boolean isTruncated(String rawText) {
        return rawText.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES;
    }

    private String buildDocumentContentResponse(ExtractedDocumentMetadata metadata, String content, boolean truncated) {
         ObjectNode result = OBJECT_MAPPER.createObjectNode();
         result.put(FIELD_NAME, metadata.name());
         result.put(FIELD_FILE_TYPE, metadata.extension());
        result.put(FIELD_SIZE_BYTES, metadata.sizeBytes());
        result.put("url", metadata.webUrl());
        result.put("site", metadata.location().site());
        result.put("library", metadata.location().library());
        result.put("folder", metadata.location().folder());
        result.put("truncated", truncated);
        result.put("content", content);
        try {
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new SharePointOperationException("Failed to serialize document content response", e);
        }
    }

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
                parsed.put(FIELD_ID, site.path(FIELD_ID).asText(null));
                parsed.put(FIELD_NAME, site.path(FIELD_NAME).asText(null));
                parsed.put(FIELD_DISPLAY_NAME, site.path(FIELD_DISPLAY_NAME).asText(null));
                parsed.put(FIELD_WEB_URL, site.path(FIELD_WEB_URL).asText(null));
                parsed.put(FIELD_DESCRIPTION, site.path(FIELD_DESCRIPTION).asText(null));
                parsed.put(FIELD_CREATED_DATE_TIME, site.path(FIELD_CREATED_DATE_TIME).asText(null));
                parsed.put(FIELD_LAST_MODIFIED_DATE_TIME, site.path(FIELD_LAST_MODIFIED_DATE_TIME).asText(null));
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
            result.put(FIELD_ID, site.path(FIELD_ID).asText(null));
            result.put(FIELD_NAME, site.path(FIELD_NAME).asText(null));
            result.put(FIELD_DISPLAY_NAME, site.path(FIELD_DISPLAY_NAME).asText(null));
            result.put(FIELD_WEB_URL, site.path(FIELD_WEB_URL).asText(null));
            result.put(FIELD_DESCRIPTION, site.path(FIELD_DESCRIPTION).asText(null));
            result.put(FIELD_CREATED_DATE_TIME, site.path(FIELD_CREATED_DATE_TIME).asText(null));
            result.put(FIELD_LAST_MODIFIED_DATE_TIME, site.path(FIELD_LAST_MODIFIED_DATE_TIME).asText(null));
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
                parsed.put(FIELD_ID, drive.path(FIELD_ID).asText(null));
                parsed.put(FIELD_NAME, drive.path(FIELD_NAME).asText(null));
                parsed.put(FIELD_DESCRIPTION, drive.path(FIELD_DESCRIPTION).asText(null));
                parsed.put(FIELD_WEB_URL, drive.path(FIELD_WEB_URL).asText(null));
                parsed.put("driveType", drive.path("driveType").asText(null));
                parsed.put(FIELD_CREATED_DATE_TIME, drive.path(FIELD_CREATED_DATE_TIME).asText(null));
                parsed.put(FIELD_LAST_MODIFIED_DATE_TIME, drive.path(FIELD_LAST_MODIFIED_DATE_TIME).asText(null));

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

    private record DocumentLocation(String site, String library, String folder) {}

    private record ExtractedDocumentMetadata(String name, long sizeBytes, String extension,
                                             String webUrl, DocumentLocation location) {}
}