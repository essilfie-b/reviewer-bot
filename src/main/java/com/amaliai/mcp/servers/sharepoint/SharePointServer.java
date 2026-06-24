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

        private final SharePointService sharePointService;
        private final SharePointTokenManager tokenManager;

        // -------------------------------------------------------------------------
        // MCP Tools
        // -------------------------------------------------------------------------

        @Tool(description = "Gets all documents and files from the authenticated user's OneDrive. "
                        + "Returns item IDs, file names, URLs, sizes, file types, and last modified dates. "
                        + "Use the returned 'id' field with getDocumentContent to read a file's content.")
        public String getDocuments(
                        @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId) {

                UUID integrationId = tokenManager.resolveIntegrationId();
                String token = tokenManager.getAccessToken(armsUserId, integrationId);
                return sharePointService.listDocuments(token);
        }

        @Tool(description = "Searches for documents in the authenticated user's OneDrive by keyword. "
                        + "Optionally filter by file type, author, or date range via the filter parameter. "
                        + "Returns item IDs, file names, URLs, authors, file sizes, and last modified dates. "
                        + "Use the returned 'id' field with getDocumentContent to read a file's content.")
        public String searchDocuments(
                        @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
                        @ToolParam(description = "Keywords to search for") String query,
                        @ToolParam(description = "Optional search filters: fileType (docx/pdf/xlsx/pptx/txt/csv/md/html/xml), "
                                        + "author (display name substring), from/to (ISO-8601 date range), top (max results, default 20 max 50)") SearchFilter filter) {

                UUID integrationId = tokenManager.resolveIntegrationId();
                String token = tokenManager.getAccessToken(armsUserId, integrationId);
                return sharePointService.searchDocuments(token, query,
                                filter != null ? filter.fileType() : null,
                                filter != null ? filter.author() : null,
                                filter != null ? filter.from() : null,
                                filter != null ? filter.to() : null,
                                filter != null ? filter.top() : null);
        }

        @Tool(description = "Lists the contents of a specific folder in the user's OneDrive. "
                        + "Returns both files and sub-folders with their IDs, names, item type (file/folder), "
                        + "URLs, sizes, and last modified dates. "
                        + "Use the 'id' field from getDocuments or a previous getFolderContents call as the folderId. "
                        + "Use the returned file 'id' with getDocumentContent to read a file's content, "
                        + "or with getFolderContents to navigate into a sub-folder.")
        public String getFolderContents(
                        @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
                        @ToolParam(description = "The drive item ID of the folder to list") String folderId) {

                UUID integrationId = tokenManager.resolveIntegrationId();
                String token = tokenManager.getAccessToken(armsUserId, integrationId);
                return sharePointService.getFolderContents(token, folderId);
        }

        @Tool(description = "Returns metadata for a single file in the user's OneDrive: "
                        + "id, name, file type, MIME type, size, web URL, who created it, creation date, "
                        + "who last modified it, and the last modification date. "
                        + "Use the 'id' field from getDocuments or searchDocuments as the itemId.")
        public String getFileMetadata(
                        @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
                        @ToolParam(description = "The drive item ID of the file (from getDocuments or searchDocuments)") String itemId) {

                UUID integrationId = tokenManager.resolveIntegrationId();
                String token = tokenManager.getAccessToken(armsUserId, integrationId);
                return sharePointService.getFileMetadata(token, itemId);
        }

        @Tool(description = "Reads and returns the text content of a document from the user's OneDrive. "
                        + "Supported file types: txt, md, csv, log, html, xml, json, docx, xlsx, pdf. "
                        + "PPT and PPTX are not supported. Files larger than 10 MB are rejected. "
                        + "Content is truncated at 512 KB. "
                        + "Use the 'id' field from getDocuments or searchDocuments as the itemId.")
        public String getDocumentContent(
                        @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
                        @ToolParam(description = "The drive item ID of the document (from getDocuments or searchDocuments)") String itemId) {

                UUID integrationId = tokenManager.resolveIntegrationId();
                String token = tokenManager.getAccessToken(armsUserId, integrationId);
                return sharePointService.getDocumentContent(token, itemId);
        }

        @Tool(description = "Lists all SharePoint sites the authenticated user has access to. "
                        + "Returns site IDs, names, display names, URLs, descriptions, and timestamps. "
                        + "Use the returned 'id' field with getSiteDetails or listLibraries to explore a site.")
        public String listSites(
                        @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
                        @ToolParam(description = "Maximum number of sites to return (default 20, max 50)", required = false) Integer top) {

                UUID integrationId = tokenManager.resolveIntegrationId();
                String token = tokenManager.getAccessToken(armsUserId, integrationId);
                return sharePointService.listSites(token, top);
        }

        @Tool(description = "Gets detailed information about a specific SharePoint site. "
                        + "Returns site ID, name, display name, URL, description, creation date, last modified date, and whether it's a root site. "
                        + "Use the 'id' field from listSites as the siteId.")
        public String getSiteDetails(
                        @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
                        @ToolParam(description = "The SharePoint site ID (from listSites)") String siteId) {

                UUID integrationId = tokenManager.resolveIntegrationId();
                String token = tokenManager.getAccessToken(armsUserId, integrationId);
                return sharePointService.getSiteDetails(token, siteId);
        }

        @Tool(description = "Lists document libraries (drives) in a SharePoint site. "
                        + "Returns library IDs, names, descriptions, URLs, drive types, timestamps, and storage quota information. "
                        + "Use the 'id' field from listSites as the siteId. "
                        + "The returned library 'id' can be used to access documents within that library.")
        public String listLibraries(
                        @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
                        @ToolParam(description = "The SharePoint site ID (from listSites)") String siteId,
                        @ToolParam(description = "Maximum number of libraries to return (default 20, max 50)", required = false) Integer top) {

            UUID integrationId = tokenManager.resolveIntegrationId();
            String token = tokenManager.getAccessToken(armsUserId, integrationId);
            return sharePointService.listLibraries(token, siteId, top);
    }

    @Tool(description = "Lists documents in the authenticated user's OneDrive that were modified within the last N days. "
            + "Returns item IDs, file names, URLs, sizes, file types, and last modified dates, "
            + "ordered by most-recently-modified first. "
            + "Optionally filter by file type and cap the number of results via the filter parameter. "
            + "Use the returned 'id' field with getDocumentContent to read a file's content.")
    public String getRecentDocuments(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "Look-back window in days (1-365, default 7)") int days,
            @ToolParam(description = "Optional filters: fileType (docx/pdf/xlsx/pptx/txt/csv/md/html/xml), "
                    + "top (max results, default 20 max 50)", required = false) RecentFilter filter) {

        UUID integrationId = tokenManager.resolveIntegrationId();
        String token = tokenManager.getAccessToken(armsUserId, integrationId);
        return sharePointService.listRecentDocuments(token, days,
                filter != null ? filter.fileType() : null,
                filter != null ? filter.top() : null);
    }

    @Tool(description = "Generates a direct, time-limited download URL for a file in the user's OneDrive or SharePoint. "
            + "Returns the file name, type, MIME type, size in bytes, and a pre-signed 'downloadUrl' that the user can open "
            + "in a browser or use with any HTTP client to download the actual file — no further authentication is required. "
            + "The URL is typically valid for approximately 1 hour. "
            + "Use the 'id' field from getDocuments or searchDocuments as the itemId.")
    public String downloadFile(
            @ToolParam(description = "The ARMS user ID of the authenticated user") int armsUserId,
            @ToolParam(description = "The drive item ID of the file to download (from getDocuments or searchDocuments)") String itemId) {

        UUID integrationId = tokenManager.resolveIntegrationId();
        String token = tokenManager.getAccessToken(armsUserId, integrationId);
        return sharePointService.getFileDownloadUrl(token, itemId);
    }
}
