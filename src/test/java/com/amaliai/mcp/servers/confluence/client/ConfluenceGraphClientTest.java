package com.amaliai.mcp.servers.confluence.client;

import com.amaliai.mcp.servers.confluence.exception.ConfluenceAuthException;
import com.amaliai.mcp.servers.confluence.exception.ConfluenceOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConfluenceGraphClient}.
 *
 * Strategy
 * --------
 * RestClient uses a fluent builder chain:
 *   client.get() → RequestHeadersUriSpec → uri(fn) → RequestHeadersSpec → header(...) → retrieve() → ResponseSpec → body(Class)
 *
 * We stub each link of this chain with Mockito so that every code path
 * (success, 401, 403, other 4xx) is exercised for every public method,
 * giving JaCoCo 100 % instruction + branch coverage.
 */
@ExtendWith(MockitoExtension.class)
class ConfluenceGraphClientTest {

    // --- Chain mocks ---
    @Mock private RestClient restClient;
    @Mock private RestClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock private RestClient.RequestHeadersSpec<?> headersSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    private ConfluenceGraphClient client;

    private static final String TOKEN    = "test-token";
    private static final String CLOUD_ID = "cloud-123";
    private static final String BODY     = "{\"results\":[]}";

    @BeforeEach
    void setUp() {
        client = new ConfluenceGraphClient(restClient);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void stubSuccess(String responseBody) {
        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(any(Function.class));
        doReturn(headersSpec).when(headersSpec).header(eq(HttpHeaders.AUTHORIZATION), anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        doReturn(responseBody).when(responseSpec).body(String.class);
    }

    @SuppressWarnings("unchecked")
    private void stubHttpError(HttpStatus status) {
        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(any(Function.class));
        doReturn(headersSpec).when(headersSpec).header(eq(HttpHeaders.AUTHORIZATION), anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        doThrow(HttpClientErrorException.create(status, status.getReasonPhrase(),
                HttpHeaders.EMPTY, new byte[0], null))
                .when(responseSpec).body(String.class);
    }

    // ------------------------------------------------------------------
    // search()
    // ------------------------------------------------------------------
    @Nested
    class SearchTests {

        @Test
        void search_success_returnsBody() {
            stubSuccess(BODY);
            String result = client.search(TOKEN, CLOUD_ID, "type=page", 10);
            assertThat(result).isEqualTo(BODY);
        }

        @ParameterizedTest
        @ValueSource(ints = {401, 403})
        void search_authError_throwsConfluenceAuthException(int statusCode) {
            stubHttpError(HttpStatus.valueOf(statusCode));
            assertThatThrownBy(() -> client.search(TOKEN, CLOUD_ID, "type=page", 10))
                    .isInstanceOf(ConfluenceAuthException.class)
                    .hasMessageContaining(String.valueOf(statusCode));
        }

        @Test
        void search_otherHttpError_throwsConfluenceOperationException() {
            stubHttpError(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThatThrownBy(() -> client.search(TOKEN, CLOUD_ID, "type=page", 10))
                    .isInstanceOf(ConfluenceOperationException.class);
        }
    }

    // ------------------------------------------------------------------
    // getPage()
    // ------------------------------------------------------------------
    @Nested
    class GetPageTests {

        @Test
        void getPage_success_returnsBody() {
            stubSuccess(BODY);
            assertThat(client.getPage(TOKEN, CLOUD_ID, "page-1")).isEqualTo(BODY);
        }

        @ParameterizedTest
        @ValueSource(ints = {401, 403})
        void getPage_authError_throwsConfluenceAuthException(int statusCode) {
            stubHttpError(HttpStatus.valueOf(statusCode));
            assertThatThrownBy(() -> client.getPage(TOKEN, CLOUD_ID, "page-1"))
                    .isInstanceOf(ConfluenceAuthException.class);
        }

        @Test
        void getPage_otherHttpError_throwsConfluenceOperationException() {
            stubHttpError(HttpStatus.NOT_FOUND);
            assertThatThrownBy(() -> client.getPage(TOKEN, CLOUD_ID, "page-1"))
                    .isInstanceOf(ConfluenceOperationException.class);
        }
    }

    // ------------------------------------------------------------------
    // getPageWithContent()
    // ------------------------------------------------------------------
    @Nested
    class GetPageWithContentTests {

        @Test
        void getPageWithContent_success_returnsBody() {
            stubSuccess(BODY);
            assertThat(client.getPageWithContent(TOKEN, CLOUD_ID, "page-1")).isEqualTo(BODY);
        }

        @ParameterizedTest
        @ValueSource(ints = {401, 403})
        void getPageWithContent_authError_throwsConfluenceAuthException(int statusCode) {
            stubHttpError(HttpStatus.valueOf(statusCode));
            assertThatThrownBy(() -> client.getPageWithContent(TOKEN, CLOUD_ID, "page-1"))
                    .isInstanceOf(ConfluenceAuthException.class);
        }

        @Test
        void getPageWithContent_otherHttpError_throwsConfluenceOperationException() {
            stubHttpError(HttpStatus.BAD_REQUEST);
            assertThatThrownBy(() -> client.getPageWithContent(TOKEN, CLOUD_ID, "page-1"))
                    .isInstanceOf(ConfluenceOperationException.class);
        }
    }

    // ------------------------------------------------------------------
    // getSpaceByKey()
    // ------------------------------------------------------------------
    @Nested
    class GetSpaceByKeyTests {

        @Test
        void getSpaceByKey_success_returnsBody() {
            stubSuccess(BODY);
            assertThat(client.getSpaceByKey(TOKEN, CLOUD_ID, "ENG")).isEqualTo(BODY);
        }

        @ParameterizedTest
        @ValueSource(ints = {401, 403})
        void getSpaceByKey_authError_throwsConfluenceAuthException(int statusCode) {
            stubHttpError(HttpStatus.valueOf(statusCode));
            assertThatThrownBy(() -> client.getSpaceByKey(TOKEN, CLOUD_ID, "ENG"))
                    .isInstanceOf(ConfluenceAuthException.class);
        }

        @Test
        void getSpaceByKey_otherHttpError_throwsConfluenceOperationException() {
            stubHttpError(HttpStatus.GONE);
            assertThatThrownBy(() -> client.getSpaceByKey(TOKEN, CLOUD_ID, "ENG"))
                    .isInstanceOf(ConfluenceOperationException.class);
        }
    }

    // ------------------------------------------------------------------
    // listPagesBySpaceId()
    // ------------------------------------------------------------------
    @Nested
    class ListPagesBySpaceIdTests {

        @Test
        void listPagesBySpaceId_success_returnsBody() {
            stubSuccess(BODY);
            assertThat(client.listPagesBySpaceId(TOKEN, CLOUD_ID, "space-42", 25, null)).isEqualTo(BODY);
        }

        @ParameterizedTest
        @ValueSource(ints = {401, 403})
        void listPagesBySpaceId_authError_throwsConfluenceAuthException(int statusCode) {
            stubHttpError(HttpStatus.valueOf(statusCode));
            assertThatThrownBy(() -> client.listPagesBySpaceId(TOKEN, CLOUD_ID, "space-42", 25, null))
                    .isInstanceOf(ConfluenceAuthException.class);
        }

        @Test
        void listPagesBySpaceId_otherHttpError_throwsConfluenceOperationException() {
            stubHttpError(HttpStatus.SERVICE_UNAVAILABLE);
            assertThatThrownBy(() -> client.listPagesBySpaceId(TOKEN, CLOUD_ID, "space-42", 25, null))
                    .isInstanceOf(ConfluenceOperationException.class);
        }
    }

    // ------------------------------------------------------------------
    // listSpaces()
    // ------------------------------------------------------------------
    @Nested
    class ListSpacesTests {

        @Test
        void listSpaces_allParamsNull_success() {
            stubSuccess(BODY);
            assertThat(client.listSpaces(TOKEN, CLOUD_ID, null, null, null, 10, null)).isEqualTo(BODY);
        }

        @Test
        void listSpaces_allParamsBlank_success() {
            stubSuccess(BODY);
            assertThat(client.listSpaces(TOKEN, CLOUD_ID, "  ", "  ", "  ", 10, "  ")).isEqualTo(BODY);
        }

        @Test
        void listSpaces_withAllOptionalParams_success() {
            stubSuccess(BODY);
            assertThat(client.listSpaces(TOKEN, CLOUD_ID, "global", "current", "eng", 50, "cursor-abc")).isEqualTo(BODY);
        }

        @Test
        void listSpaces_withTypeOnly_success() {
            stubSuccess(BODY);
            assertThat(client.listSpaces(TOKEN, CLOUD_ID, "personal", null, null, 10, null)).isEqualTo(BODY);
        }

        @Test
        void listSpaces_withStatusOnly_success() {
            stubSuccess(BODY);
            assertThat(client.listSpaces(TOKEN, CLOUD_ID, null, "archived", null, 10, null)).isEqualTo(BODY);
        }

        @Test
        void listSpaces_withQueryOnly_success() {
            stubSuccess(BODY);
            assertThat(client.listSpaces(TOKEN, CLOUD_ID, null, null, "engineering", 10, null)).isEqualTo(BODY);
        }

        @Test
        void listSpaces_withCursorOnly_success() {
            stubSuccess(BODY);
            assertThat(client.listSpaces(TOKEN, CLOUD_ID, null, null, null, 10, "next-cursor")).isEqualTo(BODY);
        }

        @ParameterizedTest
        @ValueSource(ints = {401, 403})
        void listSpaces_authError_throwsConfluenceAuthException(int statusCode) {
            stubHttpError(HttpStatus.valueOf(statusCode));
            assertThatThrownBy(() -> client.listSpaces(TOKEN, CLOUD_ID, null, null, null, 10, null))
                    .isInstanceOf(ConfluenceAuthException.class);
        }

        @Test
        void listSpaces_otherHttpError_throwsConfluenceOperationException() {
            stubHttpError(HttpStatus.TOO_MANY_REQUESTS);
            assertThatThrownBy(() -> client.listSpaces(TOKEN, CLOUD_ID, null, null, null, 10, null))
                    .isInstanceOf(ConfluenceOperationException.class);
        }
    }

    // ------------------------------------------------------------------
    // getSpace() — own try/catch
    // ------------------------------------------------------------------
    @Nested
    class GetSpaceTests {

        @Test
        void getSpace_success_returnsBody() {
            stubSuccess(BODY);
            assertThat(client.getSpace(TOKEN, CLOUD_ID, "space-99")).isEqualTo(BODY);
        }

        @ParameterizedTest
        @ValueSource(ints = {401, 403})
        void getSpace_authError_throwsConfluenceAuthException(int statusCode) {
            stubHttpError(HttpStatus.valueOf(statusCode));
            assertThatThrownBy(() -> client.getSpace(TOKEN, CLOUD_ID, "space-99"))
                    .isInstanceOf(ConfluenceAuthException.class);
        }


        @Test
        void getSpace_otherHttpError_throwsConfluenceOperationException() {
            stubHttpError(HttpStatus.NOT_FOUND);
            assertThatThrownBy(() -> client.getSpace(TOKEN, CLOUD_ID, "space-99"))
                    .isInstanceOf(ConfluenceOperationException.class);
        }
    }

    // ------------------------------------------------------------------
    // getPageChildren() — own try/catch
    // ------------------------------------------------------------------
    @Nested
    class GetPageChildrenTests {

        @Test
        void getPageChildren_success_returnsBody() {
            stubSuccess(BODY);
            assertThat(client.getPageChildren(TOKEN, CLOUD_ID, "page-1", 20, null)).isEqualTo(BODY);
        }

        @ParameterizedTest
        @ValueSource(ints = {401, 403})
        void getPageChildren_authError_throwsConfluenceAuthException(int statusCode) {
            stubHttpError(HttpStatus.valueOf(statusCode));
            assertThatThrownBy(() -> client.getPageChildren(TOKEN, CLOUD_ID, "page-1", 20, null))
                    .isInstanceOf(ConfluenceAuthException.class);
        }

        @Test
        void getPageChildren_otherHttpError_throwsConfluenceOperationException() {
            stubHttpError(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThatThrownBy(() -> client.getPageChildren(TOKEN, CLOUD_ID, "page-1", 20, null))
                    .isInstanceOf(ConfluenceOperationException.class);
        }
    }

    // ------------------------------------------------------------------
    // getAttachments() — own try/catch
    // ------------------------------------------------------------------
    @Nested
    class GetAttachmentsTests {

        @Test
        void getAttachments_success_returnsBody() {
            stubSuccess(BODY);
            assertThat(client.getAttachments(TOKEN, CLOUD_ID, "page-1", 15, null)).isEqualTo(BODY);
        }

        @ParameterizedTest
        @ValueSource(ints = {401, 403})
        void getAttachments_authError_throwsConfluenceAuthException(int statusCode) {
            stubHttpError(HttpStatus.valueOf(statusCode));
            assertThatThrownBy(() -> client.getAttachments(TOKEN, CLOUD_ID, "page-1", 15, null))
                    .isInstanceOf(ConfluenceAuthException.class);
        }

        @Test
        void getAttachments_otherHttpError_throwsConfluenceOperationException() {
            stubHttpError(HttpStatus.CONFLICT);
            assertThatThrownBy(() -> client.getAttachments(TOKEN, CLOUD_ID, "page-1", 15, null))
                    .isInstanceOf(ConfluenceOperationException.class);
        }
    }

    // ------------------------------------------------------------------
    // Authorization header is always set correctly
    // ------------------------------------------------------------------
    @Nested
    class AuthorizationHeaderTests {

        @Test
        void search_setsCorrectBearerHeader() {
            stubSuccess(BODY);
            client.search(TOKEN, CLOUD_ID, "type=page", 5);
            verify(headersSpec).header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        }

        @Test
        void listSpaces_setsCorrectBearerHeader() {
            stubSuccess(BODY);
            client.listSpaces(TOKEN, CLOUD_ID, null, null, null, 5, null);
            verify(headersSpec).header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        }
    }
}