package com.hotelos.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BookingSource {
    online,
    walk_in,
    manager;

    @JsonValue
    public String toDbValue() {
        return this == walk_in ? "walk-in" : name();
    }

    @JsonCreator
    public static BookingSource fromDbValue(String value) {
        if (value == null) {
            return null;
        }
        if ("walk-in".equals(value) || "walk_in".equals(value)) {
            return walk_in;
        }
        return valueOf(value);
    }
}
