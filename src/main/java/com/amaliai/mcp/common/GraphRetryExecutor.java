package com.amaliai.mcp.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.function.Supplier;

/**
 * Executes Graph / Confluence API calls with exponential backoff retries.
 * <p>
 * Transient failures (HTTP 429, 503, connection resets) are common against the
 * Microsoft Graph and Atlassian APIs. This helper retries the supplied call up
 * to {@link #MAX_RETRIES} times, doubling the wait between attempts.
 */
@Component
public class GraphRetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(GraphRetryExecutor.class);

    private static final int  MAX_RETRIES      = 3;
    private static final long INITIAL_BACKOFF_MS = 200L;

    /**
     * Runs {@code call}, retrying on failure with exponential backoff.
     *
     * @param description short label for logging (e.g. {@code "searchDocuments"})
     * @param call        the API invocation to execute
     * @return the call's result once it succeeds
     */
    public <T> T executeWithRetry(String description, Supplier<T> call) {
        long backoff = INITIAL_BACKOFF_MS;
        RuntimeException lastError = null;

        for (int attempt = 1; attempt < MAX_RETRIES; attempt++) {
            try {
                return call.get();
            } catch (HttpClientErrorException e) {
                lastError = e;
                log.warn("'{}' failed on attempt {} ({}), retrying in {}ms",
                        description, attempt, e.getStatusCode(), backoff);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    log.warn("Retry sleep interrupted for '{}'", description);
                }
                backoff *= 2;
            }
        }

        throw lastError;
    }
}
