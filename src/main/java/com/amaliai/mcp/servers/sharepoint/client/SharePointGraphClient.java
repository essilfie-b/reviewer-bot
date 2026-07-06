package com.amaliai.mcp.servers.sharepoint.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class SharePointGraphClient {

    private static final String SELECT_ITEM_FIELDS =
            "id,name,webUrl,createdBy,lastModifiedBy,lastModifiedDateTime,size,file,folder";

    private final RestClient graphClient;
    private final RestClient graphClientNoRedirect;

    public SharePointGraphClient(
            @Qualifier("graphClient") RestClient graphClient,
            @Qualifier("graphClientNoRedirect") RestClient graphClientNoRedirect) {
        this.graphClient = graphClient;
        this.graphClientNoRedirect = graphClientNoRedirect;
    }

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
                        .queryParam("$select", "name,size,webUrl,parentReference,file")
                        .build(itemId))
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                .retrieve()
                .body(String.class);
    }

    /**
     * Lists files and folders inside a specific folder by its drive item ID,
     * ordered by most-recently-modified first.
     */
    public String fetchFolderChildren(String token, String folderId) {
        log.debug("Graph: GET /me/drive/items/{}/children", folderId);
        return graphClient.get()
                .uri(b -> b.path("/me/drive/items/{id}/children")
                        .queryParam("$select", SELECT_ITEM_FIELDS)
                        .queryParam("$top", MAX_TOP)
                        .queryParam("$orderby", "lastModifiedDateTime desc")
                        .build(folderId))
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                .retrieve()
                .body(String.class);
    }

    /**
     * Fetches full metadata for a single drive item, including creation and modification details.
     */
    public String fetchItemFullMetadata(String token, String itemId) {
        log.debug("Graph: GET /me/drive/items/{}/full-metadata", itemId);
        return graphClient.get()
                .uri(b -> b.path("/me/drive/items/{id}")
                        .queryParam("$select",
                                "id,name,size,webUrl,createdBy,createdDateTime," +
                                "lastModifiedBy,lastModifiedDateTime,file")
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

    /**
     * Fetches a time-limited CDN download URL for a drive item by hitting the
     * {@code /content} endpoint and capturing the {@code Location} header from
     * the HTTP 302 redirect, without following it.
     *
     * @return the pre-signed CDN URL, or {@code null} if no Location header was present
     */
    public String fetchDownloadUrl(String token, String itemId) {
        log.debug("Graph: GET /me/drive/items/{}/content (no-redirect, capture Location)", itemId);
        final String[] locationHolder = new String[1];
        graphClientNoRedirect.get()
                .uri(b -> b.path("/me/drive/items/{id}/content").build(itemId))
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                .exchange((request, response) -> {
                    URI location = response.getHeaders().getLocation();
                    locationHolder[0] = location != null ? location.toString() : null;
                    return null;
                });
        return locationHolder[0];
    }

    /**
     * Moves a drive item under a new parent folder, optionally renaming it, via a
     * Graph PATCH on the item's {@code parentReference}.
     *
     * @param targetFolderId the drive item ID of the destination folder
     * @param newName        optional new name; when blank the item keeps its name
     * @return the raw Graph response for the updated drive item
     */
    public String moveItem(String token, String itemId, String targetFolderId, String newName) {
        log.debug("Graph: PATCH /me/drive/items/{} -> parent {}",
                sanitizeLogValue(itemId), sanitizeLogValue(targetFolderId));
        Map<String, Object> payload = new HashMap<>();
        payload.put("parentReference", Map.of("id", targetFolderId));
        if (newName != null && !newName.isBlank()) {
            payload.put("name", newName);
        }
        String response = graphClient.patch()
                .uri(b -> b.path("/me/drive/items/{id}").build(itemId))
                .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                .body(payload)
                .retrieve()
                .body(String.class);
        if (response == null) {
            throw new IllegalStateException("Graph returned an empty response for moveItem");
        }
        return response;
    }

    /** Replaces line breaks and tabs so caller-supplied values cannot forge log lines. */
    private static String sanitizeLogValue(String value) {
        return value == null ? "null" : value.replaceAll("[\r\n\t]", "_");
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