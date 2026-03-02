package com.amaliai.mcp.servers.sharepoint;

import com.amaliai.mcp.servers.sharepoint.service.SharePointService;
import com.amaliai.mcp.servers.sharepoint.util.SharePointTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * MCP tool entrypoint for SharePoint / OneDrive operations.
 * <p>
 * This class is intentionally thin: it acquires a token and delegates every
 * operation to {@link SharePointService}. All exception handling is managed
 * centrally by {@link com.amaliai.mcp.config.McpToolExceptionHandler}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SharePointServer {

    private final SharePointService      sharePointService;
    private final SharePointTokenManager tokenManager;

    // -------------------------------------------------------------------------
    // MCP Tools
    // -------------------------------------------------------------------------

    @Tool(description = "Gets all documents and files from the authenticated user's OneDrive. "
            + "Returns item IDs, file names, URLs, sizes, file types, and last modified dates. "
            + "Use the returned 'id' field with getDocumentContent to read a file's content.")
    public String getDocuments(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The SharePoint integration ID (UUID format)") String integrationId) {

        String token = tokenManager.getAccessToken(armsUserId, UUID.fromString(integrationId));
        return sharePointService.listDocuments(token);
    }

    @Tool(description = "Searches for documents in the authenticated user's OneDrive by keyword. "
            + "Optionally filter by file type, author, or date range via the filter parameter. "
            + "Returns item IDs, file names, URLs, authors, file sizes, and last modified dates. "
            + "Use the returned 'id' field with getDocumentContent to read a file's content.")
    public String searchDocuments(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The SharePoint integration ID (UUID format)") String integrationId,
            @ToolParam(description = "Keywords to search for") String query,
            @ToolParam(description = "Optional search filters: fileType (docx/pdf/xlsx/pptx/txt/csv/md/html/xml), "
                    + "author (display name substring), from/to (ISO-8601 date range), top (max results, default 20 max 50)") SearchFilter filter) {

        String token = tokenManager.getAccessToken(armsUserId, UUID.fromString(integrationId));
        return sharePointService.searchDocuments(token, query,
                filter != null ? filter.fileType() : null,
                filter != null ? filter.author()   : null,
                filter != null ? filter.from()     : null,
                filter != null ? filter.to()       : null,
                filter != null ? filter.top()      : null);
    }

    @Tool(description = "Reads and returns the text content of a document from the user's OneDrive. "
            + "Supported file types: txt, md, csv, log, html, xml, json, docx, xlsx, pdf. "
            + "PPT and PPTX are not supported. Files larger than 10 MB are rejected. "
            + "Content is truncated at 512 KB. "
            + "Use the 'id' field from getDocuments or searchDocuments as the itemId.")
    public String getDocumentContent(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The SharePoint integration ID (UUID format)") String integrationId,
            @ToolParam(description = "The drive item ID of the document (from getDocuments or searchDocuments)") String itemId) {

        String token = tokenManager.getAccessToken(armsUserId, UUID.fromString(integrationId));
        return sharePointService.getDocumentContent(token, itemId);
    }
}
