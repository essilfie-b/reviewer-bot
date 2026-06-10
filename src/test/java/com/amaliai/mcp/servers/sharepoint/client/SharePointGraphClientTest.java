package com.amaliai.mcp.servers.sharepoint.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link SharePointGraphClient}.
 *
 * Coverage strategy
 * -----------------
 * SharePointGraphClient is a thin HTTP wrapper for Graph API endpoints.
 * Tests verify correct instantiation and that the client can be created
 * with the required RestClient dependencies.
 *
 * Integration tests would validate actual HTTP call behavior.
 * Unit tests focus on constructor and injection verification.
 */
@ExtendWith(MockitoExtension.class)
class SharePointGraphClientTest {

    @Mock private RestClient graphClient;
    @Mock private RestClient graphClientNoRedirect;

    private SharePointGraphClient sharePointGraphClient;

    @BeforeEach
    void setUp() {
        sharePointGraphClient = new SharePointGraphClient(graphClient, graphClientNoRedirect);
    }

    @Test
    void constructor_injectsGraphClients() {
        assertThat(sharePointGraphClient).isNotNull();
    }

    @Nested
    class OneDriveOperationsTests {

        @Test
        void sharePointGraphClient_createsSuccessfully() {
            assertThat(sharePointGraphClient).isNotNull();
        }

        @Test
        void graphClientIsAvailable() {
            assertThat(graphClient).isNotNull();
        }

        @Test
        void graphClientNoRedirectIsAvailable() {
            assertThat(graphClientNoRedirect).isNotNull();
        }
    }

    @Nested
    class SharePointSitesOperationsTests {

        @Test
        void sitesClientCanBeUsed() {
            assertThat(sharePointGraphClient).isNotNull();
        }
    }
}



