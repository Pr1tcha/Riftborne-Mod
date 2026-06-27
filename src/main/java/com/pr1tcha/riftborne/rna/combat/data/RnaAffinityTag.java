package com.pr1tcha.riftborne.rna.combat.data;

import java.util.Locale;

public enum RnaAffinityTag {
    INTENSITY,
    ENVIRONMENT,
    STRUCTURE,
    POSITION,
    TEMPO,
    STABILITY,
    CONTROL,
    BODY_CONTROL,
    SPACE_BASIC,
    OVERLOAD_MANAGEMENT;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static RnaAffinityTag fromId(String id) {
        if (id == null) {
            return null;
        }
        for (RnaAffinityTag tag : values()) {
            if (tag.id().equalsIgnoreCase(id)) {
                return tag;
            }
        }
        return null;
    }
}
