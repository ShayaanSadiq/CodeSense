package com.codesense.prototype.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Slot {
    IO("io"),
    STRATEGY("strategy"),
    MEMORY("memory"),
    STOP("stop");

    private final String value;

    Slot(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static Slot from(String raw) {
        if (raw == null) {
            return IO;
        }
        for (Slot slot : values()) {
            if (slot.value.equals(raw) || slot.name().equalsIgnoreCase(raw)) {
                return slot;
            }
        }
        return IO;
    }
}
