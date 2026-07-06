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
 * Unit tests for {@link SharePointService#listFileVersions}.
 *
 * Covers input validation, delegation to the Graph client, response shaping,
 * the default/max {@code top} bounds, and the parse-failure error contract.
 */
@ExtendWith(MockitoExtension.class)
class SharePointFileVersionsServiceTest {

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
    void listFileVersions_blankItemId_throwsAndDoesNotCallGraph() {
        assertThatThrownBy(() -> service.listFileVersions(TOKEN, "  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itemId");
        verify(graphClient, never()).fetchItemVersions(anyString(), anyString());
    }

    @Test
    void listFileVersions_shapesEachVersion() throws Exception {
        String raw = "{\"value\":[{\"id\":\"3.0\",\"lastModifiedDateTime\":\"2026-01-02T10:00:00Z\","
                + "\"lastModifiedBy\":{\"user\":{\"displayName\":\"Ada\"}},\"size\":1234}]}";
        when(graphClient.fetchItemVersions(TOKEN, ITEM_ID)).thenReturn(raw);

        JsonNode result = MAPPER.readTree(service.listFileVersions(TOKEN, ITEM_ID, null));

        assertThat(result).hasSize(1);
        JsonNode version = result.get(0);
        assertThat(version.get("id").asText()).isEqualTo("3.0");
        assertThat(version.get("lastModifiedDateTime").asText()).isEqualTo("2026-01-02T10:00:00Z");
        assertThat(version.get("lastModifiedBy").asText()).isEqualTo("Ada");
        assertThat(version.get("sizeBytes").asLong()).isEqualTo(1234L);
    }

    @Test
    void listFileVersions_cappedByTop() throws Exception {
        StringBuilder value = new StringBuilder("{\"value\":[");
        for (int i = 0; i < 5; i++) {
            if (i > 0) value.append(',');
            value.append("{\"id\":\"v").append(i).append("\"}");
        }
        value.append("]}");
        when(graphClient.fetchItemVersions(TOKEN, ITEM_ID)).thenReturn(value.toString());

        JsonNode result = MAPPER.readTree(service.listFileVersions(TOKEN, ITEM_ID, 2));

        assertThat(result).hasSize(2);
    }

    @Test
    void listFileVersions_missingModifierHandledSafely() throws Exception {
        when(graphClient.fetchItemVersions(TOKEN, ITEM_ID)).thenReturn("{\"value\":[{\"id\":\"1.0\"}]}");

        JsonNode result = MAPPER.readTree(service.listFileVersions(TOKEN, ITEM_ID, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("lastModifiedBy").isNull()).isTrue();
        assertThat(result.get(0).get("sizeBytes").asLong()).isZero();
    }

    @Test
    void listFileVersions_malformedJson_throwsOperationException() {
        when(graphClient.fetchItemVersions(TOKEN, ITEM_ID)).thenReturn("{not json");

        assertThatThrownBy(() -> service.listFileVersions(TOKEN, ITEM_ID, null))
                .isInstanceOf(SharePointOperationException.class);
    }
}
