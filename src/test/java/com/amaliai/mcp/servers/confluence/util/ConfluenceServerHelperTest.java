package com.amaliai.mcp.servers.confluence.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ConfluenceServerHelperTest {

    @Test
    void resolveCredentials_resolvesIntegrationThenBuildsCredentials() {
        ConfluenceTokenManager tokenManager = mock(ConfluenceTokenManager.class);
        UUID integrationId = UUID.randomUUID();

        when(tokenManager.resolveIntegrationId()).thenReturn(integrationId);
        when(tokenManager.getAccessToken(42, integrationId)).thenReturn("token-1");
        when(tokenManager.getCloudId(42, integrationId)).thenReturn("cloud-1");

        ConfluenceServerHelper.Credentials credentials = ConfluenceServerHelper.resolveCredentials(42, tokenManager);

        assertThat(credentials.token()).isEqualTo("token-1");
        assertThat(credentials.cloudId()).isEqualTo("cloud-1");
        verify(tokenManager).resolveIntegrationId();
        verify(tokenManager).getAccessToken(42, integrationId);
        verify(tokenManager).getCloudId(42, integrationId);
    }
}

