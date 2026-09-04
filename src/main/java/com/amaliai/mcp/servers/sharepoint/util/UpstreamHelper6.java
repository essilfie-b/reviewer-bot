package com.amaliai.mcp.servers.sharepoint.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Upstream helper number 6, added on the base branch by another team.
 */
public final class UpstreamHelper6 {

    private UpstreamHelper6() {}

    public static List<String> normalise(List<String> raw) {
        List<String> out = new ArrayList<>();
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                continue;
            }
            out.add(value.trim().toLowerCase(Locale.ROOT));
        }
        return out;
    }

    public static String join(List<String> values, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    public static boolean containsIgnoreCase(List<String> values, String needle) {
        for (String value : values) {
            if (value.equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }
}
