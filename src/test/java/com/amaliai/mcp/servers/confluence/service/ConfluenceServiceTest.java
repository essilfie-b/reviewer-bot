package com.amaliai.mcp.servers.confluence.service;

import com.amaliai.mcp.servers.confluence.client.ConfluenceGraphClient;
import com.amaliai.mcp.servers.confluence.dto.SpaceInfo;
import com.amaliai.mcp.servers.confluence.util.ConfluenceServiceUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ConfluenceService}.
 *
 * Coverage strategy
 * -----------------
 * ConfluenceServiceUtil is a static utility class; we mock it with
 * {@link MockedStatic} so no real JSON parsing runs and every code path
 * inside ConfluenceService is exercised in isolation.
 *
 * Every method is tested for:
 *  - happy path (delegates correctly, returns util result)
 *  - validation branches (null / blank inputs → IllegalArgumentException)
 *  - limit clamping branches (null / ≤0 → default, over max → max, in-range → as-is)
 */
@ExtendWith(MockitoExtension.class)
class ConfluenceServiceTest {

    private static final String TOKEN    = "tok";
    private static final String CLOUD_ID = "cloud-1";
    private static final String RAW      = "{\"raw\":true}";
    private static final String PARSED   = "{\"parsed\":true}";

    @Mock  private ConfluenceGraphClient confluenceClient;
    @InjectMocks private ConfluenceService service;

    /** Opened/closed per test so stubs don't leak between tests. */
    private MockedStatic<ConfluenceServiceUtil> utilMock;

    @BeforeEach
    void openStaticMock() {
        utilMock = Mockito.mockStatic(ConfluenceServiceUtil.class);
    }

    @AfterEach
    void closeStaticMock() {
        utilMock.close();
    }

    // -------------------------------------------------------------------------
    // searchContent()
    // -------------------------------------------------------------------------
    @Nested
    class SearchContentTests {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "  "})
        void searchContent_blankQuery_throwsIllegalArgument(String query) {
            assertThatThrownBy(() -> service.searchContent(TOKEN, CLOUD_ID, query, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("query must not be empty");
        }

        @Test
        void searchContent_queryTooLong_throwsIllegalArgument() {
            String longQuery = "a".repeat(1_001);
            assertThatThrownBy(() -> service.searchContent(TOKEN, CLOUD_ID, longQuery, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1000");
        }

        @Test
        void searchContent_nullLimit_usesDefaultLimit() {
            utilMock.when(() -> ConfluenceServiceUtil.buildCql(anyString(), isNull())).thenReturn("cql");
            when(confluenceClient.search(TOKEN, CLOUD_ID, "cql", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSearchResponse(RAW)).thenReturn(PARSED);

            String result = service.searchContent(TOKEN, CLOUD_ID, "test", null, null);

            assertThat(result).isEqualTo(PARSED);
            verify(confluenceClient).search(TOKEN, CLOUD_ID, "cql", 20);
        }

        @Test
        void searchContent_zeroLimit_usesDefaultLimit() {
            utilMock.when(() -> ConfluenceServiceUtil.buildCql(anyString(), isNull())).thenReturn("cql");
            when(confluenceClient.search(TOKEN, CLOUD_ID, "cql", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSearchResponse(RAW)).thenReturn(PARSED);

            service.searchContent(TOKEN, CLOUD_ID, "test", null, 0);

            verify(confluenceClient).search(TOKEN, CLOUD_ID, "cql", 20);
        }

        @Test
        void searchContent_negativeLimit_usesDefaultLimit() {
            utilMock.when(() -> ConfluenceServiceUtil.buildCql(anyString(), isNull())).thenReturn("cql");
            when(confluenceClient.search(TOKEN, CLOUD_ID, "cql", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSearchResponse(RAW)).thenReturn(PARSED);

            service.searchContent(TOKEN, CLOUD_ID, "test", null, -5);

            verify(confluenceClient).search(TOKEN, CLOUD_ID, "cql", 20);
        }

        @Test
        void searchContent_limitExceedsMax_clampsToMax() {
            utilMock.when(() -> ConfluenceServiceUtil.buildCql(anyString(), isNull())).thenReturn("cql");
            when(confluenceClient.search(TOKEN, CLOUD_ID, "cql", 50)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSearchResponse(RAW)).thenReturn(PARSED);

            service.searchContent(TOKEN, CLOUD_ID, "test", null, 999);

            verify(confluenceClient).search(TOKEN, CLOUD_ID, "cql", 50);
        }

        @Test
        void searchContent_limitWithinBounds_usesProvidedLimit() {
            utilMock.when(() -> ConfluenceServiceUtil.buildCql(anyString(), isNull())).thenReturn("cql");
            when(confluenceClient.search(TOKEN, CLOUD_ID, "cql", 30)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSearchResponse(RAW)).thenReturn(PARSED);

            service.searchContent(TOKEN, CLOUD_ID, "test", null, 30);

            verify(confluenceClient).search(TOKEN, CLOUD_ID, "cql", 30);
        }

        @Test
        void searchContent_withSpaceKey_passesThroughToUtil() {
            utilMock.when(() -> ConfluenceServiceUtil.buildCql("test", "ENG")).thenReturn("cql-with-space");
            when(confluenceClient.search(TOKEN, CLOUD_ID, "cql-with-space", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSearchResponse(RAW)).thenReturn(PARSED);

            String result = service.searchContent(TOKEN, CLOUD_ID, "test", "ENG", null);

            assertThat(result).isEqualTo(PARSED);
        }
    }

    // -------------------------------------------------------------------------
    // getPage()
    // -------------------------------------------------------------------------
    @Nested
    class GetPageTests {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "  "})
        void getPage_blankPageId_throwsIllegalArgument(String pageId) {
            assertThatThrownBy(() -> service.getPage(TOKEN, CLOUD_ID, pageId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pageId must not be empty");
        }

        @Test
        void getPage_validPageId_returnsUtilResult() {
            when(confluenceClient.getPage(TOKEN, CLOUD_ID, "p1")).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseV2PageResponse(RAW)).thenReturn(PARSED);

            assertThat(service.getPage(TOKEN, CLOUD_ID, "p1")).isEqualTo(PARSED);
        }
    }

    // -------------------------------------------------------------------------
    // getPageContent()
    // -------------------------------------------------------------------------
    @Nested
    class GetPageContentTests {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "  "})
        void getPageContent_blankPageId_throwsIllegalArgument(String pageId) {
            assertThatThrownBy(() -> service.getPageContent(TOKEN, CLOUD_ID, pageId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pageId must not be empty");
        }

        @Test
        void getPageContent_validPageId_returnsUtilResult() {
            when(confluenceClient.getPageWithContent(TOKEN, CLOUD_ID, "p1")).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseV2PageContentResponse(RAW)).thenReturn(PARSED);

            assertThat(service.getPageContent(TOKEN, CLOUD_ID, "p1")).isEqualTo(PARSED);
        }
    }

    // -------------------------------------------------------------------------
    // listPages()
    // -------------------------------------------------------------------------
    @Nested
    class ListPagesTests {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "  "})
        void listPages_blankSpaceKey_throwsIllegalArgument(String spaceKey) {
            assertThatThrownBy(() -> service.listPages(TOKEN, CLOUD_ID, spaceKey, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("spaceKey must not be empty");
        }

        @Test
        void listPages_nullLimit_usesDefaultLimit() {
            SpaceInfo spaceInfo = new SpaceInfo("42", "ENG", "Engineering");
            when(confluenceClient.getSpaceByKey(TOKEN, CLOUD_ID, "ENG")).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSpaceResult(RAW, "ENG")).thenReturn(spaceInfo);
            when(confluenceClient.listPagesBySpaceId(TOKEN, CLOUD_ID, "42", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parsePagesListResponse(RAW, "ENG", "Engineering"))
                    .thenReturn(PARSED);

            String result = service.listPages(TOKEN, CLOUD_ID, "ENG", null);

            assertThat(result).isEqualTo(PARSED);
            verify(confluenceClient).listPagesBySpaceId(TOKEN, CLOUD_ID, "42", 20);
        }

        @Test
        void listPages_zeroLimit_usesDefaultLimit() {
            SpaceInfo spaceInfo = new SpaceInfo("42", "ENG", "Engineering");
            when(confluenceClient.getSpaceByKey(TOKEN, CLOUD_ID, "ENG")).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSpaceResult(RAW, "ENG")).thenReturn(spaceInfo);
            when(confluenceClient.listPagesBySpaceId(TOKEN, CLOUD_ID, "42", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parsePagesListResponse(RAW, "ENG", "Engineering"))
                    .thenReturn(PARSED);

            service.listPages(TOKEN, CLOUD_ID, "ENG", 0);

            verify(confluenceClient).listPagesBySpaceId(TOKEN, CLOUD_ID, "42", 20);
        }

        @Test
        void listPages_negativeLimit_usesDefaultLimit() {
            SpaceInfo spaceInfo = new SpaceInfo("42", "ENG", "Engineering");
            when(confluenceClient.getSpaceByKey(TOKEN, CLOUD_ID, "ENG")).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSpaceResult(RAW, "ENG")).thenReturn(spaceInfo);
            when(confluenceClient.listPagesBySpaceId(TOKEN, CLOUD_ID, "42", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parsePagesListResponse(RAW, "ENG", "Engineering"))
                    .thenReturn(PARSED);

            service.listPages(TOKEN, CLOUD_ID, "ENG", -1);

            verify(confluenceClient).listPagesBySpaceId(TOKEN, CLOUD_ID, "42", 20);
        }

        @Test
        void listPages_limitExceedsMax_clampsToMax() {
            SpaceInfo spaceInfo = new SpaceInfo("42", "ENG", "Engineering");
            when(confluenceClient.getSpaceByKey(TOKEN, CLOUD_ID, "ENG")).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSpaceResult(RAW, "ENG")).thenReturn(spaceInfo);
            when(confluenceClient.listPagesBySpaceId(TOKEN, CLOUD_ID, "42", 50)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parsePagesListResponse(RAW, "ENG", "Engineering"))
                    .thenReturn(PARSED);

            service.listPages(TOKEN, CLOUD_ID, "ENG", 999);

            verify(confluenceClient).listPagesBySpaceId(TOKEN, CLOUD_ID, "42", 50);
        }

        @Test
        void listPages_limitWithinBounds_usesProvidedLimit() {
            SpaceInfo spaceInfo = new SpaceInfo("42", "ENG", "Engineering");
            when(confluenceClient.getSpaceByKey(TOKEN, CLOUD_ID, "ENG")).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSpaceResult(RAW, "ENG")).thenReturn(spaceInfo);
            when(confluenceClient.listPagesBySpaceId(TOKEN, CLOUD_ID, "42", 15)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parsePagesListResponse(RAW, "ENG", "Engineering"))
                    .thenReturn(PARSED);

            service.listPages(TOKEN, CLOUD_ID, "ENG", 15);

            verify(confluenceClient).listPagesBySpaceId(TOKEN, CLOUD_ID, "42", 15);
        }
    }

    // -------------------------------------------------------------------------
    // getSpace()
    // -------------------------------------------------------------------------
    @Nested
    class GetSpaceTests {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "  "})
        void getSpace_blankSpaceId_throwsIllegalArgument(String spaceId) {
            assertThatThrownBy(() -> service.getSpace(TOKEN, CLOUD_ID, spaceId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("spaceId must not be empty");
        }

        @Test
        void getSpace_validSpaceId_trimmedAndDelegated() {
            when(confluenceClient.getSpace(TOKEN, CLOUD_ID, "99")).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSpaceResponse(RAW)).thenReturn(PARSED);

            // Pass with surrounding whitespace to verify trim()
            assertThat(service.getSpace(TOKEN, CLOUD_ID, "  99  ")).isEqualTo(PARSED);
            verify(confluenceClient).getSpace(TOKEN, CLOUD_ID, "99");
        }
    }

    // -------------------------------------------------------------------------
    // listSpaces()
    // -------------------------------------------------------------------------
    @Nested
    class ListSpacesTests {

        @Test
        void listSpaces_nullLimit_usesDefaultLimit() {
            when(confluenceClient.listSpaces(TOKEN, CLOUD_ID, null, null, 25, null)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSpacesListResponse(RAW)).thenReturn(PARSED);

            assertThat(service.listSpaces(TOKEN, CLOUD_ID, null, null, null, null)).isEqualTo(PARSED);
            verify(confluenceClient).listSpaces(TOKEN, CLOUD_ID, null, null, 25, null);
        }

        @Test
        void listSpaces_zeroLimit_usesDefaultLimit() {
            when(confluenceClient.listSpaces(TOKEN, CLOUD_ID, null, null, 25, null)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSpacesListResponse(RAW)).thenReturn(PARSED);

            service.listSpaces(TOKEN, CLOUD_ID, null, null, 0, null);

            verify(confluenceClient).listSpaces(TOKEN, CLOUD_ID, null, null, 25, null);
        }

        @Test
        void listSpaces_negativeLimit_usesDefaultLimit() {
            when(confluenceClient.listSpaces(TOKEN, CLOUD_ID, null, null, 25, null)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSpacesListResponse(RAW)).thenReturn(PARSED);

            service.listSpaces(TOKEN, CLOUD_ID, null, null, -10, null);

            verify(confluenceClient).listSpaces(TOKEN, CLOUD_ID, null, null, 25, null);
        }

        @Test
        void listSpaces_limitExceedsMax_clampsToMax() {
            when(confluenceClient.listSpaces(TOKEN, CLOUD_ID, null, null, 250, null)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSpacesListResponse(RAW)).thenReturn(PARSED);

            service.listSpaces(TOKEN, CLOUD_ID, null, null, 9999, null);

            verify(confluenceClient).listSpaces(TOKEN, CLOUD_ID, null, null, 250, null);
        }

        @Test
        void listSpaces_limitWithinBounds_usesProvidedLimit() {
            when(confluenceClient.listSpaces(TOKEN, CLOUD_ID, "global", "current", 100, "cursor-x"))
                    .thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseSpacesListResponse(RAW)).thenReturn(PARSED);

            assertThat(service.listSpaces(TOKEN, CLOUD_ID, "global", "current", 100, "cursor-x"))
                    .isEqualTo(PARSED);
        }
    }

    // -------------------------------------------------------------------------
    // getAttachments()
    // -------------------------------------------------------------------------
    @Nested
    class GetAttachmentsTests {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "  "})
        void getAttachments_blankPageId_throwsIllegalArgument(String pageId) {
            assertThatThrownBy(() -> service.getAttachments(TOKEN, CLOUD_ID, pageId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pageId must not be empty");
        }

        @Test
        void getAttachments_nullLimit_usesDefaultLimit() {
            when(confluenceClient.getAttachments(TOKEN, CLOUD_ID, "p1", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseAttachmentsResponse(RAW)).thenReturn(PARSED);

            assertThat(service.getAttachments(TOKEN, CLOUD_ID, "p1", null)).isEqualTo(PARSED);
            verify(confluenceClient).getAttachments(TOKEN, CLOUD_ID, "p1", 20);
        }

        @Test
        void getAttachments_zeroLimit_usesDefaultLimit() {
            when(confluenceClient.getAttachments(TOKEN, CLOUD_ID, "p1", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseAttachmentsResponse(RAW)).thenReturn(PARSED);

            service.getAttachments(TOKEN, CLOUD_ID, "p1", 0);

            verify(confluenceClient).getAttachments(TOKEN, CLOUD_ID, "p1", 20);
        }

        @Test
        void getAttachments_negativeLimit_usesDefaultLimit() {
            when(confluenceClient.getAttachments(TOKEN, CLOUD_ID, "p1", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseAttachmentsResponse(RAW)).thenReturn(PARSED);

            service.getAttachments(TOKEN, CLOUD_ID, "p1", -3);

            verify(confluenceClient).getAttachments(TOKEN, CLOUD_ID, "p1", 20);
        }

        @Test
        void getAttachments_limitExceedsMax_clampsToMax() {
            when(confluenceClient.getAttachments(TOKEN, CLOUD_ID, "p1", 50)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseAttachmentsResponse(RAW)).thenReturn(PARSED);

            service.getAttachments(TOKEN, CLOUD_ID, "p1", 200);

            verify(confluenceClient).getAttachments(TOKEN, CLOUD_ID, "p1", 50);
        }

        @Test
        void getAttachments_limitWithinBounds_usesProvidedLimit() {
            when(confluenceClient.getAttachments(TOKEN, CLOUD_ID, "p1", 10)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parseAttachmentsResponse(RAW)).thenReturn(PARSED);

            service.getAttachments(TOKEN, CLOUD_ID, "p1", 10);

            verify(confluenceClient).getAttachments(TOKEN, CLOUD_ID, "p1", 10);
        }
    }

    // -------------------------------------------------------------------------
    // getPageChildren()
    // -------------------------------------------------------------------------
    @Nested
    class GetPageChildrenTests {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "  "})
        void getPageChildren_blankPageId_throwsIllegalArgument(String pageId) {
            assertThatThrownBy(() -> service.getPageChildren(TOKEN, CLOUD_ID, pageId, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pageId must not be empty");
        }

        @Test
        void getPageChildren_nullLimit_usesDefaultLimit() {
            when(confluenceClient.getPageChildren(TOKEN, CLOUD_ID, "p1", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parsePageChildrenResponse(RAW)).thenReturn(PARSED);

            assertThat(service.getPageChildren(TOKEN, CLOUD_ID, "p1", null)).isEqualTo(PARSED);
            verify(confluenceClient).getPageChildren(TOKEN, CLOUD_ID, "p1", 20);
        }

        @Test
        void getPageChildren_zeroLimit_usesDefaultLimit() {
            when(confluenceClient.getPageChildren(TOKEN, CLOUD_ID, "p1", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parsePageChildrenResponse(RAW)).thenReturn(PARSED);

            service.getPageChildren(TOKEN, CLOUD_ID, "p1", 0);

            verify(confluenceClient).getPageChildren(TOKEN, CLOUD_ID, "p1", 20);
        }

        @Test
        void getPageChildren_negativeLimit_usesDefaultLimit() {
            when(confluenceClient.getPageChildren(TOKEN, CLOUD_ID, "p1", 20)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parsePageChildrenResponse(RAW)).thenReturn(PARSED);

            service.getPageChildren(TOKEN, CLOUD_ID, "p1", -1);

            verify(confluenceClient).getPageChildren(TOKEN, CLOUD_ID, "p1", 20);
        }

        @Test
        void getPageChildren_limitExceedsMax_clampsToMax() {
            when(confluenceClient.getPageChildren(TOKEN, CLOUD_ID, "p1", 50)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parsePageChildrenResponse(RAW)).thenReturn(PARSED);

            service.getPageChildren(TOKEN, CLOUD_ID, "p1", 100);

            verify(confluenceClient).getPageChildren(TOKEN, CLOUD_ID, "p1", 50);
        }

        @Test
        void getPageChildren_limitWithinBounds_usesProvidedLimit() {
            when(confluenceClient.getPageChildren(TOKEN, CLOUD_ID, "p1", 5)).thenReturn(RAW);
            utilMock.when(() -> ConfluenceServiceUtil.parsePageChildrenResponse(RAW)).thenReturn(PARSED);

            service.getPageChildren(TOKEN, CLOUD_ID, "p1", 5);

            verify(confluenceClient).getPageChildren(TOKEN, CLOUD_ID, "p1", 5);
        }
    }
}