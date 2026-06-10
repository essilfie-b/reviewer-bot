package com.amaliai.mcp.servers.sharepoint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SearchFilter} record.
 *
 * Coverage strategy
 * -----------------
 * SearchFilter is a simple Record with no complex logic.
 * Tests verify field access and record equality.
 */
class SearchFilterTest {

    @Test
    void constructor_withAllFields_createsCorrectly() {
        SearchFilter filter = new SearchFilter("docx", "John Doe", "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z", 10);

        assertThat(filter.fileType()).isEqualTo("docx");
        assertThat(filter.author()).isEqualTo("John Doe");
        assertThat(filter.from()).isEqualTo("2025-01-01T00:00:00Z");
        assertThat(filter.to()).isEqualTo("2025-12-31T23:59:59Z");
        assertThat(filter.top()).isEqualTo(10);
    }

    @Test
    void constructor_withNullFields_createsCorrectly() {
        SearchFilter filter = new SearchFilter(null, null, null, null, null);

        assertThat(filter.fileType()).isNull();
        assertThat(filter.author()).isNull();
        assertThat(filter.from()).isNull();
        assertThat(filter.to()).isNull();
        assertThat(filter.top()).isNull();
    }

    @Test
    void constructor_withPartialFields_createsCorrectly() {
        SearchFilter filter = new SearchFilter("pdf", "Jane Smith", null, null, 25);

        assertThat(filter.fileType()).isEqualTo("pdf");
        assertThat(filter.author()).isEqualTo("Jane Smith");
        assertThat(filter.from()).isNull();
        assertThat(filter.to()).isNull();
        assertThat(filter.top()).isEqualTo(25);
    }

    @Test
    void equality_withIdenticalFields_isEqual() {
        SearchFilter filter1 = new SearchFilter("xlsx", "Author", "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z", 50);
        SearchFilter filter2 = new SearchFilter("xlsx", "Author", "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z", 50);

        assertThat(filter1).isEqualTo(filter2);
    }

    @Test
    void equality_withDifferentFields_isNotEqual() {
        SearchFilter filter1 = new SearchFilter("xlsx", "Author1", "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z", 50);
        SearchFilter filter2 = new SearchFilter("xlsx", "Author2", "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z", 50);

        assertThat(filter1).isNotEqualTo(filter2);
    }

    @Test
    void toString_includesAllFields() {
        SearchFilter filter = new SearchFilter("docx", "John Doe", "2025-01-01T00:00:00Z", "2025-12-31T23:59:59Z", 10);

        String str = filter.toString();

        assertThat(str).contains("SearchFilter");
        assertThat(str).contains("docx");
        assertThat(str).contains("John Doe");
    }
}

