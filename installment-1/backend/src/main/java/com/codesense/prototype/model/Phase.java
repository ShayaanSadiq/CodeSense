package com.codesense.prototype.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Phase {
    ASK("ask"),
    CONFIRM("confirm"),
    ARRANGE("arrange"),
    REFUSED("refused"),
    DONE("done");

    private final String value;

    Phase(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static Phase from(String raw) {
        if (raw == null) {
            return ASK;
        }
        for (Phase phase : values()) {
            if (phase.value.equals(raw) || phase.name().equalsIgnoreCase(raw)) {
                return phase;
            }
        }
        return ASK;
    }
}
