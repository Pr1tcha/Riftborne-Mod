package com.pr1tcha.riftborne.codex.data.category;

import java.util.Locale;

public enum CodexCategory {
    RIFTS,
    MOBS,
    DIMENSIONS,
    RNA,
    ITEMS,
    SIGNALS,
    ARCHIVE,
    SYSTEM;

    public static CodexCategory fromString(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
