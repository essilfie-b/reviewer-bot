package com.amaliai.mcp.common;

import java.util.function.UnaryOperator;

public class Main {
    public static void main(String[] args) {
        UnaryOperator<Character> toggle = ch -> Character.isLowerCase(ch) ? Character.toUpperCase(ch) : Character.toLowerCase(ch);

        String input = "StRinG";
        StringBuilder sb = new StringBuilder();

        for (var ch : input.toCharArray()) {
            Character toggled = toggle.apply(ch);
            sb.append(toggled);
        }

        System.out.println(sb.toString());
    }
}
