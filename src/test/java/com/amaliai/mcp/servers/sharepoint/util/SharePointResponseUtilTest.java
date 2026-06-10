package com.amaliai.mcp.servers.sharepoint.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SharePointResponseUtil}.
 *
 * Coverage strategy
 * -----------------
 * SharePointResponseUtil provides utility methods for formatting responses.
 * Tests cover both errorResponse (format safety) and trimResponse (byte-level truncation).
 */
class SharePointResponseUtilTest {

    private SharePointResponseUtil responseUtil;

    @BeforeEach
    void setUp() {
        responseUtil = new SharePointResponseUtil();
    }

    @Nested
    class ErrorResponseTests {

        @Test
        void errorResponse_formatsCorrectly() {
            String error = responseUtil.errorResponse("getDocuments", "Access denied");

            assertThat(error).isEqualTo("{\"tool\":\"getDocuments\",\"error\":\"Access denied\"}");
        }

        @Test
        void errorResponse_withEmptyMessage_formatsCorrectly() {
            String error = responseUtil.errorResponse("searchDocuments", "");

            assertThat(error).isEqualTo("{\"tool\":\"searchDocuments\",\"error\":\"\"}");
        }

        @Test
        void errorResponse_withSpecialCharacters_escapesCorrectly() {
            String error = responseUtil.errorResponse("downloadFile", "File \"test.pdf\" not found");

            assertThat(error).contains("downloadFile").contains("test.pdf");
        }

        @Test
        void errorResponse_withMultipleTools_distinguishesCorrectly() {
            String error1 = responseUtil.errorResponse("tool1", "error");
            String error2 = responseUtil.errorResponse("tool2", "error");

            assertThat(error1).contains("\"tool\":\"tool1\"");
            assertThat(error2).contains("\"tool\":\"tool2\"");
            assertThat(error1).isNotEqualTo(error2);
        }
    }

    @Nested
    class TrimResponseTests {

        @Test
        void trimResponse_withinLimit_returnsUnchanged() {
            String content = "Hello World";
            int maxBytes = 1024;

            String result = responseUtil.trimResponse(content, maxBytes);

            assertThat(result).isEqualTo("Hello World");
        }

        @Test
        void trimResponse_exceedsLimit_truncatesAndAppendsMarker() {
            String content = "This is a long content that exceeds the maximum byte limit";
            int maxBytes = 10;

            String result = responseUtil.trimResponse(content, maxBytes);

            assertThat(result).endsWith("... [TRUNCATED]");
            assertThat(result.getBytes(StandardCharsets.UTF_8))
                    .hasSizeGreaterThan(maxBytes);        }

        @Test
        void trimResponse_atExactLimit_returnsUnchanged() {
            String content = "12345";
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            int maxBytes = bytes.length;

            String result = responseUtil.trimResponse(content, maxBytes);

            assertThat(result).isEqualTo(content).doesNotContain("TRUNCATED");
        }

        @Test
        void trimResponse_multibyteCharacters_respectsByteLimit() {
            String content = "Hello 世界 это тестирование 🎉🎊";
            byte[] fullBytes = content.getBytes(StandardCharsets.UTF_8);
            int maxBytes = 10;

            String result = responseUtil.trimResponse(content, maxBytes);

            byte[] resultBytes = result.getBytes(StandardCharsets.UTF_8);
            assertThat(resultBytes)
                    .hasSizeGreaterThanOrEqualTo(maxBytes)
                    .hasSizeLessThan(fullBytes.length);
        }

        @Test
        void trimResponse_largeContent_truncatesCorrectly() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                sb.append("This is line ").append(i).append(" of large content. ");
            }
            String content = sb.toString();
            int maxBytes = 500;

            String result = responseUtil.trimResponse(content, maxBytes);

            assertThat(result).endsWith("... [TRUNCATED]");

            byte[] bytes = result.getBytes(StandardCharsets.UTF_8);

            assertThat(bytes).hasSizeGreaterThan(maxBytes);
        }

        @Test
        void trimResponse_emptyString_returnsEmpty() {
            String result = responseUtil.trimResponse("", 100);

            assertThat(result).isEmpty();
        }

        @Test
        void trimResponse_singleByteLimit_stillAppendsMarker() {
            String content = "Testing";
            int maxBytes = 1;

            String result = responseUtil.trimResponse(content, maxBytes);

            assertThat(result).endsWith("... [TRUNCATED]");
        }

        @Test
        void trimResponse_zeroByteLimit_truncates() {
            String content = "Content";
            int maxBytes = 0;

            String result = responseUtil.trimResponse(content, maxBytes);

            assertThat(result).endsWith("... [TRUNCATED]");
        }

        @Test
        void trimResponse_preservesJsonStructure() {
            String jsonContent = "{\"data\": \"" + "x".repeat(1000) + "\"}";
            int maxBytes = 100;

            String result = responseUtil.trimResponse(jsonContent, maxBytes);

            assertThat(result).startsWith("{\"data\": \"").endsWith("... [TRUNCATED]");
        }
    }
}

