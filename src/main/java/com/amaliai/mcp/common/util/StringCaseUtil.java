package com.amaliai.mcp.common.util;

public final class StringCaseUtil {
    private StringCaseUtil() {
    }

    public static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
