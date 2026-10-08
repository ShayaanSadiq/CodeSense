package com.codesense.prototype.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LlmMode {
    OPENAI("openai"),
    OLLAMA("ollama"),
    GEMINI("gemini"),
    GROQ("groq"),
    RULES("rules");

    private final String value;

    LlmMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static LlmMode from(String raw) {
        if (raw == null) {
            return RULES;
        }
        for (LlmMode mode : values()) {
            if (mode.value.equals(raw) || mode.name().equalsIgnoreCase(raw)) {
                return mode;
            }
        }
        return RULES;
    }
}
