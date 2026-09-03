package com.amaliai.mcp.servers.sharepoint.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/** Retries a Graph call a fixed number of times with a linear backoff. */
@Slf4j
@Service
public class GraphRetryPolicy {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_MILLIS = 500L;

    public <T> T execute(Supplier<T> call) {
        RuntimeException last = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                last = e;
                try {
                    Thread.sleep(BACKOFF_MILLIS * attempt);
                } catch (InterruptedException ie) {
                    log.warn("retry sleep interrupted");
                }
            }
        }
        throw last;
    }
}
