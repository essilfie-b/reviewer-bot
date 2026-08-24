package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;

/** {@link DocumentPageFetcher} backed by the Microsoft Graph drive-items endpoint. */
public class GraphDocumentPageFetcher implements DocumentPageFetcher {

    private static final int PAGE_SIZE = 200;

    private final RestClient restClient;

    public GraphDocumentPageFetcher(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DocumentPage fetchPage(String siteId, String libraryId, String pageToken) {
        String url = "/sites/" + siteId + "/drives/" + libraryId + "/root/children?$top=" + PAGE_SIZE;
        if (pageToken != null) {
            url = url + "&$skiptoken=" + pageToken;
        }

        Map<String, Object> body = restClient.get().uri(url).retrieve().body(Map.class);

        List<Map<String, Object>> values = (List<Map<String, Object>>) body.get("value");
        List<DocumentRecord> documents = new ArrayList<>();
        for (Map<String, Object> value : values) {
            documents.add(new DocumentRecord(
                    (String) value.get("id"),
                    (String) value.get("name"),
                    Instant.parse((String) value.get("lastModifiedDateTime")),
                    ((Number) value.get("size")).longValue(),
                    (String) value.get("createdBy")));
        }
        return new DocumentPage(documents, (String) body.get("@odata.nextLink"));
    }
}
