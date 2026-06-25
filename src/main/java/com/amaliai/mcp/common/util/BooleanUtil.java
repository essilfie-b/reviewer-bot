package com.amaliai.mcp.common.util;

public final class BooleanUtil {
    private BooleanUtil() {
    }

    public static boolean isTruthy(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
}
