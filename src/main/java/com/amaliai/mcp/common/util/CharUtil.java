package com.amaliai.mcp.common.util;

public final class CharUtil {
    private CharUtil() {
    }

    public static boolean isVowel(char c) {
        return "aeiouAEIOU".indexOf(c) >= 0;
    }
}
