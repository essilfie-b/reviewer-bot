package com.amaliai.mcp.servers.confluence.service;

import com.amaliai.mcp.servers.confluence.client.ConfluenceGraphClient;
import com.amaliai.mcp.servers.confluence.dto.SpaceInfo;
import com.amaliai.mcp.servers.confluence.exception.ConfluenceOperationException;
import com.amaliai.mcp.servers.confluence.util.ConfluenceServiceUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Business logic for Confluence MCP tools.
 * <p>
 * Responsible for input validation, CQL query construction, delegating HTTP
 * calls to {@link ConfluenceGraphClient}, and shaping raw API responses into
 * clean JSON for the LLM.
 * <p>
 * <b>Error contract:</b> validation failures throw {@link IllegalArgumentException};
 * API or serialisation failures throw {@link ConfluenceOperationException}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceService {

    private static final int DEFAULT_LIMIT    = 20;
    private static final int MAX_LIMIT        = 50;
    private static final int MAX_QUERY_LENGTH = 1_000;
    private static final int DEFAULT_SPACES_LIMIT = 250;
    private static final int MAX_SPACES_LIMIT = 250;
    private static final Duration SPACE_INFO_CACHE_TTL = Duration.ofHours(24);

    private static final Set<String> VALID_SPACE_TYPES = Set.of("global", "personal", "collaboration", "knowledge_base");
    private static final Set<String> VALID_SPACE_STATUSES = Set.of("current", "archived");

    private final ConfluenceGraphClient confluenceClient;
    private final Cache<String, SpaceInfo> spaceInfoCache = Caffeine.newBuilder()
            .expireAfterWrite(SPACE_INFO_CACHE_TTL)
            .maximumSize(1_000)
            .build();

    /**
     * Validates that a space type filter is one of the allowed enum values.
     */
    private void validateSpaceType(String type) {
        if (type != null && !type.isBlank() && !VALID_SPACE_TYPES.contains(type.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Invalid space type '" + type + "'. Must be one of: " + VALID_SPACE_TYPES);
        }
    }

    /**
     * Validates that a space status filter is one of the allowed enum values.
     */
    private void validateSpaceStatus(String status) {
        if (status != null && !status.isBlank() && !VALID_SPACE_STATUSES.contains(status.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Invalid space status '" + status + "'. Must be one of: " + VALID_SPACE_STATUSES);
        }
    }

    /**
     * Searches Confluence pages by keyword using CQL and returns a trimmed JSON
     * array of matching results.
     *
     * @param token    the user's Confluence access token
     * @param cloudId  the Atlassian cloud ID for the user's tenant
     * @param query    keyword or phrase to search for (must not be blank)
     * @param spaceKey optional space key to restrict the search scope
     * @param limit    maximum results; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @return JSON array string with fields: id, title, type, spaceKey, spaceName, url, excerpt, lastModified
     * @throws IllegalArgumentException     if {@code query} is blank or exceeds the length limit
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String searchContent(String token, String cloudId,
                                String query, String spaceKey, Integer limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be empty");
        }
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "query exceeds maximum length of " + MAX_QUERY_LENGTH + " characters");
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        String cql = ConfluenceServiceUtil.buildCql(query, spaceKey);
        log.info("Confluence search — cloudId={} cql='{}' limit={}", cloudId, cql, effectiveLimit);

        String raw = confluenceClient.search(token, cloudId, cql, effectiveLimit);
        return ConfluenceServiceUtil.parseSearchResponse(raw);
    }

    /**
     * Fetches metadata for a single Confluence page by its ID (v2 API).
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param pageId  the Confluence page ID (must not be blank)
     * @return JSON object with fields: id, title, type, spaceKey, url, lastModified
     * @throws IllegalArgumentException     if {@code pageId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String getPage(String token, String cloudId, String pageId) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be empty");
        }
        log.info("Confluence getPage — cloudId={} pageId={}", cloudId, pageId);
        return ConfluenceServiceUtil.parseV2PageResponse(confluenceClient.getPage(token, cloudId, pageId));
    }

    /**
     * Fetches metadata and full text content for a single Confluence page (v2 API).
     * The page body is stripped of HTML and truncated at
     * characters if the page is very long.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param pageId  the Confluence page ID (must not be blank)
     * @return JSON object with metadata fields plus {@code content} and {@code truncated}
     * @throws IllegalArgumentException     if {@code pageId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String getPageContent(String token, String cloudId, String pageId) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be empty");
        }
        log.info("Confluence getPageContent — cloudId={} pageId={}", cloudId, pageId);
        return ConfluenceServiceUtil.parseV2PageContentResponse(confluenceClient.getPageWithContent(token, cloudId, pageId));
    }

    /**
     * Lists pages in a Confluence space (v2 API, two-step).
     * Step 1: resolves the human-readable {@code spaceKey} to a numeric space ID and display name.
     * Step 2: lists current pages in that space with optional cursor-based pagination.
     *
     * @param token    the user's Confluence access token
     * @param cloudId  the Atlassian cloud ID for the user's tenant
     * @param spaceKey the space key to list pages from (must not be blank)
     * @param limit    maximum results; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @param cursor   optional pagination cursor from a previous response's nextCursor
     * @return JSON object with {@code results} (array of page objects) and {@code nextCursor}
     * @throws IllegalArgumentException     if {@code spaceKey} is blank
     * @throws ConfluenceOperationException if the space is not found or the API response cannot be parsed
     */
    public String listPages(String token, String cloudId, String spaceKey, Integer limit, String cursor) {
        if (spaceKey == null || spaceKey.isBlank()) {
            throw new IllegalArgumentException("spaceKey must not be empty");
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        log.info("Confluence listPages — cloudId={} spaceKey={} limit={} cursor={}", cloudId, spaceKey.trim(), effectiveLimit, cursor);

        SpaceInfo space = resolveSpace(token, cloudId, spaceKey);

        return ConfluenceServiceUtil.parsePagesListResponse(
                confluenceClient.listPagesBySpaceId(token, cloudId, space.id(), effectiveLimit, cursor),
                space.key(), space.name());
    }

    private SpaceInfo resolveSpace(String token, String cloudId, String spaceKey) {
        String normalizedKey = spaceKey.trim().toUpperCase(Locale.ROOT);
        String cacheKey = cloudId + ":" + normalizedKey;
        SpaceInfo space = spaceInfoCache.getIfPresent(cacheKey);
        if (space == null) {
            // Try exact key first, then sanitized key (strips underscores/spaces e.g. HR_IT → HRIT)
            String sanitizedKey = normalizedKey.replaceAll("[^A-Z0-9]", "");
            try {
                space = ConfluenceServiceUtil.parseSpaceResult(
                        confluenceClient.getSpaceByKey(token, cloudId, normalizedKey), normalizedKey);
            } catch (ConfluenceOperationException e) {
                if (!sanitizedKey.equals(normalizedKey)) {
                    try {
                        space = ConfluenceServiceUtil.parseSpaceResult(
                                confluenceClient.getSpaceByKey(token, cloudId, sanitizedKey), sanitizedKey);
                    } catch (ConfluenceOperationException ignored) {
                        // fall through to name search
                    }
                }
            }
            // Fall back to fetching all spaces and matching by name or key client-side
            if (space == null) {
                log.info("Space key '{}' not found by key lookup, falling back to full name search for: {}", normalizedKey, spaceKey.trim());
                space = ConfluenceServiceUtil.parseSpaceResult(
                        confluenceClient.listSpaces(token, cloudId, null, null, null, DEFAULT_SPACES_LIMIT, null),
                        spaceKey.trim());
            }
            spaceInfoCache.put(cacheKey, space);
        }
        return space;
    }
    /**
     * Retrieves details of a single Confluence space by its key.
     *
     * <p>Resolution is two-step:
     * 1) resolve {@code spaceKey} to a numeric space ID, 2) fetch full space details by ID.
     * The space ID lookup is cached for 24 hours to avoid redundant API calls.
     *
     * @param token    the user's Confluence access token
     * @param cloudId  the Atlassian cloud ID for the user's tenant
     * @param spaceKey the human-readable Confluence space key (must not be blank)
     * @return JSON object string with fields: id, key, name, type, status,
     *         authorId, createdAt, homepageId, description, url
     * @throws IllegalArgumentException     if {@code spaceKey} is blank
     * @throws ConfluenceOperationException if the space is not found or the API response cannot be parsed
     */
    public String getSpace(String token, String cloudId, String spaceKey) {
        if (spaceKey == null || spaceKey.isBlank()) {
            throw new IllegalArgumentException("spaceKey must not be empty");
        }

        log.info("Confluence getSpace — cloudId={} spaceKey={}", cloudId, spaceKey.trim().toUpperCase(Locale.ROOT));

        SpaceInfo space = resolveSpace(token, cloudId, spaceKey);
        String raw = confluenceClient.getSpace(token, cloudId, space.id());
        return ConfluenceServiceUtil.parseSpaceResponse(raw);
    }

    /**
     * Lists Confluence spaces for the tenant with optional type/status filters
     * and cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param type    optional space type: global|personal|collaboration|knowledge_base
     * @param status  optional space status: current|archived
     * @param query   optional space name/title query for server-side filtering
     * @param limit   maximum spaces; clamped to [{@value DEFAULT_SPACES_LIMIT}, {@value MAX_SPACES_LIMIT}]
     * @param cursor  optional opaque pagination cursor from a previous call
     * @return JSON object string with {@code results} (array of spaces) and {@code nextCursor}
     * @throws IllegalArgumentException     if type or status filters are invalid enum values
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String listSpaces(String token, String cloudId,
                             String type, String status, String query, Integer limit, String cursor) {
        validateSpaceType(type);
        validateSpaceStatus(status);

        int effectiveLimit = (limit == null || limit <= 0)
                ? DEFAULT_SPACES_LIMIT
                : Math.min(limit, MAX_SPACES_LIMIT);
        String normalizedQuery = (query == null || query.isBlank()) ? null : query.trim();

        log.info("Confluence listSpaces — cloudId={} type={} status={} query={} limit={} cursor={}",
                cloudId, type, status, normalizedQuery, effectiveLimit, cursor);

        String raw = confluenceClient.listSpaces(token, cloudId, type, status, normalizedQuery, effectiveLimit, cursor);

        return ConfluenceServiceUtil.parseSpacesListResponse(raw);
    }

    /**
     * Returns the attachments for a Confluence page as a trimmed JSON array with cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param pageId  numeric ID of the page whose attachments to fetch (must not be blank)
     * @param limit   maximum results; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @param cursor  optional pagination cursor from a previous response's nextCursor
     * @return JSON object with {@code results} (array of attachments) and {@code nextCursor}
     * @throws IllegalArgumentException     if {@code pageId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String getAttachments(String token, String cloudId, String pageId, Integer limit, String cursor) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be empty");
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        log.info("Confluence attachments — cloudId={} pageId={} limit={} cursor={}", cloudId, pageId, effectiveLimit, cursor);

        String raw = confluenceClient.getAttachments(token, cloudId, pageId, effectiveLimit, cursor);
        return ConfluenceServiceUtil.parseAttachmentsResponse(raw);
    }

    /**
     * Returns all pages in a Confluence space that have at least one file attachment,
     * with each page's attachment list embedded in the response.
     * Avoids the N+1 pattern of calling getAttachments for every page individually.
     *
     * @param token    the user's Confluence access token
     * @param cloudId  the Atlassian cloud ID for the user's tenant
     * @param spaceKey space key or display name (e.g. "ENG" or "Amalitech Handbook")
     * @param limit    maximum pages to inspect; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @return JSON object with {@code results} — array of page objects each containing an {@code attachments} array
     */
    public String getPagesWithAttachments(String token, String cloudId, String spaceKey, Integer limit) {
        if (spaceKey == null || spaceKey.isBlank()) {
            throw new IllegalArgumentException("spaceKey must not be empty");
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        log.info("Confluence getPagesWithAttachments — cloudId={} spaceKey={} limit={}", cloudId, spaceKey.trim(), effectiveLimit);

        SpaceInfo space = resolveSpace(token, cloudId, spaceKey);
        String pagesRaw = confluenceClient.listPagesBySpaceId(token, cloudId, space.id(), effectiveLimit, null);
        List<String> pageIds = ConfluenceServiceUtil.extractPageIds(pagesRaw);

        Map<String, String> attachmentRawByPageId = pageIds.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        Function.identity(),
                        pageId -> confluenceClient.getAttachments(token, cloudId, pageId, MAX_LIMIT, null)));

        return ConfluenceServiceUtil.buildPagesWithAttachmentsResponse(
                pagesRaw, space.key(), space.name(), attachmentRawByPageId);
    }

    /**
     * Returns the direct child pages of a Confluence page as a trimmed JSON array with cursor-based pagination.
     *
     * @param token   the user's Confluence access token
     * @param cloudId the Atlassian cloud ID for the user's tenant
     * @param pageId  numeric ID of the parent page (must not be blank)
     * @param limit   maximum results; clamped to [{@value DEFAULT_LIMIT}, {@value MAX_LIMIT}]
     * @param cursor  optional pagination cursor from a previous response's nextCursor
     * @return JSON object with {@code results} (array of child pages) and {@code nextCursor}
     * @throws IllegalArgumentException     if {@code pageId} is blank
     * @throws ConfluenceOperationException if the API response cannot be parsed
     */
    public String getPageChildren(String token, String cloudId, String pageId, Integer limit, String cursor) {
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be empty");
        }

        int effectiveLimit = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        log.info("Confluence page children — cloudId={} pageId={} limit={} cursor={}", cloudId, pageId, effectiveLimit, cursor);

        String raw = confluenceClient.getPageChildren(token, cloudId, pageId, effectiveLimit, cursor);
        return ConfluenceServiceUtil.parsePageChildrenResponse(raw);
    }
}

