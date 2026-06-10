package com.amaliai.mcp.servers.sharepoint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SharePointConstants}.
 *
 * Coverage strategy
 * -----------------
 * SharePointConstants is a utility class with static constants.
 * Tests verify the constants are defined and have expected values.
 */
class SharePointConstantsTest {

    @Test
    void toolNames_areDefinedCorrectly() {
        assertThat(SharePointConstants.TOOL_SEARCH).isEqualTo("searchDocuments");
        assertThat(SharePointConstants.TOOL_GET).isEqualTo("getDocuments");
        assertThat(SharePointConstants.TOOL_CONTENT).isEqualTo("getDocumentContent");
        assertThat(SharePointConstants.TOOL_METADATA).isEqualTo("getFileMetadata");
        assertThat(SharePointConstants.TOOL_LIST_SITES).isEqualTo("listSites");
        assertThat(SharePointConstants.TOOL_SITE_DETAILS).isEqualTo("getSiteDetails");
        assertThat(SharePointConstants.TOOL_LIST_LIBRARIES).isEqualTo("listLibraries");
        assertThat(SharePointConstants.TOOL_FOLDER).isEqualTo("getFolderContents");
        assertThat(SharePointConstants.TOOL_DOWNLOAD_URL).isEqualTo("downloadFile");
    }

    @Test
    void allowedFileTypes_containsExpectedExtensions() {
        assertThat(SharePointConstants.ALLOWED_FILE_TYPES)
                .contains("docx", "pdf", "xlsx", "pptx", "txt", "csv", "md", "html", "xml")
                .hasSize(9);
    }

    @Test
    void supportedContentTypes_includesAllowedTypesPlus() {
        assertThat(SharePointConstants.SUPPORTED_CONTENT_TYPES)
                .contains("txt", "md", "csv", "log", "html", "xml", "json", "docx", "xlsx", "pdf")
                .hasSize(10);
    }

    @Test
    void pagination_defaultsAndMaxes_areCorrect() {
        assertThat(SharePointConstants.DEFAULT_TOP).isEqualTo(20);
        assertThat(SharePointConstants.MAX_TOP).isEqualTo(50);
    }

    @Test
    void sizeCaps_areDefinedCorrectly() {
        assertThat(SharePointConstants.MAX_QUERY_LENGTH).isEqualTo(1_000);
        assertThat(SharePointConstants.MAX_RESPONSE_BYTES).isEqualTo(512 * 1_024);
        assertThat(SharePointConstants.MAX_CONTENT_BYTES).isEqualTo(512 * 1_024);
        assertThat(SharePointConstants.MAX_FILE_SIZE_BYTES).isEqualTo(10L * 1_024 * 1_024);
    }

    @Test
    void errorMessages_areDefinedCorrectly() {
        assertThat(SharePointConstants.MSG_AUTH_FAILED).isEqualTo("Authentication failed");
        assertThat(SharePointConstants.MSG_INTERNAL_ERR).isEqualTo("Internal error");
        assertThat(SharePointConstants.MSG_GRAPH_ERR).isEqualTo("Graph API returned ");
        assertThat(SharePointConstants.BEARER_PREFIX).isEqualTo("Bearer ");
        assertThat(SharePointConstants.LOG_TOKEN_FAILURE).isEqualTo("Failed to retrieve token for user {}");
    }
}

