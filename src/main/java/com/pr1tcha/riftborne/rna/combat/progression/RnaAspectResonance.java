package com.pr1tcha.riftborne.rna.combat.progression;

import java.util.Locale;

/**
 * The seven latent directions from which regular technique usage may eventually form an Aspect.
 * Resonance is evidence of a tendency, not an unlocked Aspect.
 */
public enum RnaAspectResonance {
    ENERGETIC,
    ELEMENTAL,
    MATERIAL_OBJECT,
    SPATIAL_DISTORTION,
    TEMPORAL_GRADIENT,
    TEMPORAL_FIXATION,
    SEQUENCE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "rna.riftborne.resonance." + id();
    }

    public static RnaAspectResonance fromId(String id) {
        if (id == null) {
            return null;
        }
        for (RnaAspectResonance resonance : values()) {
            if (resonance.id().equalsIgnoreCase(id)) {
                return resonance;
            }
        }
        return null;
    }
}
