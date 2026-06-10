package com.amaliai.mcp.servers.confluence.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfluenceOperationExceptionTest {

    @Test
    void constructor_setsMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("bad state");

        ConfluenceOperationException ex = new ConfluenceOperationException("operation failed", cause);

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex).hasMessage("operation failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}

