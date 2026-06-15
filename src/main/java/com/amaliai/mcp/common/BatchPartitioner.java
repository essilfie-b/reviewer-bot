package com.amaliai.mcp.common;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a list of identifiers into fixed-size batches for Microsoft Graph
 * {@code $batch} requests, which accept at most 20 sub-requests each.
 * <p>
 * Callers use this to page through large ID sets (e.g. all page IDs in a space)
 * without exceeding the Graph batching limit.
 */
@Component
public class BatchPartitioner {

    /** Maximum number of sub-requests Graph allows in a single $batch call. */
    public static final int MAX_BATCH_SIZE = 20;

    /**
     * Partitions {@code ids} into consecutive sublists of at most {@code batchSize}.
     *
     * @param ids       the identifiers to partition (order is preserved)
     * @param batchSize the maximum size of each batch
     * @return a list of batches covering every element of {@code ids}
     */
    public List<List<String>> partition(List<String> ids, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i + batchSize <= ids.size(); i += batchSize) {
            batches.add(new ArrayList<>(ids.subList(i, i + batchSize)));
        }
        return batches;
    }
}
