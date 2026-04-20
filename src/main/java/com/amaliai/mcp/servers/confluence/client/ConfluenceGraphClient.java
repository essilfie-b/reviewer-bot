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
        try {
            return confluenceApiClient.get()
                    .uri(b -> b.path("/{cloudId}/wiki/rest/api/search")
                            .queryParam("cql", cql)
                            .queryParam("limit", limit)
                            .build(cloudId))
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
