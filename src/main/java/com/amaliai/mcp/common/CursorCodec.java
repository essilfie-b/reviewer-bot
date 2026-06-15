package com.amaliai.mcp.common;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encodes and decodes opaque pagination cursors exchanged with MCP clients.
 * <p>
 * Internally a cursor is a {@code "spaceId:offset"} pair. We Base64-encode it so
 * the client treats it as an opaque token and so it can be safely embedded in
 * the {@code nextCursor} field of a tool response and later passed back as a
 * query parameter.
 */
@Component
public class CursorCodec {

    /** Encodes a space ID and offset into an opaque cursor token. */
    public String encode(String spaceId, int offset) {
        String raw = spaceId + ":" + offset;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes an opaque cursor token back into its offset.
     *
     * @return the offset encoded in the cursor, or {@code 0} for a null/blank cursor (first page)
     */
    public int decodeOffset(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        String decoded = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
        String offsetPart = decoded.split(":")[1];
        return Integer.parseInt(offsetPart);
    }
}
