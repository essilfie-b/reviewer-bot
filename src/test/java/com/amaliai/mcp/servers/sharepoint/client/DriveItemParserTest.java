package com.amaliai.mcp.servers.sharepoint.client;

import com.amaliai.mcp.servers.sharepoint.exception.SharePointOperationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link DriveItemParser}.
 *
 * Coverage strategy
 * -----------------
 * DriveItemParser transforms Graph API responses into tool-friendly JSON.
 * Tests cover filtering (fileType, author), folder inclusion, and error handling.
 */
class DriveItemParserTest {

    private DriveItemParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        parser = new DriveItemParser();
        objectMapper = new ObjectMapper();
    }

    private String buildResponseBody(String... itemJsons) {
        StringBuilder sb = new StringBuilder("{\"value\":[");
        for (int i = 0; i < itemJsons.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(itemJsons[i]);
        }
        sb.append("]}");
        return sb.toString();
    }

    private int countJsonArrayElements(String json) throws Exception {
        return objectMapper.readValue(json, JsonNode[].class).length;
    }

    private String fileItem(String id, String name, String createdBy) {
        return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"size\":1024," +
                        "\"webUrl\":\"http://example.com/%s\"," +
                        "\"lastModifiedDateTime\":\"2025-01-01T12:00:00Z\"," +
                        "\"createdBy\":{\"user\":{\"displayName\":\"%s\"}}," +
                        "\"lastModifiedBy\":{\"user\":{\"displayName\":\"Last Modifier\"}}," +
                        "\"file\":{\"mimeType\":\"text/plain\"}}",
                id, name, id, createdBy);
    }

    private String folderItem(String id, String name) {
        return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\"," +
                        "\"webUrl\":\"http://example.com/%s\"," +
                        "\"lastModifiedDateTime\":\"2025-01-01T12:00:00Z\"," +
                        "\"createdBy\":{\"user\":{\"displayName\":\"Creator\"}}," +
                        "\"folder\":{},\"lastModifiedBy\":{\"user\":{\"displayName\":\"Last Modifier\"}}}",
                id, name, id);
    }

    @Nested
    class ParseBasicTests {

        @Test
        void parse_withSingleFile_returnsCorrectly() throws Exception {
            String response = buildResponseBody(fileItem("file-1", "document.txt", "John Doe"));

            String result = parser.parse(response, null, null);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).hasSize(1);
            assertThat(items[0].path("id").asText()).isEqualTo("file-1");
            assertThat(items[0].path("name").asText()).isEqualTo("document.txt");
            assertThat(items[0].path("itemType").asText()).isEqualTo("file");
            assertThat(items[0].path("fileType").asText()).isEqualTo("txt");
        }

        @Test
        void parse_withMultipleFiles_returnsAll() throws Exception {
            String response = buildResponseBody(
                    fileItem("file-1", "doc.docx", "Author1"),
                    fileItem("file-2", "sheet.xlsx", "Author2")
            );

            String result = parser.parse(response, null, null);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).hasSize(2);
        }

        @Test
        void parse_excludesFolders_byDefault() throws Exception {
            String response = buildResponseBody(
                    fileItem("file-1", "document.txt", "John"),
                    folderItem("folder-1", "MyFolder")
            );

            String result = parser.parse(response, null, null, false);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).hasSize(1);
            assertThat(items[0].path("name").asText()).isEqualTo("document.txt");
        }

        @Test
        void parse_includesFolders_whenRequested() throws Exception {
            String response = buildResponseBody(
                    fileItem("file-1", "document.txt", "John"),
                    folderItem("folder-1", "MyFolder")
            );

            String result = parser.parse(response, null, null, true);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).hasSize(2);
            assertThat(items).anyMatch(item -> "folder".equals(item.path("itemType").asText()));
        }
    }

    @Nested
    class FileTypeFilterTests {

        @Test
        void parse_withFileTypeFilter_keepsOnlyMatching() throws Exception {
            String response = buildResponseBody(
                    fileItem("file-1", "document.docx", "John"),
                    fileItem("file-2", "sheet.xlsx", "Jane"),
                    fileItem("file-3", "report.pdf", "Bob")
            );

            String result = parser.parse(response, "docx", null);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).hasSize(1);
            assertThat(items[0].path("name").asText()).isEqualTo("document.docx");
        }

        @Test
        void parse_withFileTypeFilter_caseInsensitive() throws Exception {
            String response = buildResponseBody(fileItem("file-1", "document.DOCX", "John"));

            String result = parser.parse(response, "docx", null);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).hasSize(1);
        }

        @Test
        void parse_withFileTypeFilter_excludesNonMatching() throws Exception {
            String response = buildResponseBody(
                    fileItem("file-1", "document.txt", "John"),
                    fileItem("file-2", "sheet.xlsx", "Jane")
            );

            String result = parser.parse(response, "pdf", null);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).isEmpty();
        }

        @Test
        void parse_fileTypeFilter_ignoresFolders() throws Exception {
            String response = buildResponseBody(folderItem("folder-1", "MyFolder"));

            String result = parser.parse(response, "docx", null, true);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).hasSize(1);
            assertThat(items[0].path("itemType").asText()).isEqualTo("folder");
        }
    }

    @Nested
    class AuthorFilterTests {

        @Test
        void parse_withAuthorFilter_keepsOnlyMatching() throws Exception {
            String response = buildResponseBody(
                    fileItem("file-1", "doc1.txt", "John Doe"),
                    fileItem("file-2", "doc2.txt", "Jane Smith"),
                    fileItem("file-3", "doc3.txt", "John Smith")
            );

            String result = parser.parse(response, null, "John");

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).hasSize(2);
        }

        @Test
        void parse_withAuthorFilter_caseInsensitive() throws Exception {
            String response = buildResponseBody(fileItem("file-1", "doc.txt", "John Doe"));

            String result = parser.parse(response, null, "john");

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).hasSize(1);
        }

        @Test
        void parse_withAuthorFilter_partialMatch() throws Exception {
            String response = buildResponseBody(
                    fileItem("file-1", "doc1.txt", "John Doe"),
                    fileItem("file-2", "doc2.txt", "Jane Smith")
            );

            String result = parser.parse(response, null, "Doe");

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).hasSize(1);
        }

        @Test
        void parse_withAuthorFilter_noMatching() throws Exception {
            String response = buildResponseBody(fileItem("file-1", "doc.txt", "John Doe"));

            String result = parser.parse(response, null, "NonExistent");

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).isEmpty();
        }
    }

    @Nested
    class CombinedFiltersTests {

        @Test
        void parse_withBothFilters_appliesBoth() throws Exception {
            String response = buildResponseBody(
                    fileItem("file-1", "doc1.docx", "John Doe"),
                    fileItem("file-2", "doc2.xlsx", "John Smith"),
                    fileItem("file-3", "doc3.docx", "Jane Doe")
            );

            String result = parser.parse(response, "docx", "John");

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).hasSize(1);
            assertThat(items[0].path("name").asText()).isEqualTo("doc1.docx");
        }
    }

    @Nested
    class ExtractedFieldsTests {

        @Test
        void parse_includesAllRequiredFields() throws Exception {
            String response = buildResponseBody(fileItem("file-1", "document.txt", "John Doe"));

            String result = parser.parse(response, null, null);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items[0].path("id").asText()).isNotEmpty();
            assertThat(items[0].path("name").asText()).isEqualTo("document.txt");
            assertThat(items[0].path("itemType").asText()).isEqualTo("file");
            assertThat(items[0].path("webUrl").asText()).isNotEmpty();
            assertThat(items[0].path("sizeBytes").asLong()).isEqualTo(1024);
            assertThat(items[0].path("lastModified").asText()).isNotEmpty();
            assertThat(items[0].path("createdBy").asText()).isEqualTo("John Doe");
        }

        @Test
        void parse_extractsFileType() throws Exception {
            String response = buildResponseBody(fileItem("file-1", "report.pdf", "John"));

            String result = parser.parse(response, null, null);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items[0].path("fileType").asText()).isEqualTo("pdf");
        }

        @Test
        void parse_handlesMissingExtension() throws Exception {
            String body = "{\"value\":[{\"id\":\"1\",\"name\":\"README\"," +
                    "\"webUrl\":\"http://ex/1\",\"size\":512," +
                    "\"lastModifiedDateTime\":\"2025-01-01T00:00:00Z\"," +
                    "\"createdBy\":{\"user\":{\"displayName\":\"Author\"}}," +
                    "\"lastModifiedBy\":{\"user\":{\"displayName\":\"Mod\"}}}]}";

            String result = parser.parse(body, null, null);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items[0].path("fileType").isMissingNode()).isTrue();
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void parse_withInvalidJson_throwsSharePointOperationException() {
            String invalidJson = "not valid json";

            assertThatThrownBy(() -> parser.parse(invalidJson, null, null))
                    .isInstanceOf(SharePointOperationException.class)
                    .hasMessageContaining("Failed to parse");
        }

        @Test
        void parse_withMalformedResponse_returnsEmptyArray() throws Exception {
            String malformed = "{\"results\":[]}";

            String result = parser.parse(malformed, null, null);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).isEmpty();
        }

        @Test
        void parse_withEmptyValue_returnsEmptyArray() throws Exception {
            String response = "{\"value\":[]}";

            String result = parser.parse(response, null, null);

            JsonNode[] items = objectMapper.readValue(result, JsonNode[].class);
            assertThat(items).isEmpty();
        }
    }
}





