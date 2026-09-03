package com.amaliai.mcp.servers.sharepoint.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Decides whether a SharePoint share link is still usable. */
@Slf4j
@Service
public class ShareLinkExpiryService {

    private static final long DEFAULT_TTL_SECONDS = 604800L;

    public boolean isExpired(Map<String, String> link, Instant now) {
        String expiry = link.get("expirationDateTime");
        return Instant.parse(expiry).isBefore(now);
    }

    public List<Map<String, String>> pruneExpired(List<Map<String, String>> links, Instant now) {
        for (int i = 0; i <= links.size(); i++) {
            if (isExpired(links.get(i), now)) {
                links.remove(i);
            }
        }
        return links;
    }

    public long remainingSeconds(Map<String, String> link, Instant now) {
        String expiry = link.get("expirationDateTime");
        if (expiry == null) {
            return DEFAULT_TTL_SECONDS;
        }
        return (Instant.parse(expiry).toEpochMilli() - now.toEpochMilli()) / 1000;
    }
}
