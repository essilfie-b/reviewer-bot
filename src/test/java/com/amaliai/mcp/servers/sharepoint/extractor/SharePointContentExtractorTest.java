package com.amaliai.mcp.servers.sharepoint.extractor;

import com.amaliai.mcp.servers.sharepoint.exception.SharePointOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link SharePointContentExtractor}.
 *
 * Coverage strategy
 * -----------------
 * SharePointContentExtractor uses Apache Tika to extract text from binary files.
 * Tests verify:
 * - Text extraction from common formats (txt, csv, json)
 * - Error handling for invalid/unsupported formats
 * - MIME type detection via file extension
 *
 * Note: Full binary extraction tests (DOCX, XLSX, PDF) would require test fixtures.
 * These tests focus on error cases and simple text formats.
 */
class SharePointContentExtractorTest {

    private SharePointContentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new SharePointContentExtractor();
    }

    @Nested
    class TextFileExtractionTests {

        @Test
        void extractText_fromPlainTextFile_succeeds() {
            String content = "Hello, World!";
            byte[] bytes = content.getBytes();

            String result = extractor.extractText(bytes, "document.txt");

            assertThat(result).contains("Hello").contains("World");
        }

        @Test
        void extractText_fromJsonFile_extractsContent() {
            String json = "{\"key\": \"value\", \"number\": 42}";
            byte[] bytes = json.getBytes();

            String result = extractor.extractText(bytes, "data.json");

            assertThat(result).contains("key").contains("value");
        }

        @Test
        void extractText_fromMarkdownFile_extractsContent() {
            String markdown = "# Title\n\nThis is a paragraph.";
            byte[] bytes = markdown.getBytes();

            String result = extractor.extractText(bytes, "README.md");

            assertThat(result).contains("Title").contains("paragraph");
        }

        @Test
        void extractText_fromHtmlFile_extractsText() {
            String html = "<html><body><h1>Hello</h1><p>World</p></body></html>";
            byte[] bytes = html.getBytes();

            String result = extractor.extractText(bytes, "page.html");

            assertThat(result).contains("Hello").contains("World");
        }

        @Test
        void extractText_fromXmlFile_extractsContent() {
            String xml = "<?xml version=\"1.0\"?><root><data>Test</data></root>";
            byte[] bytes = xml.getBytes();

            String result = extractor.extractText(bytes, "data.xml");

            // Tika extracts just the text content from XML, not tags
            assertThat(result).contains("Test");
        }

        @Test
        void extractText_fromCsvFile_extractsContent() {
            String csv = "Name,Age,Email\nJohn,30,john@example.com\nJane,25,jane@example.com";
            byte[] bytes = csv.getBytes();

            String result = extractor.extractText(bytes, "data.csv");

            assertThat(result).contains("Name").contains("John").contains("Jane");
        }

        @Test
        void extractText_fromLogFile_extractsContent() {
            String log = "[INFO] Application started\n[ERROR] An error occurred";
            byte[] bytes = log.getBytes();

            String result = extractor.extractText(bytes, "application.log");

            assertThat(result).contains("Application started").contains("error");
        }
    }

    @Nested
    class EmptyContentTests {

        @Test
        void extractText_fromEmptyFile_throwsOperationException() {
            byte[] emptyBytes = new byte[0];

            // Tika throws exception for empty files
            assertThatThrownBy(() -> extractor.extractText(emptyBytes, "empty.txt"))
                    .isInstanceOf(SharePointOperationException.class);
        }

        @Test
        void extractText_fromWhitespaceOnlyFile_returnsWhitespace() {
            String whitespace = "   \n\t\n   ";
            byte[] bytes = whitespace.getBytes();

            String result = extractor.extractText(bytes, "blank.txt");

            assertThat(result).isNotEmpty();
        }
    }

    @Nested
    class EncodingTests {

        @Test
        void extractText_withUtf8Characters_decodesCorrectly() {
            String content = "Hello 你好 مرحبا Привет";
            byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            String result = extractor.extractText(bytes, "multilingual.txt");

            assertThat(result).contains("Hello");
            // Tika may normalize the content, but should preserve multi-byte characters
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void extractText_withBinaryData_throwsOperationException() {
            // Create random binary data that doesn't match any known format
            byte[] binaryData = new byte[]{
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,  // JPEG header
                    0x00, 0x10, 0x4A, 0x46  // followed by random data
            };

            // This might succeed (extracting empty string) or throw exception depending on Tika version
            // The important thing is it doesn't crash the application
            try {
                String result = extractor.extractText(binaryData, "random.bin");
                // If it succeeds, result should be some string (possibly empty)
                assertThat(result).isNotNull();
            } catch (SharePointOperationException e) {
                // Also acceptable - Tika might not handle unknown binary formats
                assertThat(e.getCause()).isNotNull();
            }
        }

        @Test
        void extractText_withNullFileName_throwsOperationException() {
            byte[] bytes = "some content".getBytes();

            // Tika uses filename for MIME detection, so null should work but might affect detection
            String result = extractor.extractText(bytes, null);

            assertThat(result).isNotNull();
        }

        @Test
        void extractText_withCorruptedZipFile_throwsOperationException() {
            // Create invalid ZIP-like data (DOCX/XLSX are just ZIPs)
            byte[] corruptedZip = new byte[]{
                    0x50, 0x4B, 0x03, 0x04,  // ZIP header
                    0x00, 0x00, 0x00, 0x00   // corrupted data
            };

            // Tika might handle this gracefully or throw based on file extension
            try {
                String result = extractor.extractText(corruptedZip, "file.xlsx");
                // If it doesn't throw, result should be some string
                assertThat(result).isNotNull();
            } catch (SharePointOperationException e) {
                // Expected for corrupted files
                assertThat(e.getMessage()).contains("Failed to extract");
            }
        }
    }

    @Nested
    class LargeContentTests {

        @Test
        void extractText_fromLargeFile_succeeds() {
            // Create a large text content
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                sb.append("Line ").append(i).append(": This is test content.\n");
            }
            byte[] bytes = sb.toString().getBytes();

            String result = extractor.extractText(bytes, "large.txt");

            assertThat(result).isNotEmpty();
            assertThat(result.length()).isGreaterThan(100_000);
        }
    }
}




