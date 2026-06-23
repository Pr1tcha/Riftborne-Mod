package com.pr1tcha.riftborne.codex.data.entry;

import java.util.Locale;

public enum CodexSourceType {
    SCAN,
    SIGNAL,
    FLASH_DRIVE,
    AUTO_UNLOCK,
    QUEST,
    SYSTEM,
    OTHER;

    public static CodexSourceType fromString(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
