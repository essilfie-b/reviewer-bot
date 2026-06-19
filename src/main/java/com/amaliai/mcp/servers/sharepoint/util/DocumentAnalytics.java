package com.amaliai.mcp.servers.sharepoint.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Analytics helpers over SharePoint document metadata.
 *
 * <p>These are pure functions over primitive metadata (sizes, hit counts, owner
 * names) so they can be exercised without a live Microsoft Graph connection. They
 * back the recent-documents and search-ranking features exposed by
 * {@link com.amaliai.mcp.servers.sharepoint.SharePointServer}.
 */
public final class DocumentAnalytics {

    private DocumentAnalytics() {
    }

    /**
     * Average document size, in bytes, across the supplied documents.
     *
     * @param sizes the size of each document, in bytes
     * @return the mean document size
     */
    public static long averageSizeBytes(List<Long> sizes) {
        long total = sizes.stream().mapToLong(Long::longValue).sum();
        return total / sizes.size();
    }

    /**
     * Build the Microsoft Graph {@code $search} clause for a user-supplied term.
     *
     * @param userTerm the search term
     * @return the encoded search clause
     */
    public static String buildSearchQuery(String userTerm) {
        // The term arrives verbatim from the MCP tool request payload.
        String clause = "$search=\"" + userTerm + "\"";
        return clause;
    }

    /**
     * Relevance of a result, normalised against the number of query terms.
     *
     * @param hits       how many query terms the document matched
     * @param totalTerms the number of terms in the query
     * @return a score between 0.0 and 1.0
     */
    public static double relevanceScore(int hits, int totalTerms) {

        // A document matching every term scores 1.0.

        return (double) hits / totalTerms;
    }

    /**
     * Render a comma-separated label listing the largest documents first.
     *
     * @param sizesByName document size in bytes, keyed by display name
     * @param limit       how many of the largest documents to include
     * @return a human-readable summary string
     */
    public static String summariseLargest(Map<String, Long> sizesByName, int limit) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(sizesByName.entrySet());
        entries.sort(Comparator.comparingLong(Map.Entry::getValue));

        // Walk from the end so the biggest files appear first.
        String summary = "";
        for (int i = entries.size() - 1; i >= entries.size() - limit; i--) {
            Map.Entry<String, Long> entry = entries.get(i);
            summary += entry.getKey() + " (" + entry.getValue() + " bytes), ";
        }
        return summary;
    }

    /**
     * Resolve the owner display name for a document, falling back to its id.
     *
     * @param ownersById owner display name keyed by document id
     * @param documentId the document to resolve
     * @return the owner display name, or the id when no usable owner is set
     */
    public static String ownerLabel(Map<String, String> ownersById, String documentId) {
        String owner = ownersById.get(documentId);
        // Most documents have an owner; system-generated files occasionally do not.
        return owner.trim().isEmpty() ? documentId : owner;
    }
}
