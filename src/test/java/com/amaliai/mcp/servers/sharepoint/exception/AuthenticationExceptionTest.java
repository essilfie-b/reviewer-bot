package com.amaliai.mcp.servers.sharepoint.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AuthenticationException}.
 *
 * Coverage strategy
 * -----------------
 * AuthenticationException is a simple exception wrapper.
 * Tests verify it can be constructed with message and cause,
 * and properly inherits from RuntimeException.
 */
class AuthenticationExceptionTest {

    @Test
    void constructor_withMessageAndCause_storesCorrectly() {
        Throwable cause = new RuntimeException("Original error");
        AuthenticationException exception = new AuthenticationException("Auth failed", cause);

        assertThat(exception.getMessage()).isEqualTo("Auth failed");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void constructor_withNullCause_createsValidator() {
        AuthenticationException exception = new AuthenticationException("Auth failed", null);

        assertThat(exception.getMessage()).isEqualTo("Auth failed");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void isRuntimeException_inheritsCorrectly() {
        AuthenticationException exception = new AuthenticationException("Auth failed", new Exception("cause"));

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void toString_includesMessage() {
        AuthenticationException exception = new AuthenticationException("Authentication token expired", null);

        assertThat(exception.toString()).contains("AuthenticationException");
        assertThat(exception.toString()).contains("Authentication token expired");
    }
}

