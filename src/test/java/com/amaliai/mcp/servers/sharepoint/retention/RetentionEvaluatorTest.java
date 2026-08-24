package com.amaliai.mcp.servers.sharepoint.retention;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RetentionEvaluatorTest {

    private final RetentionEvaluator evaluator = new RetentionEvaluator();

    @Test
    void flagsDocumentOlderThanTheWindow() {
        RetentionPolicy policy = new RetentionPolicy("site", "lib", Duration.ofDays(30), false);
        DocumentRecord old = new DocumentRecord("1", "old.docx",
                Instant.parse("2020-01-01T00:00:00Z"), 10L, "someone");
        assertTrue(evaluator.isExpired(old, policy, Instant.parse("2024-01-01T00:00:00Z")));
    }

    @Test
    void neverFlagsDocumentUnderLegalHold() {
        RetentionPolicy policy = new RetentionPolicy("site", "lib", Duration.ofDays(30), true);
        DocumentRecord old = new DocumentRecord("1", "old.docx",
                Instant.parse("2020-01-01T00:00:00Z"), 10L, "someone");
        assertFalse(evaluator.isExpired(old, policy, Instant.parse("2024-01-01T00:00:00Z")));
    }
}
