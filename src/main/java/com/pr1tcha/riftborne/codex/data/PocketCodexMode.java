package com.pr1tcha.riftborne.codex.data;

public enum PocketCodexMode {
    SCANNER,
    PULSE,
    BUFFER;

    public static PocketCodexMode byIndex(int index) {
        PocketCodexMode[] values = values();
        return values[Math.floorMod(index, values.length)];
    }
}
