package com.amaliai.mcp.servers.confluence.util;

import java.util.UUID;

// This class contains helper methods moved from ConfluenceServer.
public class ConfluenceServerHelper {
    public static Credentials resolveCredentials(int armsUserId, ConfluenceTokenManager tokenManager) {
        UUID integrationId = tokenManager.resolveIntegrationId();
        return new Credentials(
                tokenManager.getAccessToken(armsUserId, integrationId),
                tokenManager.getCloudId(armsUserId, integrationId));
    }

    public record Credentials(String token, String cloudId) {}


}
