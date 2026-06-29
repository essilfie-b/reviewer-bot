package com.amaliai.mcp.servers.sharepoint;

import java.util.Set;

/**
 * Shared constants for the SharePoint MCP server.
 * <p>
 * Centralising these here prevents magic values from scattering across layers and
 * makes tuning (size caps, pagination limits, etc.) a single-file change.
 */
public final class SharePointConstants {

    private SharePointConstants() {}

    // -------------------------------------------------------------------------
    // Tool names
    // -------------------------------------------------------------------------

    public static final String TOOL_SEARCH   = "searchDocuments";
    public static final String TOOL_GET      = "getDocuments";
    public static final String TOOL_CONTENT  = "getDocumentContent";
    public static final String TOOL_METADATA = "getFileMetadata";
    public static final String TOOL_LIST_SITES = "listSites";
    public static final String TOOL_SITE_DETAILS = "getSiteDetails";
    public static final String TOOL_LIST_LIBRARIES = "listLibraries";
    public static final String TOOL_FOLDER   = "getFolderContents";
    public static final String TOOL_DOWNLOAD_URL  = "downloadFile";
    public static final String TOOL_CREATE_LINK   = "createSharingLink";

    // -------------------------------------------------------------------------
    // Allowed / supported file types
    // -------------------------------------------------------------------------

    /** File types accepted by the search and list tools' optional filter. */
    public static final Set<String> ALLOWED_FILE_TYPES =
            Set.of("docx", "pdf", "xlsx", "pptx", "txt", "csv", "md", "html", "xml");

    /** File types supported for text extraction. PPT/PPTX are intentionally excluded. */
    public static final Set<String> SUPPORTED_CONTENT_TYPES =
            Set.of("txt", "md", "csv", "log", "html", "xml", "json", "docx", "xlsx", "pdf");

    // -------------------------------------------------------------------------
    // Sharing links
    // -------------------------------------------------------------------------

    /** Link permission levels supported by the Graph {@code createLink} action. */
    public static final Set<String> ALLOWED_LINK_TYPES = Set.of("view", "edit", "embed");

    /** Audiences a sharing link can target. */
    public static final Set<String> ALLOWED_LINK_SCOPES = Set.of("anonymous", "organization");

    /** Defaults applied when the caller omits the link type or scope. */
    public static final String DEFAULT_LINK_TYPE  = "view";
    public static final String DEFAULT_LINK_SCOPE = "organization";

    // -------------------------------------------------------------------------
    // Pagination
    // -------------------------------------------------------------------------

    public static final int DEFAULT_TOP = 20;
    public static final int MAX_TOP     = 50;

    // -------------------------------------------------------------------------
    // Size / length caps
    // -------------------------------------------------------------------------

    public static final int  MAX_QUERY_LENGTH    = 1_000;
    public static final int  MAX_RESPONSE_BYTES  = 512 * 1_024;     // 512 KB — list / search responses
    public static final int  MAX_CONTENT_BYTES   = 512 * 1_024;     // 512 KB — extracted text
    public static final long MAX_FILE_SIZE_BYTES = 10L * 1_024 * 1_024; // 10 MB — reject before download

    // -------------------------------------------------------------------------
    // Common log / error messages
    // -------------------------------------------------------------------------

    public static final String MSG_AUTH_FAILED  = "Authentication failed";
    public static final String MSG_INTERNAL_ERR = "Internal error";
    public static final String MSG_GRAPH_ERR    = "Graph API returned ";
    public static final String BEARER_PREFIX    = "Bearer ";
    public static final String LOG_TOKEN_FAILURE = "Failed to retrieve token for user {}";

}