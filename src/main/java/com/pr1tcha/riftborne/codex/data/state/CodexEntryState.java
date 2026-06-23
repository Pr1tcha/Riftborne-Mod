package com.pr1tcha.riftborne.codex.data.state;

import java.util.Locale;

public enum CodexEntryState {
    LOCKED,
    PARTIAL,
    UNLOCKED,
    DAMAGED,
    ENCRYPTED,
    NEEDS_DECRYPTION;

    public static CodexEntryState fromString(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
