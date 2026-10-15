package com.codesense.prototype.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LlmProvider {
    OLLAMA("ollama"),
    OPENAI("openai"),
    GEMINI("gemini"),
    GROQ("groq"),
    RULES("rules");

    private final String value;

    LlmProvider(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static LlmProvider from(String raw) {
        if (raw == null) {
            return RULES;
        }
        for (LlmProvider provider : values()) {
            if (provider.value.equals(raw) || provider.name().equalsIgnoreCase(raw)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Choose ollama, openai, gemini, or groq.");
    }
}
