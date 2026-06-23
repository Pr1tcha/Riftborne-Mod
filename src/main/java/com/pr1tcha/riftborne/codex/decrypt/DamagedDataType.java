package com.pr1tcha.riftborne.codex.decrypt;

import java.util.Locale;

public enum DamagedDataType {
    NONE,
    FRAGMENTED,
    CORRUPTED,
    ENCRYPTED,
    UNKNOWN;

    public static DamagedDataType fromString(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
