package com.amaliai.mcp.servers.confluence.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfluenceResponseUtilTest {

    private final ConfluenceResponseUtil util = new ConfluenceResponseUtil();

    @Test
    void errorResponse_buildsExpectedJsonEnvelope() {
        String result = util.errorResponse("searchContent", "Invalid query");

        assertThat(result).isEqualTo("{\"tool\":\"searchContent\",\"error\":\"Invalid query\"}");
    }

    @Test
    void trimResponse_returnsOriginalWhenWithinLimit() {
        assertThat(util.trimResponse("hello", 10)).isEqualTo("hello");
    }

    @Test
    void trimResponse_truncatesAndAddsMarkerWhenExceeded() {
        String result = util.trimResponse("abcdefghij", 5);

        assertThat(result).isEqualTo("abcde... [TRUNCATED]");
    }
}

