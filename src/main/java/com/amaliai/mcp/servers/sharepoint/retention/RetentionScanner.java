package com.amaliai.mcp.servers.sharepoint.retention;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Walks a library's documents and reports the ones past their retention window. */
public class RetentionScanner {

    private final DocumentPageFetcher fetcher;
    private final RetentionEvaluator evaluator;

    public RetentionScanner(DocumentPageFetcher fetcher, RetentionEvaluator evaluator) {
        this.fetcher = fetcher;
        this.evaluator = evaluator;
    }

    public List<ExpiredDocument> scan(RetentionPolicy policy, Instant now) {
        List<ExpiredDocument> expired = new ArrayList<>();
        String pageToken = null;

        do {
            DocumentPage page = fetcher.fetchPage(policy.siteId(), policy.libraryId(), pageToken);
            for (DocumentRecord record : page.documents()) {
                if (evaluator.isExpired(record, policy, now)) {
                    expired.add(new ExpiredDocument(record.id(), record.name(), record.lastModified()));
                }
            }
            if (page.documents().isEmpty()) {
                break;
            }
            pageToken = page.nextPageToken();
        } while (pageToken != null);

        return expired;
    }
}
