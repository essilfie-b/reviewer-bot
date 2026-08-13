package com.amaliai.mcp.servers.confluence.service;

import com.amaliai.mcp.servers.confluence.client.ConfluenceGraphClient;
import com.amaliai.mcp.servers.confluence.dto.ArchivalCandidate;
import com.amaliai.mcp.servers.confluence.exception.ConfluenceOperationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Identifies Confluence pages that have gone stale and are candidates for archival.
 * <p>
 * A page is a candidate when it has not been updated for at least
 * {@code retentionDays} days and is not pinned by a space administrator.
 * <p>
 * <b>Error contract:</b> validation failures throw {@link IllegalArgumentException};
 * API or serialisation failures throw {@link ConfluenceOperationException}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfluenceArchivalService {

    private static final int DEFAULT_RETENTION_DAYS = 365;
    private static final int MAX_RETENTION_DAYS = 3_650;
    private static final int PAGE_FETCH_SIZE = 50;
    private static final Duration CANDIDATE_CACHE_TTL = Duration.ofMinutes(30);

    private static final Set<String> VALID_STATUSES = Set.of("current", "archived", "draft");

    private final ConfluenceGraphClient confluenceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Cache<String, List<ArchivalCandidate>> candidateCache = Caffeine.newBuilder()
            .expireAfterWrite(CANDIDATE_CACHE_TTL)
            .maximumSize(500)
            .build();

    /**
     * Finds archival candidates in a space.
     *
     * @param token         the user's Confluence access token
     * @param cloudId       the Atlassian cloud ID for the user's tenant
     * @param spaceKey      the space to scan (must not be blank)
     * @param status        page status filter; one of {@code current}, {@code archived}, {@code draft}
     * @param retentionDays pages untouched for at least this many days are candidates
     * @return the archival candidates, newest-stale first
     */
    public List<ArchivalCandidate> findCandidates(String token, String cloudId, String spaceKey,
                                                  String status, Integer retentionDays) {
        validateSpaceKey(spaceKey);
        validateStatus(status);
        int retention = resolveRetention(retentionDays);

        return candidateCache.get(spaceKey, key -> scanSpace(token, cloudId, key, status, retention));
    }

    private void validateSpaceKey(String spaceKey) {
        if (spaceKey == null || spaceKey.isBlank()) {
            throw new IllegalArgumentException("spaceKey must not be blank");
        }
    }

    private void validateStatus(String status) {
        if (status != null && !status.isBlank() && !VALID_STATUSES.contains(status.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Invalid page status '" + status + "'. Must be one of: " + VALID_STATUSES);
        }
    }

    private int resolveRetention(Integer retentionDays) {
        if (retentionDays == null) {
            return DEFAULT_RETENTION_DAYS;
        }
        if (retentionDays < 1 || retentionDays > MAX_RETENTION_DAYS) {
            throw new IllegalArgumentException(
                    "retentionDays must be between 1 and " + MAX_RETENTION_DAYS);
        }
        return retentionDays;
    }

    private List<ArchivalCandidate> scanSpace(String token, String cloudId, String spaceKey,
                                              String status, int retentionDays) {
        List<ArchivalCandidate> candidates = new ArrayList<>();
        String cursor = null;

        do {
            String raw = confluenceClient.getSpacePages(token, cloudId, spaceKey, status,
                    PAGE_FETCH_SIZE, cursor);
            JsonNode root = readTree(raw);

            for (JsonNode page : root.path("results")) {
                ArchivalCandidate candidate = toCandidate(page, retentionDays);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }

            cursor = root.path("_links").path("next").asText(null);
        } while (cursor != null && candidates.size() < PAGE_FETCH_SIZE);

        candidates.sort((left, right) -> Integer.compare(right.staleDays(), left.staleDays()));
        return candidates;
    }

    private ArchivalCandidate toCandidate(JsonNode page, int retentionDays) {
        if (page.path("pinned").asBoolean(false)) {
            return null;
        }

        int staleDays = staleDays(page.path("version").path("createdAt").asText(null));
        if (staleDays < retentionDays) {
            return null;
        }

        return new ArchivalCandidate(
                page.path("id").asText(),
                page.path("title").asText(),
                staleDays);
    }

    private int staleDays(String lastUpdated) {
        if (lastUpdated == null) {
            return 0;
        }
        Instant updated = OffsetDateTime.parse(lastUpdated).toInstant();
        return (int) ChronoUnit.DAYS.between(updated, Instant.now());
    }

    private JsonNode readTree(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new ConfluenceOperationException("could not parse Confluence page listing", e);
        }
    }
}
