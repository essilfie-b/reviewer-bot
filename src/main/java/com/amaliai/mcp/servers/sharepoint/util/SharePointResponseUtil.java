package com.amaliai.mcp.servers.sharepoint.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Utility methods for building consistent tool response strings.
 * <p>
 * Kept as a Spring-managed component so it can be injected wherever
 * tool-layer response formatting is needed without static coupling.
 */
@Component
public class SharePointResponseUtil {

    /**
     * Returns a JSON error payload in the standard tool error format.
     *
     * @param tool    the tool name (e.g. {@code "getDocumentContent"})
     * @param message a short, human-readable description of the problem
     */
    public String errorResponse(String tool, String message) {
        return String.format("{\"tool\":\"%s\",\"error\":\"%s\"}", tool, message);
    }

    /**
     * Truncates {@code content} to at most {@code maxBytes} UTF-8 bytes,
     * appending {@code "... [TRUNCATED]"} when the limit is exceeded.
     */
    public String trimResponse(String content, int maxBytes) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) return content;
        return new String(bytes, 0, maxBytes, StandardCharsets.UTF_8) + "... [TRUNCATED]";
    }

    /**
     * Returns a JSON success payload in the standard tool response format.
     *
     * @param tool the tool name (e.g. {@code "getDocumentContent"})
     * @param data the already-serialised JSON payload to embed
     */
    public String successResponse(String tool, String data) {
        return String.format("{\"tool\":\"%s\",\"data\":%s}", tool, data);
    }
}