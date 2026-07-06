package com.amaliai.mcp.servers.sharepoint.service;

import com.amaliai.mcp.servers.sharepoint.client.DriveItemParser;
import com.amaliai.mcp.servers.sharepoint.client.SharePointGraphClient;
import com.amaliai.mcp.servers.sharepoint.exception.SharePointOperationException;
import com.amaliai.mcp.servers.sharepoint.extractor.SharePointContentExtractor;
import com.amaliai.mcp.servers.sharepoint.util.SharePointResponseUtil;
import com.amaliai.mcp.servers.sharepoint.validator.SharePointValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SharePointService#listFilePermissions}.
 *
 * Covers input validation, delegation to the Graph client, response shaping,
 * the default/max {@code top} bounds, and the parse-failure error contract.
 */
@ExtendWith(MockitoExtension.class)
class SharePointFilePermissionsServiceTest {

    private static final String TOKEN = "test-token";
    private static final String ITEM_ID = "01ITEM";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock private SharePointGraphClient graphClient;
    @Mock private DriveItemParser driveItemParser;
    @Mock private SharePointContentExtractor contentExtractor;
    @Mock private SharePointValidator validator;
    @Mock private SharePointResponseUtil responseUtil;

    private SharePointService service;

    @BeforeEach
    void setUp() {
        service = new SharePointService(graphClient, driveItemParser, contentExtractor, validator, responseUtil);
        Mockito.lenient().when(responseUtil.trimResponse(anyString(), anyInt()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void listFilePermissions_blankItemId_throwsAndDoesNotCallGraph() {
        assertThatThrownBy(() -> service.listFilePermissions(TOKEN, "  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itemId");
        verify(graphClient, never()).fetchItemPermissions(anyString(), anyString());
    }

    @Test
    void listFilePermissions_shapesEachPermission() throws Exception {
        String raw = "{\"value\":[{\"id\":\"perm-1\",\"roles\":[\"read\"],"
                + "\"grantedTo\":{\"user\":{\"displayName\":\"Ada\"}},"
                + "\"link\":{\"webUrl\":\"https://share/abc\"}}]}";
        when(graphClient.fetchItemPermissions(TOKEN, ITEM_ID)).thenReturn(raw);

        JsonNode result = MAPPER.readTree(service.listFilePermissions(TOKEN, ITEM_ID, null));

        assertThat(result).hasSize(1);
        JsonNode permission = result.get(0);
        assertThat(permission.get("id").asText()).isEqualTo("perm-1");
        assertThat(permission.get("roles").isArray()).isTrue();
        assertThat(permission.get("roles").get(0).asText()).isEqualTo("read");
        assertThat(permission.get("grantedTo").asText()).isEqualTo("Ada");
        assertThat(permission.get("link").asText()).isEqualTo("https://share/abc");
    }

    @Test
    void listFilePermissions_cappedByTop() throws Exception {
        StringBuilder value = new StringBuilder("{\"value\":[");
        for (int i = 0; i < 5; i++) {
            if (i > 0) value.append(',');
            value.append("{\"id\":\"p").append(i).append("\"}");
        }
        value.append("]}");
        when(graphClient.fetchItemPermissions(TOKEN, ITEM_ID)).thenReturn(value.toString());

        JsonNode result = MAPPER.readTree(service.listFilePermissions(TOKEN, ITEM_ID, 2));

        assertThat(result).hasSize(2);
    }

    @Test
    void listFilePermissions_missingRoles_yieldsEmptyArray() throws Exception {
        when(graphClient.fetchItemPermissions(TOKEN, ITEM_ID)).thenReturn("{\"value\":[{\"id\":\"perm-1\"}]}");

        JsonNode result = MAPPER.readTree(service.listFilePermissions(TOKEN, ITEM_ID, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("roles").isArray()).isTrue();
        assertThat(result.get(0).get("roles")).isEmpty();
    }

    @Test
    void listFilePermissions_malformedJson_throwsOperationException() {
        when(graphClient.fetchItemPermissions(TOKEN, ITEM_ID)).thenReturn("{not json");

        assertThatThrownBy(() -> service.listFilePermissions(TOKEN, ITEM_ID, null))
                .isInstanceOf(SharePointOperationException.class);
    }
}
