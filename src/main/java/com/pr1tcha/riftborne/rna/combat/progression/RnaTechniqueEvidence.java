package com.pr1tcha.riftborne.rna.combat.progression;

import java.util.Locale;

/**
 * Server-observed actions that may reveal or stabilize future techniques.
 * Counters describe the Synchron's behaviour; they never grant an Aspect directly.
 */
public enum RnaTechniqueEvidence {
    DAMAGE_ABSORBED,
    PROJECTILE_INTERCEPTED,
    HIGH_LOAD_TECHNIQUE_USE,
    DIRECT_DAMAGE_DEALT,
    MOBILITY_BURST,
    CONTROL_MAINTAINED,
    OVERLOAD_SURVIVED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "rna.riftborne.evidence." + id();
    }

    public static RnaTechniqueEvidence fromId(String id) {
        if (id != null) {
            for (RnaTechniqueEvidence evidence : values()) {
                if (evidence.id().equalsIgnoreCase(id)) {
                    return evidence;
                }
            }
        }
        return null;
    }
}
