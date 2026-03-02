package com.amaliai.mcp.servers.sharepoint.exception;

/**
 * Thrown when a SharePoint / OneDrive access token cannot be retrieved or decrypted.
 * <p>
 * Separating this from {@link SharePointOperationException} lets the global
 * exception handler return a distinct auth-failure response rather than a
 * generic internal-error response.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
