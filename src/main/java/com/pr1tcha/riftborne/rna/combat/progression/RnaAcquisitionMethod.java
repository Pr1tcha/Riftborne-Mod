package com.pr1tcha.riftborne.rna.combat.progression;

import java.util.Locale;

/** Different routes may form the same technique and may be freely mixed. */
public enum RnaAcquisitionMethod {
    TRAINING(true),
    FIELD_ADAPTATION(true),
    ANOMALOUS_RESEARCH(true),
    ARTIFICIAL_IMPRINT(true),
    UNTRACKED(false);

    private final boolean playerRoute;

    RnaAcquisitionMethod(boolean playerRoute) {
        this.playerRoute = playerRoute;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean playerRoute() {
        return playerRoute;
    }

    public String translationKey() {
        return "rna.riftborne.acquisition_method." + id();
    }

    public static RnaAcquisitionMethod fromId(String id) {
        if (id != null) {
            for (RnaAcquisitionMethod method : values()) {
                if (method.id().equalsIgnoreCase(id)) {
                    return method;
                }
            }
        }
        return null;
    }
}
