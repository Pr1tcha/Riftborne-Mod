package com.pr1tcha.riftborne.rna.data;

import java.util.Locale;

public enum FormationPath {
    NONE(false, 1.0D),
    TRAINING(true, 0.8D),
    STRESS(true, 1.25D),
    ARTIFICIAL_BORN(true, 1.1D),
    TECHNOLOGICAL(false, 1.0D),
    INTERSPATIAL(false, 1.0D);

    private final boolean selectable;
    private final double metaWearMultiplier;

    FormationPath(boolean selectable, double metaWearMultiplier) {
        this.selectable = selectable;
        this.metaWearMultiplier = metaWearMultiplier;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public double metaWearMultiplier() {
        return metaWearMultiplier;
    }

    public double growthMultiplier(RnaStat stat) {
        return switch (this) {
            case TRAINING -> switch (stat) {
                case CONNECTIVITY, OVERLOAD_RESISTANCE -> 1.2D;
                case NODE_DENSITY, THROUGHPUT -> 0.9D;
            };
            case STRESS -> switch (stat) {
                case NODE_DENSITY, THROUGHPUT -> 1.25D;
                case CONNECTIVITY, OVERLOAD_RESISTANCE -> 0.9D;
            };
            case ARTIFICIAL_BORN -> 0.7D;
            case NONE, TECHNOLOGICAL, INTERSPATIAL -> 0.0D;
        };
    }

    public static FormationPath fromId(String id) {
        if (id == null || id.isBlank()) {
            return NONE;
        }
        try {
            return valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}
