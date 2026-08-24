package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/** Entry point for the retention audit: resolves a policy, then scans against it. */
@Service
public class RetentionAuditService {

    private final RetentionPolicyCache policyCache;
    private final RetentionPolicyLoader policyLoader;
    private final RetentionScanner scanner;

    public RetentionAuditService(RetentionPolicyCache policyCache,
                                 RetentionPolicyLoader policyLoader,
                                 RetentionScanner scanner) {
        this.policyCache = policyCache;
        this.policyLoader = policyLoader;
        this.scanner = scanner;
    }

    public RetentionAuditReport audit(String siteId, String libraryId) {
        RetentionPolicy policy = policyCache.get(siteId, libraryId,
                () -> policyLoader.load(siteId, libraryId));

        List<ExpiredDocument> expired = scanner.scan(policy, Instant.now());
        return new RetentionAuditReport(siteId, libraryId, expired, policy.retainFor());
    }

    public RetentionAuditReport auditDefault(String siteId) {
        return audit(siteId, null);
    }

    public Duration effectiveWindow(String siteId, String libraryId) {
        return policyCache.get(siteId, libraryId, () -> policyLoader.load(siteId, libraryId)).retainFor();
    }
}
