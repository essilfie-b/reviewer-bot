package com.amaliai.mcp.servers.sharepoint.extractor;

import com.amaliai.mcp.servers.sharepoint.exception.SharePointOperationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Extracts plain text from binary file content using Apache Tika.
 * <p>
 * The character limit on {@link BodyContentHandler} is intentionally disabled ({@code -1});
 * callers are responsible for truncating the result to their own size caps.
 * <p>
 * {@link AutoDetectParser} is thread-safe and kept as a static singleton to avoid
 * the overhead of re-initialising the parser registry on every call.
 */
@Slf4j
@Component
public class SharePointContentExtractor {

    private static final AutoDetectParser TIKA_PARSER = new AutoDetectParser();

    /**
     * Parses {@code bytes} and returns all extracted text.
     *
     * @param bytes    raw file bytes
     * @param fileName used by Tika for MIME-type detection via the file extension
     * @throws SharePointOperationException wrapping the underlying {@link TikaException},
     *                                      {@link SAXException}, or {@link IOException}
     */
    public String extractText(byte[] bytes, String fileName) {
        try {
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);

            BodyContentHandler handler = new BodyContentHandler(-1);
            TIKA_PARSER.parse(new ByteArrayInputStream(bytes), handler, metadata, new ParseContext());

            log.debug("Tika detected MIME type '{}' for '{}'", metadata.get("Content-Type"), fileName);
            return handler.toString();
        } catch (TikaException | SAXException | IOException e) {
            throw new SharePointOperationException(
                    "Failed to extract text from '" + fileName + "'", e);
        }
    }
}
