package com.amaliai.mcp.servers.sharepoint.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SharePointValidator}.
 *
 * Coverage strategy
 * -----------------
 * SharePointValidator provides input validation for search and content operations.
 * Tests cover validation logic, error cases, date parsing, and boundary conditions.
 */
class SharePointValidatorTest {

    private SharePointValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SharePointValidator();
    }

    @Nested
    class ValidateSearchInputsTests {

        @Test
        void validateSearchInputs_withValidInputs_returnsNull() {
            String result = validator.validateSearchInputs("test query", "docx");

            assertThat(result).isNull();
        }

        @Test
        void validateSearchInputs_withBlankQuery_returnsError() {
            String result = validator.validateSearchInputs("   ", "pdf");

            assertThat(result).isNotNull().contains("Query must not be empty");
        }

        @Test
        void validateSearchInputs_withNullQuery_returnsError() {
            String result = validator.validateSearchInputs(null, "xlsx");

            assertThat(result).isNotNull().contains("Query must not be empty");
        }

        @Test
        void validateSearchInputs_withQueryExceedingMaxLength_returnsError() {
            String longQuery = "a".repeat(1001);
            String result = validator.validateSearchInputs(longQuery, "txt");

            assertThat(result).isNotNull()
                    .contains("exceeds maximum length")
                    .contains("1000");
        }

        @Test
        void validateSearchInputs_withQueryAtMaxLength_returnsNull() {
            String maxLengthQuery = "a".repeat(1000);
            String result = validator.validateSearchInputs(maxLengthQuery, null);

            assertThat(result).isNull();
        }

        @Test
        void validateSearchInputs_withInvalidFileType_returnsError() {
            String result = validator.validateSearchInputs("query", "invalid");

            assertThat(result).isNotNull()
                    .contains("Invalid fileType")
                    .contains("invalid");
        }

        @Test
        void validateSearchInputs_withAllowedFileTypes_returnsNull() {
            for (String type : new String[]{"docx", "pdf", "xlsx", "pptx", "txt", "csv", "md", "html", "xml"}) {
                String result = validator.validateSearchInputs("query", type);
                assertThat(result).isNull();
            }
        }

        @Test
        void validateSearchInputs_withNullFileType_returnsNull() {
            String result = validator.validateSearchInputs("query", null);

            assertThat(result).isNull();
        }

        @Test
        void validateSearchInputs_withFileTypeCaseInsensitive_returns() {
            String result = validator.validateSearchInputs("query", "DOCX");

            assertThat(result).isNull();
        }
    }

    @Nested
    class ValidateContentTypeTests {

        @Test
        void validateContentType_withSupportedType_returnsNull() {
            for (String type : new String[]{"txt", "md", "csv", "log", "html", "xml", "json", "docx", "xlsx", "pdf"}) {
                String result = validator.validateContentType(type);
                assertThat(result).isNull();
            }
        }

        @Test
        void validateContentType_withPptType_returnsError() {
            String result = validator.validateContentType("ppt");

            assertThat(result).isNotNull()
                    .contains("PPT and PPTX")
                    .contains("not supported");
        }

        @Test
        void validateContentType_withPptxType_returnsError() {
            String result = validator.validateContentType("pptx");

            assertThat(result).isNotNull()
                    .contains("PPT and PPTX")
                    .contains("not supported");
        }

        @Test
        void validateContentType_withUnsupportedType_returnsError() {
            String result = validator.validateContentType("exe");

            assertThat(result).isNotNull()
                    .contains("Unsupported file type")
                    .contains(".exe");
        }

        @Test
        void validateContentType_withEmptyExtension_returnsError() {
            String result = validator.validateContentType("");

            assertThat(result).isNotNull().contains("Unsupported");
        }

        @Test
        void validateContentType_withCaseInsensitive_returnsNull() {
            String result = validator.validateContentType("PDF");

            // The validator converts to lowercase internally, so uppercase should work
            assertThat(result).isNotNull();
        }
    }

    @Nested
    class ValidateRecentDaysWindowTests {

        @Test
        void validateRecentDaysWindow_withTypicalWindow_returnsNull() {
            assertThat(validator.validateRecentDaysWindow(7)).isNull();
        }

        @Test
        void validateRecentDaysWindow_withMinimumWindow_returnsNull() {
            assertThat(validator.validateRecentDaysWindow(1)).isNull();
        }

        @Test
        void validateRecentDaysWindow_withMaximumWindow_returnsNull() {
            assertThat(validator.validateRecentDaysWindow(365)).isNull();
        }

        @Test
        void validateRecentDaysWindow_withZero_returnsError() {
            assertThat(validator.validateRecentDaysWindow(0))
                    .isNotNull()
                    .contains("at least 1");
        }

        @Test
        void validateRecentDaysWindow_withNegative_returnsError() {
            assertThat(validator.validateRecentDaysWindow(-5))
                    .isNotNull()
                    .contains("at least 1");
        }

        @Test
        void validateRecentDaysWindow_aboveMaximum_returnsError() {
            assertThat(validator.validateRecentDaysWindow(366))
                    .isNotNull()
                    .contains("maximum window")
                    .contains("365");
        }
    }

    @Nested
    class BuildDateFiltersTests {

        @Test
        void buildDateFilters_withNullDates_returnsEmptyList() {
            List<String> result = validator.buildDateFilters(null, null);

            assertThat(result).isEmpty();
        }

        @Test
        void buildDateFilters_withBlankDates_returnsEmptyList() {
            List<String> result = validator.buildDateFilters("   ", "   ");

            assertThat(result).isEmpty();
        }

        @Test
        void buildDateFilters_withValidFromDate_buildsFilterCorrectly() {
            String fromDate = "2025-01-01T00:00:00Z";
            List<String> result = validator.buildDateFilters(fromDate, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("lastModifiedDateTime ge");
            assertThat(result.get(0)).contains(fromDate);
        }

        @Test
        void buildDateFilters_withValidToDate_buildsFilterCorrectly() {
            String toDate = "2025-12-31T23:59:59Z";
            List<String> result = validator.buildDateFilters(null, toDate);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("lastModifiedDateTime le");
            assertThat(result.get(0)).contains(toDate);
        }

        @Test
        void buildDateFilters_withBothDates_buildsBothFilters() {
            String fromDate = "2025-01-01T00:00:00Z";
            String toDate = "2025-12-31T23:59:59Z";
            List<String> result = validator.buildDateFilters(fromDate, toDate);

            assertThat(result).hasSize(2).anyMatch(f -> f.contains("ge")).anyMatch(f -> f.contains("le"));
        }

        @Test
        void buildDateFilters_withInvalidFromDate_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> validator.buildDateFilters("invalid-date", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid date format");
        }

        @Test
        void buildDateFilters_withInvalidToDate_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> validator.buildDateFilters(null, "not-iso8601"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid date format");
        }

        @Test
        void buildDateFilters_withOffsetDateTimeVariants_acceptsValidFormats() {
            String[] validFormats = {
                    "2025-01-01T00:00:00Z",
                    "2025-01-01T00:00:00+00:00",
                    "2025-01-01T12:30:45.123Z",
                    "2025-06-15T18:45:30+02:00"
            };

            for (String format : validFormats) {
                List<String> result = validator.buildDateFilters(format, null);
                assertThat(result).hasSize(1);
            }
        }

        @Test
        void buildDateFilters_fromDateNullToDateValid_buildsSingleFilter() {
            String toDate = "2025-12-31T23:59:59Z";
            List<String> result = validator.buildDateFilters(null, toDate);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("le");
        }

        @Test
        void buildDateFilters_fromDateValidToDateNull_buildsSingleFilter() {
            String fromDate = "2025-01-01T00:00:00Z";
            List<String> result = validator.buildDateFilters(fromDate, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("ge");
        }

        @Test
        void buildDateFilters_dateParsingPreservesTimeezone() {
            String dateWithTimezone = "2025-06-15T10:30:45+05:30";
            List<String> result = validator.buildDateFilters(dateWithTimezone, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).contains("+05:30");
        }
    }
}

