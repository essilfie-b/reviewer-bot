package com.amaliai.mcp.servers.sharepoint.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.amaliai.mcp.servers.sharepoint.SharePointConstants.*;

/**
 * Thin HTTP client wrapper for the Microsoft Graph API (OneDrive scope).
 * <p>
 * Every method maps to exactly one Graph API endpoint.  Business logic,
 * response parsing, and error handling are the responsibility of callers.
 * <p>
 * The underlying {@link RestClient} bean ({@code graphClient}) is configured
 * with {@code HttpClient.Redirect.NORMAL} so that the 302 redirect issued by
 * the {@code /content} endpoint is automatically followed to the CDN.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SharePointGraphClient {

    private static final String SELECT_ITEM_FIELDS =
            "id,name,webUrl,createdBy,lastModifiedBy,lastModifiedDateTime,size,file,folder";

    private final RestClient graphClient;

    /**
     * Lists files and folders at the root of the user's OneDrive,
     * ordered by most-recently-modified first.
     */
    public String fetchRootChildren(String token) {
        log.debug("Graph: GET /me/drive/root/children");
        return graphClient.get()
                .uri(b -> b.path("/me/drive/root/children")
                        .queryParam("$select", SELECT_ITEM_FIELDS)
                        .queryParam("$top", MAX_TOP)
                        .queryParam("$orderby", "lastModifiedDateTime desc")
                        .build())
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                .retrieve()
                .body(String.class);
    }

    /**
     * Searches the user's OneDrive using an OData search expression.
     *
     * @param safeQuery   search query with OData single-quotes already escaped
     * @param top         maximum result count
     * @param dateFilters zero or more OData {@code lastModifiedDateTime} filter clauses
     */
    public String searchItems(String token, String safeQuery, int top, List<String> dateFilters) {
        log.debug("Graph: GET /me/drive/root/search(q='{}') top={}", safeQuery, top);
        String searchPath = "/me/drive/root/search(q='" + safeQuery + "')";
        return graphClient.get()
                .uri(b -> {
                    var builder = b.path(searchPath)
                            .queryParam("$select", SELECT_ITEM_FIELDS)
                            .queryParam("$top", top)
                            .queryParam("$orderby", "lastModifiedDateTime desc");
                    if (!dateFilters.isEmpty()) {
                        builder.queryParam("$filter", String.join(" and ", dateFilters));
                    }
                    return builder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                .retrieve()
                .body(String.class);
    }

    /**
     * Fetches metadata for a single drive item (name, size, file facet).
     */
    public String fetchItemMetadata(String token, String itemId) {
        log.debug("Graph: GET /me/drive/items/{}/metadata", itemId);
        return graphClient.get()
                .uri(b -> b.path("/me/drive/items/{id}")
                        .queryParam("$select", "name,size,file")
                        .build(itemId))
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                .retrieve()
                .body(String.class);
    }

    /**
     * Downloads raw file bytes.
     * <p>
     * Graph API responds with HTTP 302 → CDN; the configured {@link RestClient}
     * follows the redirect automatically, and Java's {@code HttpClient} strips the
     * {@code Authorization} header on the cross-host redirect (correct behaviour —
     * the CDN pre-signed URL does not require auth).
     */
    public byte[] downloadItemContent(String token, String itemId) {
        log.debug("Graph: GET /me/drive/items/{}/content", itemId);
        return graphClient.get()
                .uri(b -> b.path("/me/drive/items/{id}/content").build(itemId))
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                .retrieve()
                .body(byte[].class);
    }

    // -------------------------------------------------------------------------
    // SharePoint Sites API
    // -------------------------------------------------------------------------

    /**
     * Lists SharePoint sites the authenticated user has access to.
     * Uses the /sites?search=* endpoint to retrieve all accessible sites.
     */
    public String fetchUserSites(String token, int top) {
        log.debug("Graph: GET /sites?search=* top={}", top);
        return graphClient.get()
                .uri(b -> b.path("/sites")
                        .queryParam("search", "*")
                        .queryParam("$select", "id,name,displayName,webUrl,description,createdDateTime,lastModifiedDateTime")
                        .queryParam("$top", top)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                .retrieve()
                .body(String.class);
    }

    /**
     * Fetches details for a specific SharePoint site by its ID.
     *
     * @param siteId the SharePoint site ID (format: {hostname},{site-collection-id},{web-id})
     */
    public String fetchSiteDetails(String token, String siteId) {
        log.debug("Graph: GET /sites/{}", siteId);
        return graphClient.get()
                .uri(b -> b.path("/sites/{siteId}")
                        .queryParam("$select", "id,name,displayName,webUrl,description,createdDateTime,lastModifiedDateTime,root")
                        .build(siteId))
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                .retrieve()
                .body(String.class);
    }

    /**
     * Lists document libraries (drives) in a SharePoint site.
     *
     * @param siteId the SharePoint site ID
     */
    public String fetchSiteLibraries(String token, String siteId, int top) {
        log.debug("Graph: GET /sites/{}/drives top={}", siteId, top);
        return graphClient.get()
                .uri(b -> b.path("/sites/{siteId}/drives")
                        .queryParam("$select", "id,name,description,webUrl,driveType,createdDateTime,lastModifiedDateTime,quota")
                        .queryParam("$top", top)
                        .build(siteId))
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                .retrieve()
                .body(String.class);
    }
}