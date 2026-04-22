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
                        .queryParam("limit", limit)
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
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param spaceId the numeric Confluence space ID (resolved from the key by the caller)
     * @param limit   maximum number of pages to return
     * @return raw JSON response body from {@code GET /wiki/api/v2/pages?space-id={id}}
     */
    public String listPagesBySpaceId(String token, String cloudId, String spaceId, int limit) {
        log.debug("Confluence v2: GET /{}/wiki/api/v2/pages space-id={} limit={}", cloudId, spaceId, limit);
        return fetch(confluenceApiClient.get()
                .uri(b -> b.path("/{cloudId}/wiki/api/v2/pages")
                        .queryParam("space-id", spaceId)
                        .queryParam("limit", limit)
                        .queryParam("status", "current")
                        .build(cloudId)), token);
    }

    /** Attaches the bearer token, executes the request, and maps HTTP errors to domain exceptions. */
    private String fetch(RestClient.RequestHeadersSpec<?> request, String token) {
        try {
            return request
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException e) {
            log.error("Confluence API error {} — response body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new ConfluenceAuthException("Confluence API returned " + e.getStatusCode(), e);
            }
            throw new ConfluenceOperationException("Confluence API returned " + e.getStatusCode(), e);
        }
    }
}
