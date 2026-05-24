package com.codaily.retrospect;

import java.util.Locale;

public enum AiProvider {
    TEMPLATE,
    GEMINI,
    OPENAI,
    ANTHROPIC;

    public static AiProvider from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "template", "stub" -> TEMPLATE;
            case "gemini" -> GEMINI;
            case "openai", "chatgpt" -> OPENAI;
            case "anthropic", "claude" -> ANTHROPIC;
            default -> throw new IllegalArgumentException("Unsupported AI provider: " + value);
        };
    }
}
