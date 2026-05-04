package com.amaliai.mcp.servers.confluence.util;

import com.amaliai.mcp.common.AbstractIntegrationTokenManager;
import com.amaliai.mcp.servers.confluence.exception.ConfluenceAuthException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Confluence token manager.
 * <p>
 * Delegates all token fetch, catalog resolution, and decryption logic to
 * {@link AbstractIntegrationTokenManager}. Adds {@link #getCloudId} for the
 * Atlassian-specific provider metadata lookup required by every Confluence API call.
 */
@Component
public class ConfluenceTokenManager extends AbstractIntegrationTokenManager {

    public ConfluenceTokenManager(@Qualifier("backendApiClient") RestClient backendApiClient) {
        super(backendApiClient, "confluence", "TOKEN_ENCRYPTION_K");
    }

    @Override
    protected RuntimeException wrapAuthException(String message, Throwable cause) {
        return new ConfluenceAuthException(message, cause);
    }

    /**
     * Fetches the Atlassian cloud ID for the given user from the backend's
     * provider-metadata endpoint.
     *
     * @throws ConfluenceAuthException if the cloudId cannot be fetched or is blank
     */
    public String getCloudId(int armsUserId, UUID integrationId) {
        log.info("Fetching Confluence cloudId — armsUserId={}", armsUserId);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = backendApiClient.get()
                    .uri("/integrations/provider-metadata/{userId}/{integrationId}",
                            armsUserId, integrationId)
                    .retrieve()
                    .body(Map.class);

            if (metadata == null) {
                throw new IllegalStateException("Backend returned null provider metadata");
            }
            String cloudId = (String) metadata.get("cloudId");
            if (cloudId == null || cloudId.isBlank()) {
                throw new IllegalStateException("Provider metadata missing 'cloudId' field");
            }
            return cloudId;
        } catch (ConfluenceAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfluenceAuthException(
                    "Failed to retrieve Confluence cloudId for user " + armsUserId, e);
        }
    }
}
