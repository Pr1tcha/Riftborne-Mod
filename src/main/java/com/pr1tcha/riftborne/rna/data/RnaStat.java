package com.pr1tcha.riftborne.rna.data;

import java.util.Locale;

public enum RnaStat {
    NODE_DENSITY("nodeDensity"),
    CONNECTIVITY("connectivity"),
    THROUGHPUT("throughput"),
    OVERLOAD_RESISTANCE("overloadResistance");

    private final String id;

    RnaStat(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "rna.riftborne.stat." + name().toLowerCase(Locale.ROOT);
    }

    public static RnaStat fromId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (RnaStat stat : values()) {
            if (stat.id.toLowerCase(Locale.ROOT).equals(normalized)) {
                return stat;
            }
        }
        return null;
    }
}
