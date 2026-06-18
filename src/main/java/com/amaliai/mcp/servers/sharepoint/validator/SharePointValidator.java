package com.amaliai.mcp.servers.sharepoint.validator;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static com.amaliai.mcp.servers.sharepoint.SharePointConstants.*;

/**
 * Stateless input validator for all SharePoint tool parameters.
 * <p>
 * Methods either return a non-null error message (for soft validation)
 * or throw {@link IllegalArgumentException} (for date parsing failures that
 * should surface as user-facing errors in the tool layer).
 */
@Component
public class SharePointValidator {

    /**
     * Validates search query and optional file-type filter.
     *
     * @return a human-readable error message, or {@code null} if inputs are valid
     */
    public String validateSearchInputs(String query, String fileType) {
        if (query == null || query.isBlank()) {
            return "Query must not be empty";
        }
        if (query.length() > MAX_QUERY_LENGTH) {
            return "Query exceeds maximum length of " + MAX_QUERY_LENGTH + " characters";
        }
        if (fileType != null && !ALLOWED_FILE_TYPES.contains(fileType.toLowerCase())) {
            return "Invalid fileType '" + fileType + "'. Allowed values: " + ALLOWED_FILE_TYPES;
        }
        return null;
    }

    /**
     * Validates that a file extension is supported for text extraction.
     *
     * @return a human-readable error message, or {@code null} if the type is supported
     */
    public String validateContentType(String ext) {
        if ("ppt".equals(ext) || "pptx".equals(ext)) {
            return "PPT and PPTX files are not supported for content extraction";
        }
        if (!SUPPORTED_CONTENT_TYPES.contains(ext)) {
            return "Unsupported file type '." + ext + "'. Supported: " + SUPPORTED_CONTENT_TYPES;
        }
        return null;
    }

    /**
     * Validates the look-back window (in days) for the recent-documents tool.
     *
     * @return a human-readable error message, or {@code null} if the window is valid
     */
    public String validateRecentDaysWindow(int days) {
        if (days < 1) {
            return "days must be at least 1";
        }
        if (days > MAX_RECENT_DAYS) {
            return "days exceeds maximum window of " + MAX_RECENT_DAYS;
        }
        return null;
    }

    /**
     * Builds OData {@code lastModifiedDateTime} filter clauses from ISO-8601 date strings.
     *
     * @throws IllegalArgumentException if either date string is present but not valid ISO-8601
     */
    public List<String> buildDateFilters(String from, String to) {
        List<String> filters = new ArrayList<>();
        if (from != null && !from.isBlank()) {
            filters.add("lastModifiedDateTime ge " + parseIsoDate(from));
        }
        if (to != null && !to.isBlank()) {
            filters.add("lastModifiedDateTime le " + parseIsoDate(to));
        }
        return filters;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String parseIsoDate(String date) {
        try {
            return OffsetDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid date format '" + date + "'. Expected ISO-8601, e.g. 2025-01-01T00:00:00Z");
        }
    }
}
