package com.pr1tcha.riftborne.rna.power.data;

import java.util.Locale;

/**
 * The four training channels of RNA practice. Each numeric RNA parameter grows from its own kind of
 * meaningful use, and Connectivity grows on a hidden track of its own rather than as a number the
 * player can watch tick up.
 */
public enum RnaTrack {
    BANDWIDTH,
    NODE_DENSITY,
    OVERLOAD_RESISTANCE,
    CONNECTIVITY;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "rna.riftborne.track." + id();
    }

    public static RnaTrack fromId(String id) {
        if (id == null) {
            return null;
        }
        for (RnaTrack track : values()) {
            if (track.id().equalsIgnoreCase(id)) {
                return track;
            }
        }
        return null;
    }
}
