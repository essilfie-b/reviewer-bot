package com.amaliai.mcp.servers.sharepoint;

import com.amaliai.mcp.servers.sharepoint.service.SharePointService;
import com.amaliai.mcp.servers.sharepoint.util.SharePointTokenManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SharePointServer}.
 *
 * Coverage strategy
 * -----------------
 * SharePointServer is a thin delegation layer that resolves credentials
 * and forwards to the service. Each tool method has one happy path,
 * achieving 100% coverage with one test per tool.
 */
@ExtendWith(MockitoExtension.class)
class SharePointServerTest {

    private static final int ARMS_USER_ID = 42;
    private static final UUID INTEGRATION_ID = UUID.randomUUID();
    private static final String TOKEN = "test-token";
    private static final String RESULT = "{\"ok\":true}";

    @Mock private SharePointService sharePointService;
    @Mock private SharePointTokenManager tokenManager;

    @InjectMocks
    private SharePointServer sharePointServer;

    @BeforeEach
    void setUp() {
        when(tokenManager.resolveIntegrationId()).thenReturn(INTEGRATION_ID);
        when(tokenManager.getAccessToken(ARMS_USER_ID, INTEGRATION_ID)).thenReturn(TOKEN);
    }

    @Nested
    class GetDocumentsTests {

        @Test
        void getDocuments_delegatesCorrectly() {
            when(sharePointService.listDocuments(TOKEN)).thenReturn(RESULT);

            String result = sharePointServer.getDocuments(ARMS_USER_ID);

            assertThat(result).isEqualTo(RESULT);
            verify(tokenManager).resolveIntegrationId();
            verify(tokenManager).getAccessToken(ARMS_USER_ID, INTEGRATION_ID);
            verify(sharePointService).listDocuments(TOKEN);
        }
    }

    @Nested
    class SearchDocumentsTests {

        @Test
        void searchDocuments_withAllFilterParams_delegatesCorrectly() {
            SearchFilter filter = new SearchFilter("docx", "John", "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z", 10);
            when(sharePointService.searchDocuments(TOKEN, "test", "docx", "John", "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z", 10))
                    .thenReturn(RESULT);

            String result = sharePointServer.searchDocuments(ARMS_USER_ID, "test", filter);

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).searchDocuments(TOKEN, "test", "docx", "John", "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z", 10);
        }

        @Test
        void searchDocuments_withNullFilter_delegatesNullsCorrectly() {
            when(sharePointService.searchDocuments(TOKEN, "test", null, null, null, null, null))
                    .thenReturn(RESULT);

            String result = sharePointServer.searchDocuments(ARMS_USER_ID, "test", null);

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).searchDocuments(TOKEN, "test", null, null, null, null, null);
        }

        @Test
        void searchDocuments_withPartialFilter_delegatesPartialFiltersCorrectly() {
            SearchFilter filter = new SearchFilter("pdf", null, null, null, 25);
            when(sharePointService.searchDocuments(TOKEN, "query", "pdf", null, null, null, 25))
                    .thenReturn(RESULT);

            String result = sharePointServer.searchDocuments(ARMS_USER_ID, "query", filter);

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).searchDocuments(TOKEN, "query", "pdf", null, null, null, 25);
        }
    }

    @Nested
    class GetFolderContentsTests {

        @Test
        void getFolderContents_delegatesCorrectly() {
            String folderId = "folder-123";
            when(sharePointService.getFolderContents(TOKEN, folderId)).thenReturn(RESULT);

            String result = sharePointServer.getFolderContents(ARMS_USER_ID, folderId);

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).getFolderContents(TOKEN, folderId);
        }
    }

    @Nested
    class GetFileMetadataTests {

        @Test
        void getFileMetadata_delegatesCorrectly() {
            String itemId = "item-456";
            when(sharePointService.getFileMetadata(TOKEN, itemId)).thenReturn(RESULT);

            String result = sharePointServer.getFileMetadata(ARMS_USER_ID, itemId);

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).getFileMetadata(TOKEN, itemId);
        }
    }

    @Nested
    class GetDocumentContentTests {

        @Test
        void getDocumentContent_delegatesCorrectly() {
            String itemId = "item-789";
            when(sharePointService.getDocumentContent(TOKEN, itemId)).thenReturn(RESULT);

            String result = sharePointServer.getDocumentContent(ARMS_USER_ID, itemId);

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).getDocumentContent(TOKEN, itemId);
        }
    }

    @Nested
    class ListSitesTests {

        @Test
        void listSites_withoutTop_delegatesCorrectly() {
            when(sharePointService.listSites(TOKEN, null)).thenReturn(RESULT);

            String result = sharePointServer.listSites(ARMS_USER_ID, null);

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).listSites(TOKEN, null);
        }

        @Test
        void listSites_withTop_delegatesCorrectly() {
            when(sharePointService.listSites(TOKEN, 15)).thenReturn(RESULT);

            String result = sharePointServer.listSites(ARMS_USER_ID, 15);

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).listSites(TOKEN, 15);
        }
    }

    @Nested
    class GetSiteDetailsTests {

        @Test
        void getSiteDetails_delegatesCorrectly() {
            String siteId = "site-001";
            when(sharePointService.getSiteDetails(TOKEN, siteId)).thenReturn(RESULT);

            String result = sharePointServer.getSiteDetails(ARMS_USER_ID, siteId);

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).getSiteDetails(TOKEN, siteId);
        }
    }

    @Nested
    class ListLibrariesTests {

        @Test
        void listLibraries_withoutTop_delegatesCorrectly() {
            String siteId = "site-002";
            when(sharePointService.listLibraries(TOKEN, siteId, null)).thenReturn(RESULT);

            String result = sharePointServer.listLibraries(ARMS_USER_ID, siteId, null);

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).listLibraries(TOKEN, siteId, null);
        }

        @Test
        void listLibraries_withTop_delegatesCorrectly() {
            String siteId = "site-003";
            when(sharePointService.listLibraries(TOKEN, siteId, 30)).thenReturn(RESULT);

            String result = sharePointServer.listLibraries(ARMS_USER_ID, siteId, 30);

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).listLibraries(TOKEN, siteId, 30);
        }
    }

    @Nested
    class DownloadFileTests {

        @Test
        void downloadFile_delegatesCorrectly() {
            String itemId = "item-download-001";
            when(sharePointService.getFileDownloadUrl(TOKEN, itemId)).thenReturn(RESULT);

            String result = sharePointServer.downloadFile(ARMS_USER_ID, itemId);

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).getFileDownloadUrl(TOKEN, itemId);
        }
    }

    @Nested
    class CreateSharingLinkTests {

        @Test
        void createSharingLink_delegatesCorrectly() {
            String itemId = "item-share-001";
            when(sharePointService.createSharingLink(TOKEN, itemId, "edit", "anonymous")).thenReturn(RESULT);

            String result = sharePointServer.createSharingLink(ARMS_USER_ID, itemId, "edit", "anonymous");

            assertThat(result).isEqualTo(RESULT);
            verify(sharePointService).createSharingLink(TOKEN, itemId, "edit", "anonymous");
        }
    }
}

