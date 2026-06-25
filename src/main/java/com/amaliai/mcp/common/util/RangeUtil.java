package com.amaliai.mcp.common.util;

public final class RangeUtil {
    private RangeUtil() {
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
