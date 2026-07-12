package com.pr1tcha.riftborne.rna.combat.progression;

import java.util.Locale;

public enum RnaTechniqueStage {
    SEALED(false),
    DISCOVERED(false),
    STABILIZED(true),
    MASTERED(true);

    private final boolean usable;

    RnaTechniqueStage(boolean usable) {
        this.usable = usable;
    }

    public boolean usable() {
        return usable;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "rna.riftborne.technique_stage." + id();
    }

    public static RnaTechniqueStage fromId(String id) {
        if (id != null) {
            for (RnaTechniqueStage stage : values()) {
                if (stage.id().equalsIgnoreCase(id)) {
                    return stage;
                }
            }
        }
        return SEALED;
    }
}
