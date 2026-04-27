package com.amaliai.mcp.servers.confluence.exception;

/**
 * Unchecked exception for operational failures in the Confluence MCP server.
 * <p>
 * Wraps specific checked exceptions (Jackson {@code JsonProcessingException}, etc.)
 * so they propagate cleanly through the service layer without polluting method
 * signatures with broad {@code throws Exception} clauses.
 */
public class ConfluenceOperationException extends RuntimeException {

    public ConfluenceOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
