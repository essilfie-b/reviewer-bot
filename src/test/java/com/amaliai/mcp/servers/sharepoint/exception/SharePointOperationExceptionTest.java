package com.amaliai.mcp.servers.sharepoint.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SharePointOperationException}.
 *
 * Coverage strategy
 * -----------------
 * SharePointOperationException is a simple exception wrapper.
 * Tests verify it can be constructed with message and cause,
 * and properly inherits from RuntimeException.
 */
class SharePointOperationExceptionTest {

    @Test
    void constructor_withMessageAndCause_storesCorrectly() {
        Throwable cause = new RuntimeException("Parse error");
        SharePointOperationException exception = new SharePointOperationException("Failed to process", cause);

        assertThat(exception.getMessage()).isEqualTo("Failed to process");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void constructor_withNullCause_createsCorrectly() {
        SharePointOperationException exception = new SharePointOperationException("Operation failed", null);

        assertThat(exception.getMessage()).isEqualTo("Operation failed");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void isRuntimeException_inheritsCorrectly() {
        SharePointOperationException exception = new SharePointOperationException("Failed", new Exception("cause"));

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void toString_includesMessage() {
        SharePointOperationException exception = new SharePointOperationException("Document extraction failed", null);

        assertThat(exception.toString()).contains("SharePointOperationException");
        assertThat(exception.toString()).contains("Document extraction failed");
    }

    @Test
    void wrapsCheckedExceptions_correctly() {
        Exception checkedException = new Exception("Checked exception");
        SharePointOperationException exception = new SharePointOperationException("Wrapped error", checkedException);

        assertThat(exception.getCause()).isEqualTo(checkedException);
        assertThat(exception.getCause().getMessage()).isEqualTo("Checked exception");
    }
}

