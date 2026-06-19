package com.amaliai.mcp.servers.sharepoint.service;

import com.amaliai.mcp.servers.sharepoint.client.DriveItemParser;
import com.amaliai.mcp.servers.sharepoint.client.SharePointGraphClient;
import com.amaliai.mcp.servers.sharepoint.extractor.SharePointContentExtractor;
import com.amaliai.mcp.servers.sharepoint.util.SharePointResponseUtil;
import com.amaliai.mcp.servers.sharepoint.validator.SharePointValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SharePointService}.
 *
 * Coverage strategy
 * -----------------
 * SharePointService orchestrates client, validation, extraction, and formatting.
 * Tests cover:
 * - Input validation and error handling
 * - Delegation to clients and extractors
 * - Response formatting and truncation
 * - Business rule enforcement (file size limits, content types, etc.)
 */
@ExtendWith(MockitoExtension.class)
class SharePointServiceTest {

    private static final String TOKEN = "test-token";
    private static final String EMPTY_ITEMS_RESPONSE = "{\"value\":[]}";
    private static final String PARSED_RESPONSE = "[]";

    @Mock private SharePointGraphClient graphClient;
    @Mock private DriveItemParser driveItemParser;
    @Mock private SharePointContentExtractor contentExtractor;
    @Mock private SharePointValidator validator;
    @Mock private SharePointResponseUtil responseUtil;

    private SharePointService service;

     @BeforeEach
     void setUp() {
         service = new SharePointService(graphClient, driveItemParser, contentExtractor, validator, responseUtil);
         // responseUtil.trimResponse returns the input unchanged by default
         Mockito.lenient().when(responseUtil.trimResponse(anyString(), anyInt())).thenAnswer(inv -> inv.getArgument(0));
     }

    @Nested
    class ListDocumentsTests {

        @Test
        void listDocuments_delegatesAndReturnsCorrectly() {
            when(graphClient.fetchRootChildren(TOKEN)).thenReturn(EMPTY_ITEMS_RESPONSE);
            when(driveItemParser.parse(EMPTY_ITEMS_RESPONSE, null, null)).thenReturn(PARSED_RESPONSE);

            String result = service.listDocuments(TOKEN);

            assertThat(result).isEqualTo(PARSED_RESPONSE);
            verify(graphClient).fetchRootChildren(TOKEN);
            verify(driveItemParser).parse(EMPTY_ITEMS_RESPONSE, null, null);
        }
    }

    @Nested
    class GetFolderContentsTests {

        @Test
        void getFolderContents_withValidFolderId_succeeds() {
            String folderId = "folder-123";
            when(graphClient.fetchFolderChildren(TOKEN, folderId)).thenReturn(EMPTY_ITEMS_RESPONSE);
            when(driveItemParser.parse(EMPTY_ITEMS_RESPONSE, null, null, true)).thenReturn(PARSED_RESPONSE);

            String result = service.getFolderContents(TOKEN, folderId);

            assertThat(result).isEqualTo(PARSED_RESPONSE);
            verify(graphClient).fetchFolderChildren(TOKEN, folderId);
        }

        @Test
        void getFolderContents_withBlankFolderId_throwsException() {
            assertThatThrownBy(() -> service.getFolderContents(TOKEN, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("folderId must not be empty");
        }

        @Test
        void getFolderContents_withNullFolderId_throwsException() {
            assertThatThrownBy(() -> service.getFolderContents(TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("folderId must not be empty");
        }
    }

    @Nested
    class SearchDocumentsTests {

        @Test
        void searchDocuments_withValidInputs_succeeds() {
            when(validator.validateSearchInputs("query", "docx")).thenReturn(null);
            when(validator.buildDateFilters(null, null)).thenReturn(java.util.List.of());
            when(graphClient.searchItems(TOKEN, "query", 20, java.util.List.of())).thenReturn(EMPTY_ITEMS_RESPONSE);
            when(driveItemParser.parse(EMPTY_ITEMS_RESPONSE, "docx", null)).thenReturn(PARSED_RESPONSE);

            String result = service.searchDocuments(TOKEN, "query", "docx", null, null, null, null);

            assertThat(result).isEqualTo(PARSED_RESPONSE);
        }

        @Test
        void searchDocuments_withValidationError_throwsException() {
            when(validator.validateSearchInputs("", "docx")).thenReturn("Query must not be empty");

            assertThatThrownBy(() -> service.searchDocuments(TOKEN, "", "docx", null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Query must not be empty");
        }

        @Test
        void searchDocuments_withDateRange_buildsDynamicFilters() {
            when(validator.validateSearchInputs("query", null)).thenReturn(null);
            when(validator.buildDateFilters("2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z"))
                    .thenReturn(java.util.List.of(
                            "lastModifiedDateTime ge 2025-01-01T00:00:00Z",
                            "lastModifiedDateTime le 2025-12-31T23:59:59Z"
                    ));
            when(graphClient.searchItems(anyString(), anyString(), anyInt(), any(java.util.List.class)))
                    .thenReturn(EMPTY_ITEMS_RESPONSE);
            when(driveItemParser.parse(EMPTY_ITEMS_RESPONSE, null, null)).thenReturn(PARSED_RESPONSE);

            String result = service.searchDocuments(TOKEN, "query", null, null,
                    "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z", null);

            assertThat(result).isEqualTo(PARSED_RESPONSE);
        }

        @Test
        void searchDocuments_withCustomTop_respectsLimit() {
            when(validator.validateSearchInputs("query", null)).thenReturn(null);
            when(validator.buildDateFilters(null, null)).thenReturn(java.util.List.of());
            when(graphClient.searchItems(TOKEN, "query", 30, java.util.List.of())).thenReturn(EMPTY_ITEMS_RESPONSE);
            when(driveItemParser.parse(EMPTY_ITEMS_RESPONSE, null, null)).thenReturn(PARSED_RESPONSE);

            String result = service.searchDocuments(TOKEN, "query", null, null, null, null, 30);

            assertThat(result).isEqualTo(PARSED_RESPONSE);
            verify(graphClient).searchItems(TOKEN, "query", 30, java.util.List.of());
        }

        @Test
        void searchDocuments_withTopExceedingMax_clampsToMax() {
            when(validator.validateSearchInputs("query", null)).thenReturn(null);
            when(validator.buildDateFilters(null, null)).thenReturn(java.util.List.of());
            when(graphClient.searchItems(TOKEN, "query", 50, java.util.List.of())).thenReturn(EMPTY_ITEMS_RESPONSE);
            when(driveItemParser.parse(EMPTY_ITEMS_RESPONSE, null, null)).thenReturn(PARSED_RESPONSE);

            String result = service.searchDocuments(TOKEN, "query", null, null, null, null, 100);

            assertThat(result).isEqualTo(PARSED_RESPONSE);
            verify(graphClient).searchItems(TOKEN, "query", 50, java.util.List.of());
        }
    }

    @Nested
    class ListSharedWithMeTests {

        // A realistic /sharedWithMe payload: the shared file lives in another
        // user's drive and is exposed through the "remoteItem" facet.
        private static final String SHARED_RESPONSE = "{\"value\":[{"
                + "\"id\":\"local-stub-1\","
                + "\"remoteItem\":{"
                + "  \"id\":\"remote-1\","
                + "  \"name\":\"Q3-Budget.xlsx\","
                + "  \"size\":2048,"
                + "  \"webUrl\":\"https://contoso.sharepoint.com/Q3-Budget.xlsx\","
                + "  \"lastModifiedDateTime\":\"2026-06-01T10:00:00Z\","
                + "  \"file\":{\"mimeType\":\"application/vnd.openxmlformats\"},"
                + "  \"shared\":{"
                + "    \"owner\":{\"user\":{\"displayName\":\"Alice Owner\"}},"
                + "    \"sharedBy\":{\"user\":{\"displayName\":\"Bob Sharer\"}},"
                + "    \"sharedDateTime\":\"2026-06-02T09:00:00Z\"}}}]}";

        @Test
        void listSharedWithMe_withoutTop_usesDefaultAndParsesRemoteItem() {
            when(graphClient.fetchSharedWithMe(TOKEN, 20)).thenReturn(SHARED_RESPONSE);

            String result = service.listSharedWithMe(TOKEN, null);

            assertThat(result)
                    .contains("remote-1")
                    .contains("Q3-Budget.xlsx")
                    .contains("\"itemType\":\"file\"")
                    .contains("\"fileType\":\"xlsx\"")
                    .contains("Alice Owner")
                    .contains("Bob Sharer")
                    .contains("2026-06-02T09:00:00Z");
            verify(graphClient).fetchSharedWithMe(TOKEN, 20);
        }

        @Test
        void listSharedWithMe_withCustomTop_respectsLimit() {
            when(graphClient.fetchSharedWithMe(TOKEN, 15)).thenReturn(EMPTY_ITEMS_RESPONSE);

            String result = service.listSharedWithMe(TOKEN, 15);

            assertThat(result).isEqualTo("[]");
            verify(graphClient).fetchSharedWithMe(TOKEN, 15);
        }

        @Test
        void listSharedWithMe_withTopExceedingMax_clampsToMax() {
            when(graphClient.fetchSharedWithMe(TOKEN, 50)).thenReturn(EMPTY_ITEMS_RESPONSE);

            String result = service.listSharedWithMe(TOKEN, 100);

            assertThat(result).isEqualTo("[]");
            verify(graphClient).fetchSharedWithMe(TOKEN, 50);
        }
    }

    @Nested
    class GetFileMetadataTests {

        @Test
        void getFileMetadata_withValidItemId_succeeds() {
            String itemId = "item-123";
            String metadataJson = "{\"name\":\"test.pdf\",\"size\":1024}";
            when(graphClient.fetchItemFullMetadata(TOKEN, itemId)).thenReturn(metadataJson);

            String result = service.getFileMetadata(TOKEN, itemId);

            assertThat(result).isNotNull();
            verify(graphClient).fetchItemFullMetadata(TOKEN, itemId);
        }

        @Test
        void getFileMetadata_withNullItemId_throwsException() {
            assertThatThrownBy(() -> service.getFileMetadata(TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("itemId must not be empty");
        }

        @Test
        void getFileMetadata_withBlankItemId_throwsException() {
            assertThatThrownBy(() -> service.getFileMetadata(TOKEN, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("itemId must not be empty");
        }
    }

    @Nested
    class GetDocumentContentTests {

        @Test
        void getDocumentContent_withValidDocument_extractsAndReturns() {
            String itemId = "item-456";
            String metadataJson = "{\"name\":\"document.txt\",\"size\":512,\"webUrl\":\"http://ex\",\"parentReference\":{}}";
            byte[] fileBytes = "content here".getBytes();
            String extractedText = "content here";

            when(graphClient.fetchItemMetadata(TOKEN, itemId)).thenReturn(metadataJson);
            when(graphClient.downloadItemContent(TOKEN, itemId)).thenReturn(fileBytes);
            when(contentExtractor.extractText(fileBytes, "document.txt")).thenReturn(extractedText);

            String result = service.getDocumentContent(TOKEN, itemId);

            assertThat(result)
                    .isNotNull()
                    .contains("document.txt");
        }

        @Test
        void getDocumentContent_withNullItemId_throwsException() {
            assertThatThrownBy(() -> service.getDocumentContent(TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("itemId must not be empty");
        }

        @Test
        void getDocumentContent_withUnsupportedType_throwsException() {
            String itemId = "item-789";
            String metadataJson = "{\"name\":\"presentation.pptx\",\"size\":512,\"webUrl\":\"http://ex\",\"parentReference\":{}}";

            when(graphClient.fetchItemMetadata(TOKEN, itemId)).thenReturn(metadataJson);
            when(validator.validateContentType("pptx")).thenReturn("PPT and PPTX files are not supported");

            assertThatThrownBy(() -> service.getDocumentContent(TOKEN, itemId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not supported");
        }

        @Test
        void getDocumentContent_withOversizedFile_throwsException() {
            String itemId = "item-large";
            // 10 MB + 1 byte
            long oversizedBytes = (10L * 1_024 * 1_024) + 1;
            String metadataJson = "{\"name\":\"huge.pdf\",\"size\":" + oversizedBytes + ",\"webUrl\":\"http://ex\",\"parentReference\":{}}";

            when(graphClient.fetchItemMetadata(TOKEN, itemId)).thenReturn(metadataJson);
            when(validator.validateContentType("pdf")).thenReturn(null);

            assertThatThrownBy(() -> service.getDocumentContent(TOKEN, itemId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("too large");
        }

        @Test
        void getDocumentContent_withEmptyFile_throwsException() {
            String itemId = "item-empty";
            String metadataJson = "{\"name\":\"empty.txt\",\"size\":0,\"webUrl\":\"http://ex\",\"parentReference\":{}}";

            when(graphClient.fetchItemMetadata(TOKEN, itemId)).thenReturn(metadataJson);
            when(validator.validateContentType("txt")).thenReturn(null);
            when(graphClient.downloadItemContent(TOKEN, itemId)).thenReturn(new byte[0]);

            assertThatThrownBy(() -> service.getDocumentContent(TOKEN, itemId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        void getDocumentContent_withNoExtractableText_throwsException() {
            String itemId = "item-invalid";
            String metadataJson = "{\"name\":\"invalid.txt\",\"size\":100,\"webUrl\":\"http://ex\",\"parentReference\":{}}";
            byte[] fileBytes = "   ".getBytes();

            when(graphClient.fetchItemMetadata(TOKEN, itemId)).thenReturn(metadataJson);
            when(validator.validateContentType("txt")).thenReturn(null);
            when(graphClient.downloadItemContent(TOKEN, itemId)).thenReturn(fileBytes);
            when(contentExtractor.extractText(fileBytes, "invalid.txt")).thenReturn("   ");

            assertThatThrownBy(() -> service.getDocumentContent(TOKEN, itemId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No text could be extracted");
        }
    }

    @Nested
    class GetFileDownloadUrlTests {

        @Test
        void getFileDownloadUrl_withValidItemId_succeeds() {
            String itemId = "download-123";
            String metadataJson = "{\"name\":\"file.pdf\",\"size\":1024,\"file\":{\"mimeType\":\"application/pdf\"}}";
            String preSignedUrl = "https://cdn.example.com/...?token=xyz";

            when(graphClient.fetchItemFullMetadata(TOKEN, itemId)).thenReturn(metadataJson);
            when(graphClient.fetchDownloadUrl(TOKEN, itemId)).thenReturn(preSignedUrl);

            String result = service.getFileDownloadUrl(TOKEN, itemId);

            assertThat(result)
                    .isNotNull()
                    .contains("file.pdf")
                    .contains("downloadUrl")
                    .contains("download=1");
        }

        @Test
        void getFileDownloadUrl_withNullUrl_throwsException() {
            String itemId = "download-fail";
            String metadataJson = "{\"name\":\"file.pdf\",\"size\":1024,\"file\":{\"mimeType\":\"application/pdf\"}}";

            when(graphClient.fetchItemFullMetadata(TOKEN, itemId)).thenReturn(metadataJson);
            when(graphClient.fetchDownloadUrl(TOKEN, itemId)).thenReturn(null);

            assertThatThrownBy(() -> service.getFileDownloadUrl(TOKEN, itemId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Could not obtain");
        }
    }

    @Nested
    class ListSitesTests {

        @Test
        void listSites_withoutTop_usesDefault() {
            String sitesJson = "{\"value\":[{\"id\":\"site-1\",\"name\":\"TestSite\"}]}";

            when(graphClient.fetchUserSites(TOKEN, 20)).thenReturn(sitesJson);

            String result = service.listSites(TOKEN, null);

            assertThat(result).isNotNull();
            verify(graphClient).fetchUserSites(TOKEN, 20);
        }

        @Test
        void listSites_withCustomTop_respectsLimit() {
            String sitesJson = "{\"value\":[]}";

            when(graphClient.fetchUserSites(TOKEN, 15)).thenReturn(sitesJson);

            String result = service.listSites(TOKEN, 15);

            assertThat(result).isNotNull();
            verify(graphClient).fetchUserSites(TOKEN, 15);
        }
    }

    @Nested
    class GetSiteDetailsTests {

        @Test
        void getSiteDetails_withValidSiteId_succeeds() {
            String siteId = "site-abc";
            String siteJson = "{\"id\":\"site-abc\",\"name\":\"TestSite\"}";

            when(graphClient.fetchSiteDetails(TOKEN, siteId)).thenReturn(siteJson);

            String result = service.getSiteDetails(TOKEN, siteId);

            assertThat(result).isNotNull();
        }

        @Test
        void getSiteDetails_withNullSiteId_throwsException() {
            assertThatThrownBy(() -> service.getSiteDetails(TOKEN, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("siteId must not be empty");
        }
    }

    @Nested
    class ListLibrariesTests {

        @Test
        void listLibraries_withValidSiteId_succeeds() {
            String siteId = "site-xyz";
            String librariesJson = "{\"value\":[{\"id\":\"lib-1\",\"name\":\"Documents\"}]}";

            when(graphClient.fetchSiteLibraries(TOKEN, siteId, 20)).thenReturn(librariesJson);

            String result = service.listLibraries(TOKEN, siteId, null);

            assertThat(result).isNotNull();
        }

        @Test
        void listLibraries_withNullSiteId_throwsException() {
            assertThatThrownBy(() -> service.listLibraries(TOKEN, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("siteId must not be empty");
        }
    }
}



