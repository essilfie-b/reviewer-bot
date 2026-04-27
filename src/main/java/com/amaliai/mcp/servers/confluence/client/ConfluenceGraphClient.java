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

    /**
     * Lists Confluence spaces for the tenant, optionally filtered by type/status,
     * with cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param type    optional space type filter: global|personal|collaboration|knowledge_base
     * @param status  optional space status filter: current|archived
     * @param limit   maximum number of spaces to return (Confluence caps at 250)
     * @param cursor  optional pagination cursor from a previous response's {@code _links.next}
     * @return raw JSON response body from {@code GET /wiki/api/v2/spaces}
     */
    public String listSpaces(String token, String cloudId, String type, String status,
                             int limit, String cursor) {
        log.debug("Confluence: GET /{}/wiki/api/v2/spaces type={} status={} limit={} cursor={}",
                cloudId, type, status, limit, cursor);
        try {
            return confluenceApiClient.get()
                    .uri(b -> {
                        var u = b.path("/{cloudId}/wiki/api/v2/spaces")
                                .queryParam("description-format", "plain")
                                .queryParam("limit", limit);
                        if (type != null && !type.isBlank())     u.queryParam("type", type);
                        if (status != null && !status.isBlank()) u.queryParam("status", status);
                        if (cursor != null && !cursor.isBlank()) u.queryParam("cursor", cursor);
                        return u.build(cloudId);
                    })
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
        try {
            return confluenceApiClient.get()
                    .uri(b -> b.path("/{cloudId}/wiki/api/v2/spaces/{spaceId}")
                            .queryParam("description-format", "plain")
                            .build(cloudId, spaceId))
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

    /**
     * Retrieves the direct child pages of a specific Confluence page.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param pageId  the numeric page ID whose children to fetch
     * @param limit   maximum number of child pages to return
     * @return raw JSON response body from {@code GET /wiki/api/v2/pages/{pageId}/children}
     */
    public String getPageChildren(String token, String cloudId, String pageId, int limit) {
        log.debug("Confluence: GET /{}/wiki/api/v2/pages/{}/children limit={}", cloudId, pageId, limit);
        try {
            return confluenceApiClient.get()
                    .uri(b -> b.path("/{cloudId}/wiki/api/v2/pages/{pageId}/children")
                            .queryParam("limit", limit)
                            .build(cloudId, pageId))
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

    /**
     * Retrieves attachments for a specific Confluence page.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param pageId  the numeric page ID
     * @param limit   maximum number of attachments to return
     * @return raw JSON response body from {@code GET /wiki/api/v2/pages/{pageId}/attachments}
     */
    public String getAttachments(String token, String cloudId, String pageId, int limit) {
        log.debug("Confluence: GET /{}/wiki/api/v2/pages/{}/attachments limit={}", cloudId, pageId, limit);
        try {
            return confluenceApiClient.get()
                    .uri(b -> b.path("/{cloudId}/wiki/api/v2/pages/{pageId}/attachments")
                            .queryParam("limit", limit)
                            .build(cloudId, pageId))
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

    /**
     * Lists Confluence spaces for the tenant, optionally filtered by type/status,
     * with cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param type    optional space type filter: global|personal|collaboration|knowledge_base
     * @param status  optional space status filter: current|archived
     * @param limit   maximum number of spaces to return (Confluence caps at 250)
     * @param cursor  optional pagination cursor from a previous response's {@code _links.next}
     * @return raw JSON response body from {@code GET /wiki/api/v2/spaces}
     */
    public String listSpaces(String token, String cloudId, String type, String status,
                             int limit, String cursor) {
        log.debug("Confluence: GET /{}/wiki/api/v2/spaces type={} status={} limit={} cursor={}",
                cloudId, type, status, limit, cursor);
        try {
            return confluenceApiClient.get()
                    .uri(b -> {
                        var u = b.path("/{cloudId}/wiki/api/v2/spaces")
                                .queryParam("description-format", "plain")
                                .queryParam("limit", limit);
                        if (type != null && !type.isBlank())     u.queryParam("type", type);
                        if (status != null && !status.isBlank()) u.queryParam("status", status);
                        if (cursor != null && !cursor.isBlank()) u.queryParam("cursor", cursor);
                        return u.build(cloudId);
                    })
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

    /**
     * Lists Confluence spaces for the tenant, optionally filtered by type/status,
     * with cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param type    optional space type filter: global|personal|collaboration|knowledge_base
     * @param status  optional space status filter: current|archived
     * @param limit   maximum number of spaces to return (Confluence caps at 250)
     * @param cursor  optional pagination cursor from a previous response's {@code _links.next}
     * @return raw JSON response body from {@code GET /wiki/api/v2/spaces}
     */
    public String listSpaces(String token, String cloudId, String type, String status,
                             int limit, String cursor) {
        log.debug("Confluence: GET /{}/wiki/api/v2/spaces type={} status={} limit={} cursor={}",
                cloudId, type, status, limit, cursor);
        try {
            return confluenceApiClient.get()
                    .uri(b -> {
                        var u = b.path("/{cloudId}/wiki/api/v2/spaces")
                                .queryParam("description-format", "plain")
                                .queryParam("limit", limit);
                        if (type != null && !type.isBlank())     u.queryParam("type", type);
                        if (status != null && !status.isBlank()) u.queryParam("status", status);
                        if (cursor != null && !cursor.isBlank()) u.queryParam("cursor", cursor);
                        return u.build(cloudId);
                    })
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
        try {
            return confluenceApiClient.get()
                    .uri(b -> b.path("/{cloudId}/wiki/api/v2/spaces/{spaceId}")
                            .queryParam("description-format", "plain")
                            .build(cloudId, spaceId))
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

    /**
     * Retrieves the direct child pages of a specific Confluence page.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param pageId  the numeric page ID whose children to fetch
     * @param limit   maximum number of child pages to return
     * @return raw JSON response body from {@code GET /wiki/api/v2/pages/{pageId}/children}
     */
    public String getPageChildren(String token, String cloudId, String pageId, int limit) {
        log.debug("Confluence: GET /{}/wiki/api/v2/pages/{}/children limit={}", cloudId, pageId, limit);
        try {
            return confluenceApiClient.get()
                    .uri(b -> b.path("/{cloudId}/wiki/api/v2/pages/{pageId}/children")
                            .queryParam("limit", limit)
                            .build(cloudId, pageId))
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

    /**
     * Retrieves attachments for a specific Confluence page.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID identifying the tenant
     * @param pageId  the numeric page ID
     * @param limit   maximum number of attachments to return
     * @return raw JSON response body from {@code GET /wiki/api/v2/pages/{pageId}/attachments}
     */
    public String getAttachments(String token, String cloudId, String pageId, int limit) {
        log.debug("Confluence: GET /{}/wiki/api/v2/pages/{}/attachments limit={}", cloudId, pageId, limit);
        try {
            return confluenceApiClient.get()
                    .uri(b -> b.path("/{cloudId}/wiki/api/v2/pages/{pageId}/attachments")
                            .queryParam("limit", limit)
                            .build(cloudId, pageId))
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
        try {
            return confluenceApiClient.get()
                    .uri(b -> b.path("/{cloudId}/wiki/api/v2/spaces/{spaceId}")
                            .queryParam("description-format", "plain")
                            .build(cloudId, spaceId))
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
