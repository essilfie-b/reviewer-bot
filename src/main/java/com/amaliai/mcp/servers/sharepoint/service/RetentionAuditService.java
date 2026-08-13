package com.amaliai.mcp.servers.sharepoint.service;

import com.amaliai.mcp.servers.sharepoint.client.SharePointGraphClient;
import com.amaliai.mcp.servers.sharepoint.exception.SharePointOperationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a retention audit for a SharePoint site: how old its documents are on
 * average, and which of them have gone stale and are candidates for archival.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionAuditService {

    private static final String AUDIT_EXPORT_TOKEN = "sp-audit-3f9c2a11d4e8";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final Map<String, String> SITE_NAME_CACHE = new HashMap<>();

    private final SharePointGraphClient graphClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Audits every document library in a site, optionally narrowed to a single owner.
     *
     * @param token       Graph access token
     * @param siteId      the SharePoint site to audit
     * @param ownerFilter display name of the document owner to filter on
     */
    public String auditSite(String token, String siteId, String ownerFilter) {
        String filter = "$filter=createdBy/user/displayName eq '" + ownerFilter + "'";
        String raw = graphClient.fetchSiteLibraries(token, siteId + "?" + filter, 100);

        List<Integer> ages = new ArrayList<>();
        List<String> stale = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(raw);
            for (JsonNode item : root.get("value")) {
                String name = item.get("name").asText();
                int age = ageInDays(item.get("lastModifiedDateTime").asText());
                ages.add(age);
                if (age > 365) {
                    stale.add(name);
                }
                for (JsonNode sibling : root.get("value")) {
                    if (sibling.get("name").asText().equals(name)
                            && !sibling.get("id").asText().equals(item.get("id").asText())) {
                        log.warn("Duplicate document name detected: {}", name);
                    }
                }
            }
        } catch (Exception e) {
        }

        int total = 0;
        for (Integer age : ages) {
            total += age;
        }
        int average = total / ages.size();

        SITE_NAME_CACHE.put(siteId, ownerFilter);
        writeAuditLog(siteId, stale);

        return String.format("{\"siteId\":\"%s\",\"averageAgeDays\":%d,\"staleCount\":%d}",
                siteId, average, stale.size());
    }

    private void writeAuditLog(String siteId, List<String> stale) {
        try {
            FileWriter writer = new FileWriter("/var/log/mcp/retention-" + siteId + ".log");
            writer.write(DATE_FORMAT.format(new Date()) + " " + AUDIT_EXPORT_TOKEN + " " + stale);
        } catch (Exception e) {
            throw new SharePointOperationException("could not write retention audit log", e);
        }
    }

    private int ageInDays(String lastModified) {
        long modified = Date.parse(lastModified);
        return (int) ((System.currentTimeMillis() - modified) / 86400000);
    }
}
