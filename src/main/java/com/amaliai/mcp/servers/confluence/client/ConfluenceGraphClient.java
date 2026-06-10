package com.amaliai.mcp.servers.confluence.client;

import com.amaliai.mcp.servers.confluence.exception.ConfluenceAuthException;
import com.amaliai.mcp.servers.confluence.exception.ConfluenceOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Thin HTTP client wrapper for the Confluence Cloud REST API v2.
 * <p>
 * Every method maps to exactly one Confluence API endpoint. Business logic,
 * response parsing, and error handling are the responsibility of callers.
 * <p>
 * The underlying {@link RestClient} bean ({@code confluenceApiClient}) has base URL
 * {@code https://api.atlassian.com/ex/confluence}. The per-user {@code cloudId}
 * segment is appended per-call since it differs across users.
 */
@Slf4j
@Component
public class ConfluenceGraphClient {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String QUERY_PARAM_LIMIT = "limit";
    private static final String QUERY_PARAM_CURSOR = "cursor";

    private final RestClient confluenceApiClient;

    public ConfluenceGraphClient(@Qualifier("confluenceApiClient") RestClient confluenceApiClient) {
        this.confluenceApiClient = confluenceApiClient;
    }

    /**
     * Searches Confluence content using a CQL (Confluence Query Language) expression.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param cql     the CQL query string (already sanitised by the caller)
     * @param limit   maximum number of results to return
     * @return raw JSON response body from {@code GET /wiki/rest/api/search}
     */
    public String search(String token, String cloudId, String cql, int limit) {
        log.debug("Confluence: GET /{}/wiki/rest/api/search cql='{}' limit={}", cloudId, cql, limit);
        return fetch(confluenceApiClient.get()
                .uri(b -> b.path("/{cloudId}/wiki/rest/api/search")
                        .queryParam("cql", cql)
                        .queryParam(QUERY_PARAM_LIMIT, limit)
                        .queryParam("expand", "content.space")
                        .build(cloudId)), token);
    }

    /**
     * Fetches metadata for a single Confluence page by its ID (v2 API, scope: {@code read:page:confluence}).
     * Does not include the page body; see {@link #getPageWithContent} for that.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param pageId  the Confluence page ID
     * @return raw JSON response body from {@code GET /wiki/api/v2/pages/{id}}
     */
    public String getPage(String token, String cloudId, String pageId) {
        log.debug("Confluence v2: GET /{}/wiki/api/v2/pages/{}", cloudId, pageId);
        return fetch(confluenceApiClient.get()
                .uri(b -> b.path("/{cloudId}/wiki/api/v2/pages/{pageId}")
                        .build(cloudId, pageId)), token);
    }

    /**
     * Fetches metadata and rendered HTML body for a single Confluence page (v2 API, scope: {@code read:page:confluence}).
     * The body is returned as rendered HTML under {@code body.value}.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param pageId  the Confluence page ID
     * @return raw JSON response body from {@code GET /wiki/api/v2/pages/{id}?body-format=view}
     */
    public String getPageWithContent(String token, String cloudId, String pageId) {
        log.debug("Confluence v2: GET /{}/wiki/api/v2/pages/{} body-format=view", cloudId, pageId);
        return fetch(confluenceApiClient.get()
                .uri(b -> b.path("/{cloudId}/wiki/api/v2/pages/{pageId}")
                        .queryParam("body-format", "view")
                        .build(cloudId, pageId)), token);
    }

    /**
     * Looks up a Confluence space by its human-readable key (v2 API, scope: {@code read:space:confluence}).
     * Used as the first step of a page listing to resolve a space key to its numeric ID and display name.
     *
     * @param token    the user's Confluence access token
     * @param cloudId  the Atlassian cloud ID identifying the tenant
     * @param spaceKey the Confluence space key (e.g. "ENG", "LMS")
     * @return raw JSON response body from {@code GET /wiki/api/v2/spaces?keys={key}}
     */
    public String getSpaceByKey(String token, String cloudId, String spaceKey) {
        log.debug("Confluence v2: GET /{}/wiki/api/v2/spaces?keys={}", cloudId, spaceKey);
        return fetch(confluenceApiClient.get()
                .uri(b -> b.path("/{cloudId}/wiki/api/v2/spaces")
                        .queryParam("keys", spaceKey)
                        .build(cloudId)), token);
    }

    /**
     * Lists current pages within a Confluence space by its numeric space ID (v2 API, scope: {@code read:page:confluence}).
     * Only returns pages with {@code status=current}, excluding drafts and archived pages.
     * Supports cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param spaceId the numeric Confluence space ID (resolved from the key by the caller)
     * @param limit   maximum number of pages to return
     * @param cursor  optional pagination cursor from a previous response's nextCursor
     * @return raw JSON response body from {@code GET /wiki/api/v2/pages?space-id={id}}
     */
    public String listPagesBySpaceId(String token, String cloudId, String spaceId, int limit, String cursor) {
        log.debug("Confluence v2: GET /{}/wiki/api/v2/pages space-id={} limit={} cursor={}", cloudId, spaceId, limit, cursor);
        return fetch(confluenceApiClient.get()
                .uri(b -> {
                    var u = b.path("/{cloudId}/wiki/api/v2/pages")
                            .queryParam("space-id", spaceId)
                            .queryParam(QUERY_PARAM_LIMIT, limit)
                            .queryParam("status", "current");
                    if (cursor != null && !cursor.isBlank()) u.queryParam(QUERY_PARAM_CURSOR, cursor);
                    return u.build(cloudId);
                }), token);
    }

    /** Attaches the bearer token, executes the request, and maps HTTP errors to domain exceptions. */
    private String fetch(RestClient.RequestHeadersSpec<?> request, String token) {
        try {
            return request
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException e) {
            log.error("Confluence API error {}", e.getStatusCode());
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new ConfluenceAuthException("Confluence API returned " + e.getStatusCode(), e);
            }
            throw new ConfluenceOperationException("Confluence API returned " + e.getStatusCode(), e);
        }
    }

    /**
     * Lists Confluence spaces for the tenant, optionally filtered by type/status,
     * with cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param type    optional space type filter: global|personal|collaboration|knowledge_base
     * @param status  optional space status filter: current|archived
     * @param query   optional free-text space name/title query
     * @param limit   maximum number of spaces to return (Confluence caps at 250)
     * @param cursor  optional pagination cursor from a previous response's {@code _links.next}
     * @return raw JSON response body from {@code GET /wiki/api/v2/spaces}
     */
    public String listSpaces(String token, String cloudId, String type, String status, String query,
                             int limit, String cursor) {
        log.debug("Confluence: GET /{}/wiki/api/v2/spaces type={} status={} query={} limit={} cursor={}",
                cloudId, type, status, query, limit, cursor);
        return fetch(confluenceApiClient.get()
                .uri(b -> {
                    var u = b.path("/{cloudId}/wiki/api/v2/spaces")
                            .queryParam("description-format", "plain")
                            .queryParam(QUERY_PARAM_LIMIT, limit);
                    if (type != null && !type.isBlank())     u.queryParam("type", type);
                    if (status != null && !status.isBlank()) u.queryParam("status", status);
                    if (query != null && !query.isBlank())   u.queryParam("query", query);
                    if (cursor != null && !cursor.isBlank()) u.queryParam(QUERY_PARAM_CURSOR, cursor);
                    return u.build(cloudId);
                }), token);
    }

    /**
     * Fetches details for a single Confluence space by its numeric ID.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param spaceId the Confluence space ID
     * @return raw JSON response body from {@code GET /wiki/api/v2/spaces/{id}}
     */
    public String getSpace(String token, String cloudId, String spaceId) {
        log.debug("Confluence: GET /{}/wiki/api/v2/spaces/{}", cloudId, spaceId);
        return fetch(confluenceApiClient.get()
                .uri(b -> b.path("/{cloudId}/wiki/api/v2/spaces/{spaceId}")
                        .queryParam("description-format", "plain")
                        .build(cloudId, spaceId)), token);
    }

     /**
     * Retrieves the direct child pages of a specific Confluence page.
     * Supports cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param pageId  the numeric page ID whose children to fetch
     * @param limit   maximum number of child pages to return
     * @param cursor  optional pagination cursor from a previous response's nextCursor
     * @return raw JSON response body from {@code GET /wiki/api/v2/pages/{pageId}/children}
     */
     public String getPageChildren(String token, String cloudId, String pageId, int limit, String cursor) {
         log.debug("Confluence: GET /{}/wiki/api/v2/pages/{}/children limit={} cursor={}", cloudId, pageId, limit, cursor);
         return fetch(confluenceApiClient.get()
                 .uri(b -> {
                     var u = b.path("/{cloudId}/wiki/api/v2/pages/{pageId}/children")
                             .queryParam(QUERY_PARAM_LIMIT, limit);
                     if (cursor != null && !cursor.isBlank()) u.queryParam(QUERY_PARAM_CURSOR, cursor);
                     return u.build(cloudId, pageId);
                 }), token);
     }

     /**
     * Retrieves attachments for a specific Confluence page.
     * Supports cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param pageId  the numeric page ID
     * @param limit   maximum number of attachments to return
     * @param cursor  optional pagination cursor from a previous response's nextCursor
     * @return raw JSON response body from {@code GET /wiki/api/v2/pages/{pageId}/attachments}
     */
     public String getAttachments(String token, String cloudId, String pageId, int limit, String cursor) {
         log.debug("Confluence: GET /{}/wiki/api/v2/pages/{}/attachments limit={} cursor={}", cloudId, pageId, limit, cursor);
         return fetch(confluenceApiClient.get()
                 .uri(b -> {
                     var u = b.path("/{cloudId}/wiki/api/v2/pages/{pageId}/attachments")
                             .queryParam(QUERY_PARAM_LIMIT, limit);
                     if (cursor != null && !cursor.isBlank()) u.queryParam(QUERY_PARAM_CURSOR, cursor);
                     return u.build(cloudId, pageId);
                 }), token);
     }
}
