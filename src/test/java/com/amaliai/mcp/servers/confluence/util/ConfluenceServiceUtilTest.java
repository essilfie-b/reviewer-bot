package com.amaliai.mcp.servers.confluence.util;

import com.amaliai.mcp.servers.confluence.dto.SpaceInfo;
import com.amaliai.mcp.servers.confluence.exception.ConfluenceOperationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfluenceServiceUtilTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void buildCql_sanitizesInputAndAppendsSpaceFilter() {
        String cql = ConfluenceServiceUtil.buildCql("hello* \"wor?ld\"", "ENG");

        assertThat(cql).isEqualTo("type=page AND text~\"hello \\\"world\\\"\" AND space.key=\"ENG\"");
    }

    @Test
    void parseSearchResponse_mapsV1SearchResult() throws Exception {
        String raw = """
                {
                  "results": [{
                    "content": {
                      "id": "1",
                      "type": "page",
                      "title": "T1",
                      "space": {"key": "ENG", "name": "Engineering"},
                      "_links": {"webui": "/wiki/spaces/ENG/pages/1"}
                    },
                    "excerpt": "summary",
                    "lastModified": "2026-01-01T00:00:00Z"
                  }],
                  "_links": {"base": "https://acme.atlassian.net"}
                }
                """;

        JsonNode out = MAPPER.readTree(ConfluenceServiceUtil.parseSearchResponse(raw));

        assertThat(out).hasSize(1);
        assertThat(out.get(0).path("url").asText())
                .isEqualTo("https://acme.atlassian.net/wiki/spaces/ENG/pages/1");
        assertThat(out.get(0).path("spaceKey").asText()).isEqualTo("ENG");
    }

    @Test
    void parsePageChildrenResponse_usesFallbackUrlAndExtractsCursor() throws Exception {
        String raw = """
                {
                  "results": [{
                    "id": "22",
                    "title": "Child",
                    "spaceId": "77",
                    "parentId": "21",
                    "version": {"createdAt": "2026-01-02T00:00:00Z"}
                  }],
                  "_links": {
                    "base": "https://acme.atlassian.net",
                    "next": "https://x?page=2&cursor=a%2Bb"
                  }
                }
                """;

        JsonNode out = MAPPER.readTree(ConfluenceServiceUtil.parsePageChildrenResponse(raw));

        assertThat(out.path("nextCursor").asText()).isEqualTo("a+b");
        assertThat(out.path("results").get(0).path("url").asText())
                .isEqualTo("https://acme.atlassian.net/wiki/pages/22");
    }

    @Test
    void parseAttachmentsResponse_mapsLinksAndCursor() throws Exception {
        String raw = """
                {
                  "results": [{
                    "id": "a1",
                    "title": "file.pdf",
                    "mediaType": "application/pdf",
                    "fileSize": 17,
                    "parentId": "p1",
                    "webuiLink": "/wiki/x/abc",
                    "downloadLink": "/download/file.pdf",
                    "version": {"createdAt": "2026-01-03T00:00:00Z"}
                  }],
                  "_links": {
                    "base": "https://acme.atlassian.net",
                    "next": "https://x?cursor=cur-1"
                  }
                }
                """;

        JsonNode out = MAPPER.readTree(ConfluenceServiceUtil.parseAttachmentsResponse(raw));

        assertThat(out.path("nextCursor").asText()).isEqualTo("cur-1");
        JsonNode first = out.path("results").get(0);
        assertThat(first.path("url").asText()).isEqualTo("https://acme.atlassian.net/wiki/x/abc");
        assertThat(first.path("downloadLink").asText()).isEqualTo("https://acme.atlassian.net/download/file.pdf");
    }

    @Test
    void parseSpaceResult_singleResult_usesFallbackKeyWhenKeyMissing() {
        String raw = """
                {"results":[{"id":"55","name":"Engineering"}]}
                """;

        SpaceInfo info = ConfluenceServiceUtil.parseSpaceResult(raw, "ENG");

        assertThat(info.id()).isEqualTo("55");
        assertThat(info.key()).isEqualTo("ENG");
        assertThat(info.name()).isEqualTo("Engineering");
    }

    @Test
    void parseSpaceResult_multipleResults_matchesByNameOrKeyIgnoringCase() {
        String raw = """
                {"results":[
                  {"id":"1","key":"HR","name":"People"},
                  {"id":"2","key":"ENG","name":"Engineering"}
                ]}
                """;

        SpaceInfo info = ConfluenceServiceUtil.parseSpaceResult(raw, "engineering");

        assertThat(info.id()).isEqualTo("2");
        assertThat(info.key()).isEqualTo("ENG");
        assertThat(info.name()).isEqualTo("Engineering");
    }

    @Test
    void parseSpaceResult_whenNotFound_throwsOperationException() {
        String raw = "{" + "\"results\":[]}";

        assertThatThrownBy(() -> ConfluenceServiceUtil.parseSpaceResult(raw, "ENG"))
                .isInstanceOf(ConfluenceOperationException.class)
                .hasMessageContaining("No Confluence space found");
    }

    @Test
    void parseV2PageResponse_buildsNormalizedPageNode() throws Exception {
        String raw = """
                {
                  "id":"101",
                  "title":"Design",
                  "version":{"createdAt":"2026-01-04T00:00:00Z"},
                  "_links":{"base":"https://acme.atlassian.net","webui":"/spaces/ENG/pages/101"}
                }
                """;

        JsonNode out = MAPPER.readTree(ConfluenceServiceUtil.parseV2PageResponse(raw));

        assertThat(out.path("spaceKey").asText()).isEqualTo("ENG");
        assertThat(out.path("type").asText()).isEqualTo("page");
        assertThat(out.path("url").asText()).isEqualTo("https://acme.atlassian.net/spaces/ENG/pages/101");
    }

    @Test
    void parseV2PageContentResponse_stripsHtmlAndSetsTruncatedFlag() throws Exception {
        String raw = """
                {
                  "id":"9",
                  "title":"Guide",
                  "version":{"createdAt":"2026-01-05T00:00:00Z"},
                  "body":{"view":{"value":"<p>Hello &amp; <b>world</b></p>"}},
                  "_links":{"base":"https://acme.atlassian.net","webui":"/spaces/ENG/pages/9"}
                }
                """;

        JsonNode out = MAPPER.readTree(ConfluenceServiceUtil.parseV2PageContentResponse(raw));

        assertThat(out.path("content").asText()).isEqualTo("Hello & world");
        assertThat(out.path("truncated").asBoolean()).isFalse();
    }

    @Test
    void parsePagesListResponse_wrapsResultsAndNextCursor() throws Exception {
        String raw = """
                {
                  "results": [{
                    "id":"8",
                    "title":"Roadmap",
                    "version":{"createdAt":"2026-01-06T00:00:00Z"},
                    "_links":{"webui":"/spaces/ENG/pages/8"}
                  }],
                  "_links": {
                    "base":"https://acme.atlassian.net",
                    "next":"https://api?cursor=next-44"
                  }
                }
                """;

        JsonNode out = MAPPER.readTree(ConfluenceServiceUtil.parsePagesListResponse(raw, "ENG", "Engineering"));

        assertThat(out.path("nextCursor").asText()).isEqualTo("next-44");
        assertThat(out.path("results")).hasSize(1);
        assertThat(out.path("results").get(0).path("spaceName").asText()).isEqualTo("Engineering");
    }

    @Test
    void stripHtml_removesTagsAndDecodesCommonEntities() {
        String text = ConfluenceServiceUtil.stripHtml("<div>A &amp; B&nbsp;&lt;ok&gt;</div>");

        assertThat(text).isEqualTo("A & B <ok>");
    }

    @Test
    void parseSpaceResponse_mapsDescriptionAndUrl() throws Exception {
        String raw = """
                {
                  "id":"s1",
                  "key":"ENG",
                  "name":"Engineering",
                  "type":"global",
                  "status":"current",
                  "authorId":"u1",
                  "createdAt":"2026-01-01",
                  "homepageId":"p1",
                  "description":{"plain":{"value":"docs"}},
                  "_links":{"base":"https://acme.atlassian.net","webui":"/spaces/ENG"}
                }
                """;

        JsonNode out = MAPPER.readTree(ConfluenceServiceUtil.parseSpaceResponse(raw));

        assertThat(out.path("description").asText()).isEqualTo("docs");
        assertThat(out.path("url").asText()).isEqualTo("https://acme.atlassian.net/spaces/ENG");
    }

    @Test
    void parseSpacesListResponse_mapsResultsAndCursor() throws Exception {
        String raw = """
                {
                  "results": [{
                    "id":"s1",
                    "key":"ENG",
                    "name":"Engineering",
                    "type":"global",
                    "status":"current",
                    "authorId":"u1",
                    "createdAt":"2026-01-01",
                    "homepageId":"p1",
                    "description":{"plain":{"value":"docs"}},
                    "_links":{"webui":"/spaces/ENG"}
                  }],
                  "_links": {
                    "base":"https://acme.atlassian.net",
                    "next":"https://api?cursor=xyz"
                  }
                }
                """;

        JsonNode out = MAPPER.readTree(ConfluenceServiceUtil.parseSpacesListResponse(raw));

        assertThat(out.path("results")).hasSize(1);
        assertThat(out.path("results").get(0).path("url").asText()).isEqualTo("https://acme.atlassian.net/spaces/ENG");
        assertThat(out.path("nextCursor").asText()).isEqualTo("xyz");
    }

    @Test
    void extractPageIds_returnsOnlyNonNullIds() {
        List<String> ids = ConfluenceServiceUtil.extractPageIds("""
                {"results":[{"id":"1"},{"title":"missing-id"},{"id":"2"}]}
                """);

        assertThat(ids).containsExactly("1", "2");
    }

    @Test
    void buildPagesWithAttachmentsResponse_includesOnlyPagesWithAttachments() throws Exception {
        String pagesRaw = """
                {
                  "results": [
                    {"id":"p1","title":"A","version":{"createdAt":"t"},"_links":{"webui":"/spaces/ENG/pages/p1"}},
                    {"id":"p2","title":"B","version":{"createdAt":"t"},"_links":{"webui":"/spaces/ENG/pages/p2"}}
                  ],
                  "_links": {"base":"https://acme.atlassian.net"}
                }
                """;

        String attachmentsRaw = """
                {
                  "results": [{
                    "id":"a1",
                    "title":"file.txt",
                    "mediaType":"text/plain",
                    "fileSize":1,
                    "webuiLink":"/wiki/x/a1",
                    "downloadLink":"/download/a1",
                    "version":{"createdAt":"t"}
                  }],
                  "_links": {"base":"https://acme.atlassian.net"}
                }
                """;

        String outRaw = ConfluenceServiceUtil.buildPagesWithAttachmentsResponse(
                pagesRaw,
                "ENG",
                "Engineering",
                Map.of("p1", attachmentsRaw)
        );

        JsonNode out = MAPPER.readTree(outRaw);
        assertThat(out.path("results")).hasSize(1);
        JsonNode first = out.path("results").get(0);
        assertThat(first.path("id").asText()).isEqualTo("p1");
        assertThat(first.path("attachments")).hasSize(1);
    }

    @Test
    void extractCursor_handlesMissingAndPresentCursor() {
        assertThat(ConfluenceServiceUtil.extractCursor(null)).isNull();
        assertThat(ConfluenceServiceUtil.extractCursor("https://api?x=1")).isNull();
        assertThat(ConfluenceServiceUtil.extractCursor("https://api?x=1&cursor=a%2Fb")).isEqualTo("a/b");
    }
}

