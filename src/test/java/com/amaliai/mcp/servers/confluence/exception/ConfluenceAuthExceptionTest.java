package com.amaliai.mcp.servers.confluence.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfluenceAuthExceptionTest {

    @Test
    void constructor_setsMessageAndCause() {
        RuntimeException cause = new RuntimeException("boom");

        ConfluenceAuthException ex = new ConfluenceAuthException("auth failed", cause);

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex).hasMessage("auth failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}

