package com.pr1tcha.riftborne.rna.power.data;

import java.util.Locale;

/**
 * The four physical parameters of the organism (base progression v0.1).
 *
 * <p>Motorics was deliberately dropped: it duplicated Endurance through movement and RNA
 * Connectivity through control precision, and was trivially farmed by jumping. The resilience
 * parameter is named <em>Стойкость</em> rather than "stability" so it cannot be confused with the
 * RNA overload resistance.
 */
public enum PhysicalStat {
    STRENGTH,
    ENDURANCE,
    RESILIENCE,
    RECOVERY;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "physical.riftborne." + id();
    }

    public static PhysicalStat fromId(String id) {
        if (id == null) {
            return null;
        }
        for (PhysicalStat stat : values()) {
            if (stat.id().equalsIgnoreCase(id)) {
                return stat;
            }
        }
        return null;
    }
}
