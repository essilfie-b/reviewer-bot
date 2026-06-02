package com.amaliai.mcp.servers.sharepoint.util;

import com.amaliai.mcp.common.AbstractIntegrationTokenManager;
import com.amaliai.mcp.servers.sharepoint.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * SharePoint / OneDrive token manager.
 * <p>
 * Delegates all token fetch, catalog resolution, and decryption logic to
 * {@link AbstractIntegrationTokenManager}. Only supplies the SharePoint-specific
 * integration type, encryption key env-var, and exception type.
 */
@Component
public class SharePointTokenManager extends AbstractIntegrationTokenManager {

    public SharePointTokenManager(@Qualifier("backendApiClient") RestClient backendApiClient) {
        super(backendApiClient, "sharepoint", "TOKEN_ENCRYPTION_K");
    }

    @Override
    protected RuntimeException wrapAuthException(String message, Throwable cause) {
        return new AuthenticationException(message, cause);
    }
}
