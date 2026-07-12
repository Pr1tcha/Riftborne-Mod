package com.pr1tcha.riftborne.physical.pushup;

public enum PushupPhase {
    IDLE,
    PREPARE,
    LOWER,
    HOLD,
    RAISE,
    REST,
    PAUSED;

    public static PushupPhase fromOrdinal(int ordinal) {
        PushupPhase[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IDLE;
    }

    public String translationKey() {
        return "hud.riftborne.pushup." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
