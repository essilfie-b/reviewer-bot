package com.amaliai.mcp.servers.confluence.exception;

/**
 * Thrown when a Confluence access token or cloudId cannot be retrieved or decrypted.
 * <p>
 * Separating this from {@link ConfluenceOperationException} lets the global
 * exception handler return a distinct auth-failure response rather than a
 * generic internal-error response.
 */
public class ConfluenceAuthException extends RuntimeException {

    public ConfluenceAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
