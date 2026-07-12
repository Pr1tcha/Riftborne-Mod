package com.pr1tcha.riftborne.physical;

import java.util.Locale;

public enum PhysicalStat {
    ENDURANCE,
    STRENGTH,
    MOTORICS,
    STABILITY;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String titleKey() {
        return "physical.riftborne." + id();
    }

    public static PhysicalStat fromOrdinal(int ordinal) {
        PhysicalStat[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ENDURANCE;
    }
}
