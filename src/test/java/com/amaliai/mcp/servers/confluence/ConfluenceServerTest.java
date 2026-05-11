package com.amaliai.mcp.servers.confluence;

import com.amaliai.mcp.servers.confluence.service.ConfluenceService;
import com.amaliai.mcp.servers.confluence.util.ConfluenceServerHelper;
import com.amaliai.mcp.servers.confluence.util.ConfluenceTokenManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ConfluenceServer}.
 *
 * Coverage strategy
 * -----------------
 * ConfluenceServer resolves credentials via static helper for all tools.
 * → mock ConfluenceServerHelper.resolveCredentials() with MockedStatic
 *
 * Every method has exactly one code path, so a single happy-path test per
 * tool achieves 100 % instruction and branch coverage.
 */
@ExtendWith(MockitoExtension.class)
class ConfluenceServerTest {

    // ---- shared constants ----
    private static final int    ARMS_USER_ID  = 42;
    private static final String TOKEN         = "access-token";
    private static final String CLOUD_ID      = "cloud-abc";
    private static final String RESULT        = "{\"ok\":true}";

    // ---- mocks ----
    @Mock private ConfluenceService      confluenceService;
    @Mock private ConfluenceTokenManager tokenManager;

    @InjectMocks
    private ConfluenceServer confluenceServer;

    /** Shared static mock for pattern-A tools. Opened/closed per test to avoid leaks. */
    private MockedStatic<ConfluenceServerHelper> helperMock;

    /** A reusable Credentials stub returned by the static helper. */
    private ConfluenceServerHelper.Credentials creds;

    @BeforeEach
    void setUp() {
        helperMock = Mockito.mockStatic(ConfluenceServerHelper.class);
        creds = new ConfluenceServerHelper.Credentials(TOKEN, CLOUD_ID);
        helperMock.when(() -> ConfluenceServerHelper.resolveCredentials(ARMS_USER_ID, tokenManager))
                .thenReturn(creds);
    }

    @AfterEach
    void tearDown() {
        helperMock.close();
    }

    // =========================================================================
    // Pattern A — credentials resolved via ConfluenceServerHelper
    // =========================================================================

    @Nested
    class PatternAToolsTests {

        // --- searchConfluenceContent ---

        @Test
        void searchConfluenceContent_withAllParams_delegatesCorrectly() {
            when(confluenceService.searchContent(TOKEN, CLOUD_ID, "query", "ENG", 10))
                    .thenReturn(RESULT);

            String result = confluenceServer.searchConfluenceContent(ARMS_USER_ID, "query", "ENG", 10);

            assertThat(result).isEqualTo(RESULT);
            verify(confluenceService).searchContent(TOKEN, CLOUD_ID, "query", "ENG", 10);
        }

        @Test
        void searchConfluenceContent_withNullOptionals_delegatesNullsThrough() {
            when(confluenceService.searchContent(TOKEN, CLOUD_ID, "query", null, null))
                    .thenReturn(RESULT);

            String result = confluenceServer.searchConfluenceContent(ARMS_USER_ID, "query", null, null);

            assertThat(result).isEqualTo(RESULT);
        }

        // --- getConfluencePage ---

        @Test
        void getConfluencePage_delegatesCorrectly() {
            when(confluenceService.getPage(TOKEN, CLOUD_ID, "page-1")).thenReturn(RESULT);

            String result = confluenceServer.getConfluencePage(ARMS_USER_ID, "page-1");

            assertThat(result).isEqualTo(RESULT);
            verify(confluenceService).getPage(TOKEN, CLOUD_ID, "page-1");
        }

        // --- getConfluencePageContent ---

        @Test
        void getConfluencePageContent_delegatesCorrectly() {
            when(confluenceService.getPageContent(TOKEN, CLOUD_ID, "page-2")).thenReturn(RESULT);

            String result = confluenceServer.getConfluencePageContent(ARMS_USER_ID, "page-2");

            assertThat(result).isEqualTo(RESULT);
            verify(confluenceService).getPageContent(TOKEN, CLOUD_ID, "page-2");
        }

        // --- listConfluencePages ---

        @Test
        void listConfluencePages_withLimit_delegatesCorrectly() {
            when(confluenceService.listPages(TOKEN, CLOUD_ID, "ENG", 25, null)).thenReturn(RESULT);

            String result = confluenceServer.listConfluencePages(ARMS_USER_ID, "ENG", 25, null);

            assertThat(result).isEqualTo(RESULT);
            verify(confluenceService).listPages(TOKEN, CLOUD_ID, "ENG", 25, null);
        }

        @Test
        void listConfluencePages_withNullLimit_delegatesNullThrough() {
            when(confluenceService.listPages(TOKEN, CLOUD_ID, "ENG", null, null)).thenReturn(RESULT);

            String result = confluenceServer.listConfluencePages(ARMS_USER_ID, "ENG", null, null);

            assertThat(result).isEqualTo(RESULT);
        }
    }

    // =========================================================================
    // Remaining tools (also via ConfluenceServerHelper)
    // =========================================================================

    @Nested
    class RemainingToolsTests {

        // --- getConfluenceSpace ---

        @Test
        void getConfluenceSpace_delegatesCorrectly() {
            // ConfluenceServer expects a normalized spaceKey (passed-through as-is in this test)
            when(confluenceService.getSpace(TOKEN, CLOUD_ID, "ENG")).thenReturn(RESULT);

            String result = confluenceServer.getConfluenceSpace(ARMS_USER_ID, "ENG");

            assertThat(result).isEqualTo(RESULT);
            verify(confluenceService).getSpace(TOKEN, CLOUD_ID, "ENG");
        }

        // --- listConfluenceSpaces ---

        @Test
        void listConfluenceSpaces_withAllParams_delegatesCorrectly() {
            when(confluenceService.listSpaces(TOKEN, CLOUD_ID, "global", "current", "eng", 50, "cursor-x"))
                    .thenReturn(RESULT);

            String result = confluenceServer.listConfluenceSpaces(
                    ARMS_USER_ID, "global", "current", "eng", 50, "cursor-x");

            assertThat(result).isEqualTo(RESULT);
            verify(confluenceService).listSpaces(TOKEN, CLOUD_ID, "global", "current", "eng", 50, "cursor-x");
        }

        @Test
        void listConfluenceSpaces_withNullOptionals_delegatesNullsThrough() {
            when(confluenceService.listSpaces(TOKEN, CLOUD_ID, null, null, null, null, null))
                    .thenReturn(RESULT);

            String result = confluenceServer.listConfluenceSpaces(ARMS_USER_ID, null, null, null, null, null);

            assertThat(result).isEqualTo(RESULT);
        }

        // --- getConfluencePageChildren ---

        @Test
        void getConfluencePageChildren_withLimit_delegatesCorrectly() {
            when(confluenceService.getPageChildren(TOKEN, CLOUD_ID, "page-3", 15, null)).thenReturn(RESULT);

            String result = confluenceServer.getConfluencePageChildren(ARMS_USER_ID, "page-3", 15, null);

            assertThat(result).isEqualTo(RESULT);
            verify(confluenceService).getPageChildren(TOKEN, CLOUD_ID, "page-3", 15, null);
        }

        @Test
        void getConfluencePageChildren_withNullLimit_delegatesNullThrough() {
            when(confluenceService.getPageChildren(TOKEN, CLOUD_ID, "page-3", null, null)).thenReturn(RESULT);

            String result = confluenceServer.getConfluencePageChildren(ARMS_USER_ID, "page-3", null, null);

            assertThat(result).isEqualTo(RESULT);
        }

        // --- getConfluenceAttachments ---

        @Test
        void getConfluenceAttachments_withLimit_delegatesCorrectly() {
            when(confluenceService.getAttachments(TOKEN, CLOUD_ID, "page-4", 5, null)).thenReturn(RESULT);

            String result = confluenceServer.getConfluenceAttachments(ARMS_USER_ID, "page-4", 5, null);

            assertThat(result).isEqualTo(RESULT);
            verify(confluenceService).getAttachments(TOKEN, CLOUD_ID, "page-4", 5, null);
        }

        @Test
        void getConfluenceAttachments_withNullLimit_delegatesNullThrough() {
            when(confluenceService.getAttachments(TOKEN, CLOUD_ID, "page-4", null, null)).thenReturn(RESULT);

            String result = confluenceServer.getConfluenceAttachments(ARMS_USER_ID, "page-4", null, null);

            assertThat(result).isEqualTo(RESULT);
        }
    }
}