package com.amaliai.mcp.servers.sharepoint.client;

import com.amaliai.mcp.servers.sharepoint.exception.SharePointOperationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

/**
 * Transforms a Microsoft Graph API drive-items response
 * ({@code "value": [...]}) into a flat, tool-friendly JSON array.
 * <p>
 * Lives in the {@code client} package because it has intimate knowledge of
 * the Graph API response shape; changing the API version may require
 * updating this class alongside {@link SharePointGraphClient}.
 */
@Component
public class DriveItemParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Parses the Graph API response body and returns a JSON array string.
     * Folders are always skipped.
     *
     * @param responseBody   raw JSON from the Graph API ({@code "value": [...]})
     * @param fileTypeFilter optional extension to keep (e.g. {@code "xlsx"}); {@code null} = all
     * @param authorFilter   optional substring match on the creator's display name; {@code null} = all
     * @throws SharePointOperationException if the response body cannot be parsed
     */
    public String parse(String responseBody, String fileTypeFilter, String authorFilter) {
        return parse(responseBody, fileTypeFilter, authorFilter, false);
    }

    /**
     * Parses the Graph API response body and returns a JSON array string.
     *
     * @param responseBody   raw JSON from the Graph API ({@code "value": [...]})
     * @param fileTypeFilter optional extension to keep (e.g. {@code "xlsx"}); {@code null} = all
     * @param authorFilter   optional substring match on the creator's display name; {@code null} = all
     * @param includeFolders when {@code true}, folder items are included with {@code "itemType":"folder"}
     * @throws SharePointOperationException if the response body cannot be parsed
     */
    public String parse(String responseBody, String fileTypeFilter, String authorFilter, boolean includeFolders) {
        try {
            JsonNode items = OBJECT_MAPPER.readTree(responseBody).path("value");
            ArrayNode results = OBJECT_MAPPER.createArrayNode();

            for (JsonNode item : items) {
                boolean isFolder = item.has("folder");
                if (isFolder && !includeFolders) continue;

                String name      = item.path("name").asText("");
                String createdBy = item.path("createdBy").path("user").path("displayName").asText(null);

                if (!isFolder && fileTypeFilter != null
                        && !name.toLowerCase().endsWith("." + fileTypeFilter.toLowerCase())) continue;

                if (authorFilter != null && !authorFilter.isBlank()
                        && (createdBy == null
                        || !createdBy.toLowerCase().contains(authorFilter.toLowerCase()))) continue;

                ObjectNode doc = OBJECT_MAPPER.createObjectNode();
                doc.put("id",             item.path("id").asText(null));
                doc.put("name",           name);
                doc.put("itemType",       isFolder ? "folder" : "file");
                doc.put("webUrl",         item.path("webUrl").asText(null));
                doc.put("sizeBytes",      item.path("size").asLong(0L));
                doc.put("lastModified",   item.path("lastModifiedDateTime").asText(null));
                doc.put("createdBy",      createdBy);
                doc.put("lastModifiedBy", item.path("lastModifiedBy")
                        .path("user").path("displayName").asText(null));

                if (!isFolder) {
                    int dot = name.lastIndexOf('.');
                    if (dot >= 0) doc.put("fileType", name.substring(dot + 1).toLowerCase());
                }

                results.add(doc);
            }

            return OBJECT_MAPPER.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            throw new SharePointOperationException("Failed to parse Graph API drive-items response", e);
        }
    }
}
