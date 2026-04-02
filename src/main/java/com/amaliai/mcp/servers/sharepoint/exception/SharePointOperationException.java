package com.amaliai.mcp.servers.sharepoint.exception;

/**
 * Unchecked exception for operational failures in the SharePoint MCP server.
 * <p>
 * Wraps specific checked exceptions (Tika parse errors, Jackson
 * {@code JsonProcessingException}, etc.) so they propagate cleanly through
 * the service layer without polluting method signatures with broad
 * {@code throws Exception} clauses.
 */
public class SharePointOperationException extends RuntimeException {

    public SharePointOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
