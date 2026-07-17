package com.pr1tcha.riftborne.rna.power;

import java.util.Locale;

/**
 * The seven Δ-families of PS V2.5. A Δ-axis is a classification tag on an action, not a tier
 * of ownership: it feeds the load counter. Space is generally cheaper than time (per lore).
 *
 * <p>Base costs are the spec §4 defaults. They are intended to move into config later; kept as
 * enum fields for now so {@link LoadCalculator} stays a pure, testable function.
 */
public enum DeltaAxis {
    DR(Branch.SPACE, 8.0F),   // distribution
    DE(Branch.SPACE, 10.0F),  // environment / medium
    DS(Branch.SPACE, 14.0F),  // structure
    DG(Branch.SPACE, 18.0F),  // geometry
    DV(Branch.TIME, 16.0F),   // tempo
    DP(Branch.TIME, 14.0F),   // sequence
    DST(Branch.TIME, 20.0F);  // fixation

    private final Branch branch;
    private final float baseCost;

    DeltaAxis(Branch branch, float baseCost) {
        this.branch = branch;
        this.baseCost = baseCost;
    }

    public Branch branch() {
        return branch;
    }

    public float baseCost() {
        return baseCost;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DeltaAxis fromId(String id) {
        if (id == null) {
            return null;
        }
        for (DeltaAxis axis : values()) {
            if (axis.id().equalsIgnoreCase(id)) {
                return axis;
            }
        }
        return null;
    }
}
